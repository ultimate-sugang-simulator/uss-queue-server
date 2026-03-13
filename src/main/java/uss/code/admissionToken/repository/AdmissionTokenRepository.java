package uss.code.admissionToken.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uss.code.admissionToken.domain.AdmissionToken;

public interface AdmissionTokenRepository extends JpaRepository<AdmissionToken, Long> {
    void deleteByStudentId(final String studentId);
}
