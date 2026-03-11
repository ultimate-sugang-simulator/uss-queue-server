package uss.code.global.exception.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SseException extends RuntimeException {
    private final String studentId;
    private final ExceptionCode exceptionCode;
}
