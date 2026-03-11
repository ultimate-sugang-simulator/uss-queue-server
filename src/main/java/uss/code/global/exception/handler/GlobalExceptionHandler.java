package uss.code.global.exception.handler;

import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uss.code.global.exception.entity.RestApiException;

@Log4j2
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RestApiException.class)
    public void handleRestApiException(final RestApiException e) {
        log.error("예외 발생: {}", e.getExceptionCode().getMessage());
    }

}
