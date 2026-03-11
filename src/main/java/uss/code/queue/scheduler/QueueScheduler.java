package uss.code.queue.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import uss.code.admissionToken.domain.AdmissionToken;
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

    @Scheduled(cron = "*/4 * * * * *")
    public void processQueue() {
        admitWaitingStudents();
        sendQueueStatusToWaitingStudents();
    }

    private void admitWaitingStudents() {
        final List<Ticket> waitingTickets = ticketService.getTop200Tickets();

        if (waitingTickets.isEmpty()) {
            return;
        }

        waitingTickets.forEach(ticket -> {
            try {
                admitMember(ticket.getStudentId());
            } catch (SseException e) {
                log.error("대기열 입장 처리 결과 전송 실패 - studentId: {}", ticket.getStudentId());
                cleanUpMember(ticket.getStudentId());
            }
        });
    }

    private void sendQueueStatusToWaitingStudents() {
        final List<Ticket> waitingTickets = ticketService.getAllTickets();

        if (waitingTickets.isEmpty()) {
            return;
        }

        for (int i = 0; i < waitingTickets.size(); i++) {
            final Ticket ticket = waitingTickets.get(i);
            try {
                sendWaitingCount(ticket.getStudentId(), i);
            } catch (SseException e) {
                log.error("대기열 상태 전송 실패 - studentId: {}", ticket.getStudentId());
                cleanUpMember(ticket.getStudentId());
            }
        }
    }

    private void admitMember(final String studentId) {
        final AdmissionToken admissionToken = admissionTokenService.issue(studentId);
        final QueueStatusResponse response = QueueStatusResponse.accessible(admissionToken.getToken());

        final SseEmitter emitter = emitterService.getEmitter(studentId);

        emitterService.sendEvent(studentId, emitter, response);

        cleanUpMember(studentId);
    }

    private void sendWaitingCount(
            final String studentId,
            final int waitingCount
    ){
        final QueueStatusResponse response = QueueStatusResponse.waiting(waitingCount);

        final SseEmitter emitter = emitterService.getEmitter(studentId);

        emitterService.sendEvent(studentId, emitter, response);
    }

    private void cleanUpMember(final String studentId) {
        try {
            ticketService.delete(studentId);
        } catch (Exception e) {
            // 이미 삭제된 경우 무시 (동시성으로 인한 중복 삭제 시도)
        }

        try {
            emitterService.delete(studentId);
        } catch (Exception e) {
            // 이미 삭제된 경우 무시
        }
    }
}
