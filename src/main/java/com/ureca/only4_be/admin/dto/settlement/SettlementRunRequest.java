package com.ureca.only4_be.admin.dto.settlement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "정산 배치 실행 요청 DTO")
public class SettlementRunRequest {

    @Schema(description = "실행할 연월 (yyyy-MM)", example = "2026-01")
    private String targetDate;
}