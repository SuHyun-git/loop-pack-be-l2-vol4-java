package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.UserCouponService;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
public class CouponAdminController {

    private final UserCouponService userCouponService;

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/api-admin/v1/user-coupons/{userCouponId}/block")
    public ApiResponse<Void> block(@PathVariable Long userCouponId) {
        userCouponService.block(userCouponId);
        return ApiResponse.success(null);
    }
}
