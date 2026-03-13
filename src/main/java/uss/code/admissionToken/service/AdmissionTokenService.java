package uss.code.admissionToken.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uss.code.admissionToken.util.AdmissionTokenGenerator;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AdmissionTokenService {

    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public String issue(final String studentId){
        String token = AdmissionTokenGenerator.generate();
        redisTemplate.opsForValue().set(studentId, token, 60, TimeUnit.SECONDS);

        return token;
    }

    @Transactional
    public void delete(final String studentId) {
        redisTemplate.delete(studentId);
    }
}
