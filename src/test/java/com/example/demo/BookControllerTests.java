package com.example.demo;

import com.example.demo.db.Book;
import com.example.demo.db.BookRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookControllerTests {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BookRepository bookRepository;

    // MockWebServer setup
    static MockWebServer server;

    @BeforeAll
    static void startServer() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterAll
    static void stopServer() throws IOException {
        server.shutdown();
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("google.books.base-url", () -> server.url("/").toString());
    }

    // Helper to read JSON from resources
    private String getJsonBody(String resourceName) throws IOException {
        Path path = Paths.get("src", "test", "resources", resourceName);
        return Files.readString(path);
    }

    @BeforeEach
    void setup() {
        bookRepository.deleteAll();
        // Seed some initial data for existing tests
        bookRepository.save(new Book("lRtdEAAAQBAJ", "Spring in Action", "Craig Walls", 500));
        bookRepository.save(new Book("12muzgEACAAJ", "Effective Java", "Joshua Bloch", 400));
    }

    @Test
    void testGetAllBooks() throws Exception {
        mockMvc.perform(get("/books"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].title").value("Spring in Action"))
            .andExpect(jsonPath("$[1].title").value("Effective Java"));
    }

    @Test
    void testCreateBookFromGoogleId_success() throws Exception {
        String googleId = "testGoogleId123";
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(getJsonBody("googlebook_valid.json")));

        mockMvc.perform(post("/books/{googleId}", googleId))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(googleId))
            .andExpect(jsonPath("$.title").value("Test Driven Development by Example"))
            .andExpect(jsonPath("$.author").value("Kent Beck"))
            .andExpect(jsonPath("$.pageCount").value(224));

        // Verify it was persisted
        Optional<Book> persistedBook = bookRepository.findById(googleId);
        assertThat(persistedBook).isPresent();
        assertThat(persistedBook.get().getTitle()).isEqualTo("Test Driven Development by Example");
    }

    @Test
    void testCreateBookFromGoogleId_googleBookNotFound() throws Exception {
        String googleId = "nonExistentId";
        server.enqueue(new MockResponse().setResponseCode(404)); // Google API returns 404

        mockMvc.perform(post("/books/{googleId}", googleId))
            .andExpect(status().isBadRequest()); // Our controller translates 404 from upstream to 400

        // Verify nothing was persisted
        assertThat(bookRepository.findById(googleId)).isNotPresent();
    }

    @Test
    void testCreateBookFromGoogleId_bookAlreadyExists() throws Exception {
        String googleId = "existingBookId";
        bookRepository.save(new Book(googleId, "Already Exists", "Existing Author", 100)); // Seed existing book
        
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(getJsonBody("googlebook_valid.json")));

        mockMvc.perform(post("/books/{googleId}", googleId))
            .andExpect(status().isConflict()); // Expect 409 Conflict

        // Verify the book count remains the same and content is unchanged
        assertThat(bookRepository.count()).isEqualTo(3); // 2 seeded in setup + 1 existing
        assertThat(bookRepository.findById(googleId).get().getTitle()).isEqualTo("Already Exists");
    }

    @Test
    void testCreateBookFromGoogleId_googleBookInsufficientData() throws Exception {
        String googleId = "testGoogleIdNoVolume";
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(getJsonBody("googlebook_no_volumeinfo.json"))); // Response with null volumeInfo

        mockMvc.perform(post("/books/{googleId}", googleId))
            .andExpect(status().isBadRequest()); // Expect 400 Bad Request

        // Verify nothing was persisted
        assertThat(bookRepository.findById(googleId)).isNotPresent();
    }

    @Test
    void testGoogleSearchEndpoint() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(getJsonBody("effectivejava.json"))); // Re-using existing mock data

        mockMvc.perform(get("/google").param("q", "Effective Java"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.kind").value("books#volumes"))
            .andExpect(jsonPath("$.items[0].volumeInfo.title").value("Effective Java"));
    }
}
