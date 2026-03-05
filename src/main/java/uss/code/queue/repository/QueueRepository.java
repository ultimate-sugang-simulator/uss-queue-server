package uss.code.queue.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uss.code.queue.entity.Queue;

public interface QueueRepository extends JpaRepository<Queue, Long> {
    void deleteByStudentId(String studentId);

    boolean existsByStudentId(String studentId);
}
