package uss.code.global.exception.dto.response;

import lombok.Builder;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record ExceptionResponse(
        int code,
        String message
) {
    public static ExceptionResponse of(
            final int code,
            final String message
    ){
        return ExceptionResponse.builder()
                .code(code)
                .message(message)
                .build();
    }
}
