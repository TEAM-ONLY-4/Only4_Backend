package com.ureca.only4_be.admin.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "관리자 대시보드 통계 응답 DTO")
public class DashboardStatsDto {

    @Schema(description = "총 가입자 수", example = "1000000")
    private long totalMemberCount;

    @Schema(description = "이번 달 정산 완료 건수 (생성된 청구서 수)", example = "980000")
    private long settlementCompletedCount;

    @Schema(description = "이번 달 발송 대기 건수 (아직 전송 안 됨)", example = "980000")
    private long sendWaitingCount;

    @Schema(description = "이메일 발송 성공 건수", example = "971180")
    private long emailSuccessCount;

    @Schema(description = "SMS 대체 발송 성공 건수", example = "9800")
    private long smsSuccessCount;
}