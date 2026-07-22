package com.powerassetintelligence.application.port.out;

import java.util.List;

public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public PageResult {
        content = List.copyOf(content == null ? List.of() : content);
    }
}
