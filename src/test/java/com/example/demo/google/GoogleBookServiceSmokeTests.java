package com.example.demo.google;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test: Calls the real Google Books API. Intended for CI smoke, may be flaky without network.
 */
@SpringBootTest
class GoogleBookServiceSmokeTests {

    @Autowired
    private GoogleBookService googleBookService;

    @Test
    void search_effectiveJava_returnsStructuredResults() {
        GoogleBook result = googleBookService.searchBooks("effective+java", 5, 0);
        assertThat(result).isNotNull();
        assertThat(result.kind()).isEqualTo("books#volumes");
        assertThat(result.totalItems()).isGreaterThan(0);
        assertThat(result.items()).isNotNull();
        assertThat(result.items()).isNotEmpty();

        GoogleBook.Item first = result.items().get(0);
        assertThat(first.id()).isNotBlank();
        assertThat(first.selfLink()).isNotBlank();
        assertThat(first.volumeInfo()).isNotNull();
        assertThat(first.volumeInfo().title()).isEqualTo("Effective Java");
        assertThat(first.volumeInfo().authors()).isNotNull();
        assertThat(first.volumeInfo().language()).isNotNull();
        assertThat(first.searchInfo()).isNotNull();
        assertThat(first.searchInfo().textSnippet()).isNotNull();
    }
}
