package com.ureca.only4_be.api.controller;

import com.ureca.only4_be.api.dto.BatchJobResponse;
import com.ureca.only4_be.api.dto.NotificationStatDto;
import com.ureca.only4_be.api.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;

    @GetMapping("/stats")
    public ResponseEntity<List<NotificationStatDto>> getNotificationStats(){
        return ResponseEntity.ok(notificationService.getNotificationStats());
    }

    @PostMapping("/manual-send")
    public ResponseEntity<BatchJobResponse> runManualBatch(
            @RequestParam(required = false) String date
    ) {
        BatchJobResponse response = notificationService.runManualBatch(date);

        return ResponseEntity.ok(response);
    }
}
