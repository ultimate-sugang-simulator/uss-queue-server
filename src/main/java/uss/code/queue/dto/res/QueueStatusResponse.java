package uss.code.queue.dto.res;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import static lombok.AccessLevel.PRIVATE;

@Builder(access = PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QueueStatusResponse(
    boolean isAccessible,
    Long waitingCount,
    String token
) {
    public static QueueStatusResponse connected() {
        return QueueStatusResponse.builder()
                .isAccessible(false)
                .waitingCount(null)
                .token(null)
                .build();
    }

    public static QueueStatusResponse waiting(final long waitingCount) {
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
