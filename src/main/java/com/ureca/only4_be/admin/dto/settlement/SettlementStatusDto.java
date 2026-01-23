package com.ureca.only4_be.admin.dto.settlement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "정산 현황 조회 응답 DTO")
public class SettlementStatusDto {

    @Schema(description = "조회 대상 정산월", example = "2026-01")
    private String targetDate;

    @Schema(description = "전체 정산 대상 건수 (전체 회원 수)", example = "1000000")
    private long totalTargetCount;

    @Schema(description = "정산 완료 건수 (생성된 청구서 수)", example = "939209")
    private long processedCount;

    @Schema(description = "총 청구 금액 합계 (단위: 원)", example = "72369800000")
    private BigDecimal totalAmount;

    @Schema(description = "마지막 배치 실행 일시", example = "2026-01-23T16:23:00")
    private LocalDateTime lastExecutedAt;

    @Schema(description = "정산 완료 여부", example = "false")
    private boolean isCompleted;
}