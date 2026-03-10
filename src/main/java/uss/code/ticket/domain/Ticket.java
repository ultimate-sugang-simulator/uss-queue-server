package uss.code.ticket.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Ticket {

    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = false, name = "student_id")
    private String studentId;

    @Builder(access = PRIVATE)
    private Ticket(final String studentId) {
        this.studentId = studentId;
    }

    public static Ticket issue(final String studentId) {
        return Ticket.builder()
                .studentId(studentId)
                .build();
    }
}
