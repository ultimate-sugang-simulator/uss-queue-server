package uss.code.queue.dto.res;

import lombok.Builder;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
public record QueueStatusResponse(
    boolean isAccessible,
    Integer waitingCount,
    String token
) {
    public static QueueStatusResponse connected() {
        return QueueStatusResponse.builder()
                .isAccessible(false)
                .waitingCount(null)
                .token(null)
                .build();
    }

    public static QueueStatusResponse waiting(final int waitingCount) {
        return QueueStatusResponse.builder()
                .isAccessible(false)
                .waitingCount(waitingCount)
                .token(null)
                .build();
    }

    public static QueueStatusResponse accessible(final String token) {
        return QueueStatusResponse.builder()
                .isAccessible(true)
                .waitingCount(null)
                .token(token)
                .build();
    }
}
