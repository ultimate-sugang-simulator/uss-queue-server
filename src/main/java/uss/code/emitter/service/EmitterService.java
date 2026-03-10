package uss.code.emitter.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import uss.code.emitter.repository.EmitterRepository;
import uss.code.queue.dto.res.QueueStatusResponse;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class EmitterService {

    private static final Long SSE_TIMEOUT = 60 * 60 * 1000L;

    private final EmitterRepository emitterRepository;

    public SseEmitter create(final String studentId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitterRepository.save(studentId, emitter);
        return emitter;
    }

    public void delete(final String studentId){
        emitterRepository.deleteByStudentId(studentId);
    }

    public void sendEvent(
            final SseEmitter sseEmitter,
            final QueueStatusResponse payload
    ) throws IOException {
        sseEmitter.send(SseEmitter.event()
                .name("queue-status")
                .data(payload, MediaType.APPLICATION_JSON));
    }

    public void validateEmitterAlreadyExists(final String studentId){
        if(emitterRepository.existsByStudentId(studentId)){
            throw new RuntimeException("Emitter already exists");
        }
    }

    public SseEmitter getEmitter(final String studentId){
        return emitterRepository.findByStudentId(studentId);
    }
}
