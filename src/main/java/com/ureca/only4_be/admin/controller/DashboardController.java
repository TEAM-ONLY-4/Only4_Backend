package com.ureca.only4_be.admin.controller;

import com.ureca.only4_be.admin.controller.docs.DashboardControllerDocs;
import com.ureca.only4_be.admin.dto.DashboardStatsDto;
import com.ureca.only4_be.admin.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController implements DashboardControllerDocs {

    private final DashboardService dashboardService;

    @Override
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getStats(
            @RequestParam(value = "date", required = false) String date
    ) {
        DashboardStatsDto stats = dashboardService.getDashboardStats(date);

        // SettlementController와 동일하게 DTO를 바로 ok()에 담아서 반환
        return ResponseEntity.ok(stats);
    }
}