package uss.code.queue.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import uss.code.admissionToken.service.AdmissionTokenService;
import uss.code.emitter.service.EmitterService;
import uss.code.global.exception.entity.SseException;
import uss.code.queue.dto.res.QueueStatusResponse;
import uss.code.ticket.domain.Ticket;
import uss.code.ticket.service.TicketService;

import java.util.List;

@Log4j2
@Component
@RequiredArgsConstructor
public class QueueScheduler {

    private final TicketService ticketService;
    private final EmitterService emitterService;
    private final AdmissionTokenService admissionTokenService;

    @Scheduled(cron = "*/2 * * * * *")
    public void processQueue() {
        admitWaitingMembers();
        sendWaitingStatus();
    }

    private void admitWaitingMembers() {
        final List<Ticket> targetTickets = ticketService.getTargetTickets();

        if (targetTickets.isEmpty()) {
            return;
        }

        targetTickets.forEach(targetTicket -> {
            try {
                admitWaitingMember(targetTicket.getStudentId());
            } catch (SseException e) {
                log.error("대기열 입장 처리 결과 전송 실패 - studentId: {}", targetTicket.getStudentId());
                cleanUpMember(targetTicket.getStudentId());
            }
        });
    }

    private void admitWaitingMember(final String studentId) {
        final String token = admissionTokenService.issue(studentId);
        final QueueStatusResponse response = QueueStatusResponse.accessible(token);

        final SseEmitter emitter = emitterService.getEmitter(studentId);

        if (emitter == null) {
            handleDisconnectedMember(studentId, token);
            return;
        }

        emitterService.sendEvent(studentId, emitter, response);

        cleanUpMember(studentId);
    }

    private void sendWaitingStatus() {
        final List<Ticket> waitingTickets = ticketService.getAllTickets();

        if (waitingTickets.isEmpty()) {
            return;
        }

        for (int i = 0; i < waitingTickets.size(); i++) {
            final Ticket ticket = waitingTickets.get(i);
            try {
                sendWaitingStatus(ticket.getStudentId(), i);
            } catch (SseException e) {
                log.error("대기열 상태 전송 실패 - studentId: {}", ticket.getStudentId());
                cleanUpMember(ticket.getStudentId());
            }
        }
    }

    private void sendWaitingStatus(
            final String studentId,
            final int waitingCount
    ) {
        final QueueStatusResponse response = QueueStatusResponse.waiting(waitingCount);

        final SseEmitter emitter = emitterService.getEmitter(studentId);

        if (emitter == null) {
            handleDisconnectedMember(studentId, null);
            return;
        }

        emitterService.sendEvent(studentId, emitter, response);
    }

    private void handleDisconnectedMember(
            final String studentId,
            final String admissionToken
    ) {
        log.warn("연결 끊김 감지 - studentId: {}", studentId);

        if (admissionToken != null) {
            admissionTokenService.delete(admissionToken);
        }

        cleanUpMember(studentId);
    }

    private void cleanUpMember(final String studentId) {
        ticketService.delete(studentId);
        emitterService.delete(studentId);
    }
}
