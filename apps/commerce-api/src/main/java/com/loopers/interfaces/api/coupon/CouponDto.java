package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.UserCouponInfo;
import com.loopers.domain.coupon.CouponStatus;

import java.time.LocalDateTime;

public class CouponDto {

    public record IssueResponse(Long userCouponId) {
        public static IssueResponse from(Long id) {
            return new IssueResponse(id);
        }
    }

    public record MyCouponResponse(Long id, String templateName, LocalDateTime expiredAt, CouponStatus status) {
        public static MyCouponResponse from(UserCouponInfo info) {
            return new MyCouponResponse(info.id(), info.templateName(), info.expiredAt(), info.status());
        }
    }
}
