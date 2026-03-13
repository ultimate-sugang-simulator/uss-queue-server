package uss.code.ticket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uss.code.ticket.domain.Ticket;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    boolean existsByStudentId(final String studentId);

    void deleteByStudentId(final String studentId);

    @Query("""
        SELECT t
        FROM Ticket t
        ORDER BY t.id ASC
        LIMIT 200
    """)
    List<Ticket> findTop200();
}
