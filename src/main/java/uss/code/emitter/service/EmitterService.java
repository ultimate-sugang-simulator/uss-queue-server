package uss.code.emitter.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import uss.code.emitter.repository.EmitterRepository;
import uss.code.global.exception.entity.RestApiException;
import uss.code.global.exception.entity.SseException;
import uss.code.queue.dto.res.QueueStatusResponse;

import java.io.IOException;

import static uss.code.global.exception.entity.ExceptionCode.EMITTER_ALREADY_EXISTS;
import static uss.code.global.exception.entity.ExceptionCode.SSE_EVENT_SEND_FAILED;

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
            final String studentId,
            final SseEmitter sseEmitter,
            final QueueStatusResponse payload
    ) {
        try{
            sseEmitter.send(SseEmitter.event()
                    .name("queue-status")
                    .data(payload, MediaType.APPLICATION_JSON)
            );
        }catch (IOException e){
            throw new SseException(studentId, SSE_EVENT_SEND_FAILED);
        }

    }

    public void validateEmitterAlreadyExists(final String studentId){
        if(emitterRepository.existsByStudentId(studentId)){
            throw new RestApiException(EMITTER_ALREADY_EXISTS);
        }
    }

    public SseEmitter getEmitter(final String studentId){
        return emitterRepository.findByStudentId(studentId);
    }
}
