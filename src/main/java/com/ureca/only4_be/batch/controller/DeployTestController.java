package com.ureca.only4_be.batch.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/batch/deploy")
@RequiredArgsConstructor
public class DeployTestController {

    @GetMapping("/test")
    public String Test() {
        return "AWS EC2 띄우기 완료";
    }
}
