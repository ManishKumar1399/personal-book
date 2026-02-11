package com.example.demo.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class BookRepositoryTests {
    @Autowired
    private BookRepository bookRepository;

    @Test
    void testSaveAndFindAll() {
        Book book1 = new Book("one", "Title One", "Author A");
        Book book2 = new Book("two", "Title Two", "Author B");
        bookRepository.save(book1);
        bookRepository.save(book2);
        List<Book> books = bookRepository.findAll();
        assertThat(books).hasSize(2);
        assertThat(books).extracting(Book::getTitle).containsExactlyInAnyOrder("Title One", "Title Two");
    }
}

