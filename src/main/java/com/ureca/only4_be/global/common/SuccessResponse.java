package com.ureca.only4_be.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Schema(description = "성공 Response")
public class SuccessResponse<T> {

    @Schema(description = "성공 여부", defaultValue = "true")
    private final boolean success = true;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(type = "T", example = "data", description = "restful의 정보를 감싸 표현합니다. T 타입(DTO)으로 표현합니다.")
    private T data;

    public static <T> SuccessResponse<T> of(T data) {
        return new SuccessResponse<>(data);
    }

    public ResponseEntity<SuccessResponse<T>> asHttp(HttpStatus httpStatus) {
        return ResponseEntity.status(httpStatus).body(this);
    }

    public SuccessResponse(T data) {
        this.data = data;
    }
}
