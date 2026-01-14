package com.ureca.only4_be.global.common;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class PageResponse {

    @Schema(type = "PageInfo", description = "페이징 처리에 필요한 정보")
    private PageInfo pageInfo;

    @Schema(type = "List<?>", example = "List<?> dataList (code 0번 참고)", description = "페이징 처리된 데이터 리스트")
    private List<?> dataList = new ArrayList<>();

    public static PageResponse toPageResponse(PageInfo pageInfo, List<?> dataList) {
        return PageResponse.builder()
                .pageInfo(pageInfo)
                .dataList(dataList)
                .build();
    }
}
