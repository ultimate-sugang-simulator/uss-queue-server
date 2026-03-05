package uss.code.queue.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import uss.code.emitter.repository.EmitterRepository;
import uss.code.queue.dto.res.QueueStatusResponse;
import uss.code.queue.entity.Queue;
import uss.code.queue.repository.QueueRepository;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class QueueService {

    private static final Long SSE_TIMEOUT = 60 * 60 * 1000L;

    private final QueueRepository queueRepository;
    private final EmitterRepository emitterRepository;

    public SseEmitter subscribe(final String studentId) {

        validateAlreadyEmitterExists(studentId);
        validateQueueAlreadyExists(studentId);

        createQueue(studentId);

        SseEmitter sseEmitter = createEmitter(studentId);

        sseEmitter.onTimeout(() -> cleanUpQueue(studentId));
        sseEmitter.onError(e -> cleanUpQueue(studentId));
        sseEmitter.onCompletion(() -> cleanUpQueue(studentId));

        try {
            sendToClient(sseEmitter, QueueStatusResponse.connected());
        } catch (IOException e) {
            cleanUpQueue(studentId);
            throw new RuntimeException("Failed to send initial connection message", e);
        }

        return sseEmitter;
    }

    private void createQueue(final String studentId) {
        Queue queue = Queue.create(studentId);
        queueRepository.save(queue);
    }

    private void cleanUpQueue(final String studentId) {
        deleteEmitter(studentId);
        queueRepository.deleteByStudentId(studentId);
    }

    private void validateQueueAlreadyExists(final String studentId) {
        if (queueRepository.existsByStudentId(studentId)) {
            throw new RuntimeException("Queue already exists");
        }
    }

    private SseEmitter createEmitter(final String studentId) {
        SseEmitter sseEmitter = new SseEmitter(SSE_TIMEOUT);
        emitterRepository.save(studentId, sseEmitter);
        return sseEmitter;
    }

    private void deleteEmitter(final String studentId) {
        emitterRepository.deleteByStudentId(studentId);
    }

    private void sendToClient(
            final SseEmitter sseEmitter,
            final QueueStatusResponse payload
    ) throws IOException {
        sseEmitter.send(SseEmitter.event()
                .name("queue-status")
                .data(payload, MediaType.APPLICATION_JSON));
    }

    private void validateAlreadyEmitterExists(final String studentId) {
        if(emitterRepository.existsByStudentId(studentId)) {
            throw new RuntimeException("Emitter already exists");
        }
    }
}
