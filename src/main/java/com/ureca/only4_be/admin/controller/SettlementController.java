package com.ureca.only4_be.admin.controller;

import com.ureca.only4_be.admin.controller.docs.SettlementControllerDocs;
import com.ureca.only4_be.admin.dto.settlement.SettlementRunRequest;
import com.ureca.only4_be.admin.dto.settlement.SettlementStatusDto;
import com.ureca.only4_be.admin.service.SettlementService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settlement")
@RequiredArgsConstructor
public class SettlementController implements SettlementControllerDocs {

    private final SettlementService settlementService;

    @Override
    @GetMapping("/status")
    public ResponseEntity<SettlementStatusDto> getStatus(
            @RequestParam(value = "date", required = false) String date) {

        SettlementStatusDto status = settlementService.getSettlementStatus(date);
        return ResponseEntity.ok(status);
    }

    @Override
    @PostMapping("/run")
    public ResponseEntity<String> runBatch(@RequestBody(required = false) SettlementRunRequest request) {
        // Request Body가 null이면 현재 날짜 기준
        String targetDate = (request != null) ? request.getTargetDate() : null;

        // Service 호출 (검증 통과 시 성공 메시지 리턴)
        String resultMessage = settlementService.runSettlementJob(targetDate);

        return ResponseEntity.ok(resultMessage);
    }
}