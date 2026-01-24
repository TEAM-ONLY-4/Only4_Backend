package com.ureca.only4_be.admin.controller.docs;

import com.ureca.only4_be.admin.dto.DashboardStatsDto;
import com.ureca.only4_be.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "대시보드", description = "관리자 대시보드 통계 API")
public interface DashboardControllerDocs {

    @Operation(summary = "대시보드 통계 조회", description = "총 가입자, 정산 건수, 발송 현황 등 대시보드 상단 지표를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = DashboardStatsDto.class))),
            @ApiResponse(responseCode = "400", description = "날짜 형식 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": false,
                                              "message": "Invalid Type Value",
                                              "status": 400,
                                              "code": "G003",
                                              "reason": "날짜 형식이 올바르지 않습니다. (yyyy-MM)",
                                              "errors": []
                                            }
                                            """
                            ))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": false,
                                              "message": "Internal Server Error",
                                              "status": 500,
                                              "code": "G999",
                                              "reason": "Null Pointer Exception",
                                              "errors": []
                                            }
                                            """
                            )))
    })
    ResponseEntity<DashboardStatsDto> getStats(
            @Parameter(description = "조회할 연월 (yyyy-MM), 생략 시 현재 월", example = "2026-01")
            @RequestParam(value = "date", required = false) String date
    );
}