package uss.code.queue.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import uss.code.emitter.service.EmitterService;
import uss.code.queue.dto.res.QueueStatusResponse;
import uss.code.ticket.service.TicketService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class QueueFacade {

    private final TicketService ticketService;
    private final EmitterService emitterService;

    public SseEmitter subscribe(final String studentId) {

        emitterService.validateEmitterAlreadyExists(studentId);
        ticketService.validateTicketAlreadyExists(studentId);

        ticketService.issue(studentId);

        SseEmitter sseEmitter = createAndSetUpEmitter(studentId);

        try{
            emitterService.sendEvent(sseEmitter, QueueStatusResponse.connected());
        }catch (IOException e){
            cleanUp(studentId);
            throw new RuntimeException("Failed to send initial connection message", e);
        }

        return sseEmitter;
    }

    private SseEmitter createAndSetUpEmitter(final String studentId) {
        SseEmitter sseEmitter = emitterService.create(studentId);

        sseEmitter.onTimeout(() -> cleanUp(studentId));
        sseEmitter.onError(e -> cleanUp(studentId));
        sseEmitter.onCompletion(() -> cleanUp(studentId));

        return sseEmitter;
    }

    private void cleanUp(final String studentId) {
        emitterService.delete(studentId);
        ticketService.delete(studentId);
    }
}
