package uss.code.queue.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.*;
import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Queue {

    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = false, name = "stduent_id")
    private String studentId;

    @Enumerated(STRING)
    @Column(nullable = false)
    private QueueStatus status;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @Builder(access = PRIVATE)
    private Queue(final String studentId){
        this.studentId = studentId;
        this.status = QueueStatus.WAITING;
        this.createdAt = LocalDateTime.now();
    }

    public static Queue create(final String studentId){
        return Queue.builder()
                .studentId(studentId)
                .build();
    }

    public void markAsReady(){
        this.status = QueueStatus.READY;
    }
}
