package com.ureca.only4_be.admin.controller.docs;

import com.ureca.only4_be.admin.dto.settlement.SettlementStatusDto;
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

@Tag(name = "정산 관리", description = "정산 배치 실행 및 현황 조회 API")
public interface SettlementControllerDocs {

    @Operation(summary = "정산 현황 조회", description = "특정 월의 정산 진행률, 금액, 완료 여부를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = SettlementStatusDto.class))),

            @ApiResponse(responseCode = "400", description = "날짜 형식 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "INVALID_TYPE_VALUE",
                                    summary = "날짜 형식이 yyyy-MM이 아닐 경우",
                                    value = """
                                            {
                                              "success": false,
                                              "message": "Invalid Type Value",
                                              "status": 400,
                                              "code": "G003",
                                              "reason": "날짜 형식이 올바르지 않습니다. (yyyy-MM 형식을 사용해주세요.)",
                                              "errors": []
                                            }
                                            """
                            )
                    )
            ),

            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "INTERNAL_SERVER_ERROR",
                                    value = """
                                            {
                                              "success": false,
                                              "message": "Internal Server Error Exception",
                                              "status": 500,
                                              "code": "G999",
                                              "reason": "서버 내부 오류가 발생했습니다.",
                                              "errors": []
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<SettlementStatusDto> getStatus(
            @Parameter(description = "조회할 연월 (yyyy-MM)", example = "2026-01")
            @RequestParam(value = "date", required = false) String date
    );
}