package com.ureca.only4_be.admin.controller;

import com.ureca.only4_be.admin.controller.docs.SettlementControllerDocs;
import com.ureca.only4_be.admin.dto.settlement.SettlementStatusDto;
import com.ureca.only4_be.admin.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}