package uss.code.ticket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uss.code.global.exception.entity.RestApiException;
import uss.code.ticket.domain.Ticket;
import uss.code.ticket.repository.TicketRepository;

import java.util.List;

import static uss.code.global.exception.entity.ExceptionCode.TICKET_ALREADY_EXISTS;

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

    @Transactional(readOnly = true)
    public void validateTicketAlreadyExists(final String studentId) {
        if (ticketRepository.existsByStudentId(studentId)) {
            throw new RestApiException(TICKET_ALREADY_EXISTS);
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
