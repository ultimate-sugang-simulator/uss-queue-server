package uss.code.queue.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import uss.code.queue.service.QueueService;

import static org.springframework.http.MediaType.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/queue")
public class QueueController {

    private final QueueService queueService;

    @GetMapping(value = "/sub", produces = TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam final String studentId){
        return queueService.subscribe(studentId);
    }

}
