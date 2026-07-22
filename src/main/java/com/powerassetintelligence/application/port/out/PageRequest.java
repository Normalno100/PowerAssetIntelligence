package com.powerassetintelligence.application.port.out;

import java.util.List;

public record PageRequest(int page, int size, List<SortOrder> sort) {

    public PageRequest {
        sort = List.copyOf(sort == null ? List.of() : sort);
    }

    public record SortOrder(String field, Direction direction) {
        public enum Direction { ASC, DESC }
    }
}
