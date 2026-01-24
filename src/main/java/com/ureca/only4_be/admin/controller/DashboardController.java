package com.ureca.only4_be.admin.controller;

import com.ureca.only4_be.admin.dto.DashboardStatsDto;
import com.ureca.only4_be.admin.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "대시보드", description = "관리자 대시보드 통계 API")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "대시보드 통계 조회", description = "총 가입자, 정산 건수, 발송 현황 등 대시보드 상단 지표를 조회합니다.")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getStats(
            @Parameter(description = "조회할 연월 (yyyy-MM), 생략 시 현재 월", example = "2026-01")
            @RequestParam(value = "date", required = false) String date
    ) {
        DashboardStatsDto stats = dashboardService.getDashboardStats(date);
        return ResponseEntity.ok(ApiResponse.success(stats)); // ApiResponse는 사용하시는 공통 포맷에 맞게
    }
}