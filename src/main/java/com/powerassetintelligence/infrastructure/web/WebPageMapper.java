package com.powerassetintelligence.infrastructure.web;

import com.powerassetintelligence.application.port.out.PageRequest;
import com.powerassetintelligence.application.port.out.PageResult;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class WebPageMapper {
    private WebPageMapper() {}

    public static PageRequest toPageRequest(Pageable pageable) {
        if (pageable == null) {
            return new PageRequest(0, 20, List.of());
        }
        var sortOrders = pageable.getSort().stream()
                .map(order -> new PageRequest.SortOrder(
                        order.getProperty(),
                        order.getDirection() == Sort.Direction.ASC
                                ? PageRequest.SortOrder.Direction.ASC
                                : PageRequest.SortOrder.Direction.DESC
                ))
                .toList();
        return new PageRequest(pageable.getPageNumber(), pageable.getPageSize(), sortOrders);
    }

    public static <T> PageResult<T> toPageResult(Page<T> page) {
        var content = page.getContent().stream().toList();
        return new PageResult<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    public static <T> PageResult<T> mapPage(Page<T> page, java.util.function.Function<T, T> mapper) {
        var content = page.getContent().stream().map(mapper).toList();
        return new PageResult<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
