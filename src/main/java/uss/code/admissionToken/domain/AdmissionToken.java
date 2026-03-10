package uss.code.admissionToken.domain;

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
public class AdmissionToken {

    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false, name = "student_id")
    private String studentId;

    @Builder(access = PRIVATE)
    public AdmissionToken(
            final String token,
            final String studentId
    ){
        this.token = token;
        this.studentId = studentId;
    }

    public static AdmissionToken issue(
            final String token,
            final String studentId
    ){
        return AdmissionToken.builder()
                .token(token)
                .studentId(studentId)
                .build();
    }
}
