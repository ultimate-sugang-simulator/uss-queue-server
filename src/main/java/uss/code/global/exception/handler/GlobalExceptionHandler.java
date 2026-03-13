package uss.code.global.exception.handler;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uss.code.global.exception.dto.response.ExceptionResponse;
import uss.code.global.exception.entity.RestApiException;
import uss.code.global.exception.entity.SseException;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Log4j2
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SseException.class)
    public void handleSseException(final SseException e) {
        log.error("예외 발생: {}", e.getExceptionCode().getMessage());
    }

    @ExceptionHandler(RestApiException.class)
    public ResponseEntity<ExceptionResponse> handleRestApiException(final RestApiException e) {
        log.error("예외 발생: {}", e.getExceptionCode().getMessage());

        final ExceptionResponse exceptionResponse = ExceptionResponse.of(e.getExceptionCode().getCode(), e.getExceptionCode().getMessage());

        return ResponseEntity.status(e.getExceptionCode().getHttpStatus()).body(exceptionResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleException(final Exception e){
        log.error("예외 발생: {}", e.getMessage());

        return ResponseEntity.status(INTERNAL_SERVER_ERROR).build();
    }
}
