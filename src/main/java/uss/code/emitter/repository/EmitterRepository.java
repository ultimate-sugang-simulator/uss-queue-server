package uss.code.emitter.repository;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@Repository
public class EmitterRepository {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter findByStudentId(final String studentId) {
        return emitters.get(studentId);
    }

    public SseEmitter save(
            final String studentId,
            final SseEmitter emitter
    ) {
        emitters.put(studentId, emitter);
        return emitters.get(studentId);
    }

    public void deleteByStudentId(final String studentId) {
        closeConnection(studentId);
        emitters.remove(studentId);
    }

    public boolean existsByStudentId(final String studentId) {
        return emitters.containsKey(studentId);
    }

    private void closeConnection(final String studentId) {
        emitters.get(studentId).complete();
    }
}
