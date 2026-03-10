package uss.code.ticket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uss.code.ticket.domain.Ticket;
import uss.code.ticket.repository.TicketRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;

    @Transactional
    public void issue(final String studentId){
        Ticket ticket = Ticket.issue(studentId);
        ticketRepository.save(ticket);
    }

    @Transactional
    public void delete(final String studentId){
        ticketRepository.deleteByStudentId(studentId);
    }

    @Transactional
    public void validateTicketAlreadyExists(final String studentId) {
        if (ticketRepository.existsByStudentId(studentId)) {
            throw new RuntimeException("Ticket already exists");
        }
    }

    @Transactional(readOnly = true)
    public List<Ticket> getTop200Tickets() {
        return ticketRepository.findTop200();
    }

    @Transactional(readOnly = true)
    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }
}
