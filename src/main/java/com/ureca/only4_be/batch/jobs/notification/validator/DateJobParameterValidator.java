package com.ureca.only4_be.batch.jobs.notification.validator;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class DateJobParameterValidator implements JobParametersValidator {

    @Override
    public void validate(JobParameters parameters) throws JobParametersInvalidException {
        // 1. 파라미터 존재 여부 확인
        if (parameters == null) {
            throw new JobParametersInvalidException("❌ Job 파라미터가 없습니다.");
        }

        String requestDate = parameters.getString("requestDate");

        // 2. requestDate 필수 체크
        if (!StringUtils.hasText(requestDate)) {
            throw new JobParametersInvalidException("❌ 'requestDate' 파라미터는 필수입니다.");
        }

        // 3. 날짜 형식(yyyy-MM-dd) 체크
        try {
            LocalDate.parse(requestDate);
        } catch (DateTimeParseException e) {
            throw new JobParametersInvalidException("❌ requestDate 형식이 올바르지 않습니다. (YYYY-MM-DD) 입력값: " + requestDate);
        }
    }
}