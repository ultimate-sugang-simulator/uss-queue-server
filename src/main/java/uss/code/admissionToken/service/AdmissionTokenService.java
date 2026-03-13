package uss.code.admissionToken.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uss.code.admissionToken.domain.AdmissionToken;
import uss.code.admissionToken.repository.AdmissionTokenRepository;
import uss.code.admissionToken.util.AdmissionTokenGenerator;

@Service
@RequiredArgsConstructor
public class AdmissionTokenService {

    private final AdmissionTokenRepository admissionTokenRepository;

    @Transactional
    public AdmissionToken issue(final String studentId){
        String token = AdmissionTokenGenerator.generate();
        AdmissionToken admissionToken = AdmissionToken.issue(token, studentId);

        return admissionTokenRepository.save(admissionToken);
    }

    @Transactional
    public void delete(final String studentId) {
        admissionTokenRepository.deleteByStudentId(studentId);
    }
}
