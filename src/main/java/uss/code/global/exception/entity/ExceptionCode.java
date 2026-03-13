package uss.code.global.exception.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ExceptionCode {

    // SSE 관련 예외
    SSE_EMITTER_EXPIRED(HttpStatus.GONE, 1001, "SSE 연결이 만료되었습니다."),
    SSE_CONNECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 1002, "SSE 초기 연결 메시지 전송에 실패했습니다."),
    SSE_EVENT_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 1003, "SSE 이벤트 전송에 실패했습니다."),

    // Emitter 관련 예외
    EMITTER_ALREADY_EXISTS(HttpStatus.CONFLICT, 2001, "이미 SSE 연결이 존재합니다."),

    // Ticket 관련 예외
    TICKET_ALREADY_EXISTS(HttpStatus.CONFLICT, 3001, "이미 대기열 티켓이 존재합니다.");

    private final HttpStatus httpStatus;
    private final int code;
    private final String message;
}
