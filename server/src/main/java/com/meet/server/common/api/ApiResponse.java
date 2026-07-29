package com.meet.server.common.api;

import java.util.Optional;

public record ApiResponse<T>(
        boolean success,
        String message,
        Optional<T> data
) {
}
