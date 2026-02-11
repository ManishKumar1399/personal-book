package com.example.demo;

import com.example.demo.db.Book;
import com.example.demo.db.BookRepository;
import com.example.demo.google.GoogleBook;
import com.example.demo.google.GoogleBookService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

@RestController
public class BookController {
    private final BookRepository bookRepository;
    private final GoogleBookService googleBookService;

    @Autowired
    public BookController(BookRepository bookRepository, GoogleBookService googleBookService) {
        this.bookRepository = bookRepository;
        this.googleBookService = googleBookService;
    }

    @GetMapping("/books")
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @GetMapping("/google")
    public GoogleBook searchGoogleBooks(@RequestParam("q") String query,
                                        @RequestParam(value = "maxResults", required = false) Integer maxResults,
                                        @RequestParam(value = "startIndex", required = false) Integer startIndex) {
        return googleBookService.searchBooks(query, maxResults, startIndex);
    }

    @PostMapping("/books/{googleId}")
    public ResponseEntity<Book> createBookFromGoogleId(@PathVariable String googleId) {
        // 1. Fetch GoogleBook
        GoogleBook googleBook = googleBookService.fetchBookById(googleId);

        // 2. Handle Not Found or insufficient data from Google API
        // Check if googleBook is null, if it has items, and if the first item has volumeInfo
        if (googleBook == null || googleBook.items() == null || googleBook.items().isEmpty() || googleBook.items().get(0).volumeInfo() == null) {
            return ResponseEntity.badRequest().build();
        }

        // Check if the book already exists in our repository.
        if (bookRepository.existsById(googleId)) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        // 3. Map to Book Entity
        GoogleBook.VolumeInfo volumeInfo = googleBook.items().get(0).volumeInfo();
        String title = volumeInfo.title();
        String author = Optional.ofNullable(volumeInfo.authors())
                                .filter(authors -> !authors.isEmpty())
                                .map(authors -> authors.get(0))
                                .orElse(null);
        Integer pageCount = volumeInfo.pageCount();

        // Basic validation for crucial data
        if (googleId == null || title == null) {
             return ResponseEntity.badRequest().build();
        }

        Book book = new Book(googleId, title, author, pageCount);

        // 4. Persist Book
        Book persistedBook = bookRepository.save(book);

        // 5. Return Response
        return new ResponseEntity<>(persistedBook, HttpStatus.CREATED);
    }
}
