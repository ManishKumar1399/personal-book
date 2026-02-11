package com.example.demo.google;

import java.util.List;

public record GoogleBook(
        String kind,
        Integer totalItems,
        List<Item> items
) {
    public record Item(
            String id,
            String selfLink,
            VolumeInfo volumeInfo,
            SearchInfo searchInfo
    ) {}

    public record SearchInfo(
            String textSnippet
    ) {}
}
