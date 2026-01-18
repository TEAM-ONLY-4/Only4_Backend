package com.ureca.only4_be.batch.jobs.settlement.common;

public class ProductIdRange {
    // TV 요금제 (1 ~ 42)
    public static boolean isTvPlan(Long id) {
        return id >= 1 && id <= 42;
    }

    // 모바일용 부가서비스 (43 ~ 92)
    public static boolean isMobileAddon(Long id) {
        return id >= 43 && id <= 92;
    }

    // TV용 부가서비스 (93 ~ 102)
    public static boolean isTvAddon(Long id) {
        return id >= 93 && id <= 102;
    }

    // 모바일 요금제 (103 ~ 132)
    public static boolean isMobilePlan(Long id) {
        return id >= 103 && id <= 132;
    }
}
