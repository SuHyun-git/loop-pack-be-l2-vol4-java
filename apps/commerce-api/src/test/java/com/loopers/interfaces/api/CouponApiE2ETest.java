package com.loopers.interfaces.api;

import com.loopers.application.coupon.UserCouponInfo;
import com.loopers.application.user.UserService;
import com.loopers.domain.coupon.CouponStatus;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.coupon.CouponTemplateJpaRepository;
import com.loopers.infrastructure.coupon.UserCouponJpaRepository;
import com.loopers.interfaces.api.coupon.CouponDto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouponApiE2ETest {

    private static final String ISSUE_URL = "/api/v1/coupons/{templateId}/issue";
    private static final String MY_COUPONS_URL = "/api/v1/coupons/me";
    private static final String BLOCK_URL = "/api-admin/v1/user-coupons/{userCouponId}/block";

    private static final String LOGIN_ID_HEADER = "X-Loopers-LoginId";
    private static final String LOGIN_PW_HEADER = "X-Loopers-LoginPw";
    private static final String ADMIN_HEADER = "X-Loopers-Ldap";
    private static final String ADMIN_HEADER_VALUE = "loopers.admin";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private CouponTemplateJpaRepository couponTemplateJpaRepository;

    @Autowired
    private UserCouponJpaRepository userCouponJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private UserModel savedUser;
    private HttpHeaders userHeaders;
    private HttpHeaders adminHeaders;

    @BeforeEach
    void setUp() {
        savedUser = userService.signUp(new UserModel(
            "user01", "Password1!", "홍길동",
            LocalDate.of(1990, 1, 1), "user@example.com"
        ));
        userHeaders = new HttpHeaders();
        userHeaders.set(LOGIN_ID_HEADER, "user01");
        userHeaders.set(LOGIN_PW_HEADER, "Password1!");

        adminHeaders = new HttpHeaders();
        adminHeaders.set(ADMIN_HEADER, ADMIN_HEADER_VALUE);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/coupons/{templateId}/issue")
    @Nested
    class IssueCoupon {

        @DisplayName("발급 가능한 템플릿으로 요청하면 201과 userCouponId가 반환된다.")
        @Test
        void returns201_whenTemplateCanIssue() {
            // arrange
            var template = couponTemplateJpaRepository.save(
                new com.loopers.domain.coupon.CouponTemplateModel("10% 할인", CouponType.RATE, 10L, null, LocalDateTime.now().plusDays(7)));

            // act
            var response = testRestTemplate.exchange(
                ISSUE_URL, HttpMethod.POST,
                new HttpEntity<>(userHeaders),
                new ParameterizedTypeReference<ApiResponse<CouponDto.IssueResponse>>() {},
                template.getId()
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().data().userCouponId()).isNotNull();
        }

        @DisplayName("같은 템플릿을 중복 발급하면 409가 반환된다.")
        @Test
        void returns409_whenDuplicateIssue() {
            // arrange
            var template = couponTemplateJpaRepository.save(
                new com.loopers.domain.coupon.CouponTemplateModel("10% 할인", CouponType.RATE, 10L, null, LocalDateTime.now().plusDays(7)));
            testRestTemplate.exchange(
                ISSUE_URL, HttpMethod.POST,
                new HttpEntity<>(userHeaders),
                new ParameterizedTypeReference<ApiResponse<CouponDto.IssueResponse>>() {},
                template.getId()
            );

            // act
            var response = testRestTemplate.exchange(
                ISSUE_URL, HttpMethod.POST,
                new HttpEntity<>(userHeaders),
                new ParameterizedTypeReference<ApiResponse<CouponDto.IssueResponse>>() {},
                template.getId()
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @DisplayName("isActive=false 템플릿으로 발급하면 400이 반환된다.")
        @Test
        void returns400_whenTemplateInactive() {
            // arrange
            var template = couponTemplateJpaRepository.save(
                new com.loopers.domain.coupon.CouponTemplateModel("10% 할인", CouponType.RATE, 10L, null, LocalDateTime.now().plusDays(7)));
            template.update("10% 할인", false);
            couponTemplateJpaRepository.save(template);

            // act
            var response = testRestTemplate.exchange(
                ISSUE_URL, HttpMethod.POST,
                new HttpEntity<>(userHeaders),
                new ParameterizedTypeReference<ApiResponse<CouponDto.IssueResponse>>() {},
                template.getId()
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @DisplayName("GET /api/v1/coupons/me")
    @Nested
    class GetMyCoupons {

        @DisplayName("내 쿠폰 목록 조회 시 templateName과 status가 포함된 목록이 반환된다.")
        @Test
        void returns200WithCouponList_whenUserHasCoupons() {
            // arrange
            var template = couponTemplateJpaRepository.save(
                new com.loopers.domain.coupon.CouponTemplateModel("10% 할인", CouponType.RATE, 10L, null, LocalDateTime.now().plusDays(7)));
            userCouponJpaRepository.save(new com.loopers.domain.coupon.UserCouponModel(savedUser.getId(), template.getId()));

            // act
            var response = testRestTemplate.exchange(
                MY_COUPONS_URL, HttpMethod.GET,
                new HttpEntity<>(userHeaders),
                new ParameterizedTypeReference<ApiResponse<List<CouponDto.MyCouponResponse>>>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().data()).hasSize(1);
            assertThat(response.getBody().data().get(0).templateName()).isEqualTo("10% 할인");
            assertThat(response.getBody().data().get(0).status()).isEqualTo(CouponStatus.AVAILABLE);
        }
    }

    @DisplayName("PATCH /api-admin/v1/user-coupons/{userCouponId}/block")
    @Nested
    class BlockCoupon {

        @DisplayName("ADMIN이 쿠폰을 차단하면 204가 반환되고 이후 상태가 BLOCKED가 된다.")
        @Test
        void returns204AndStatusBlocked_whenAdminBlocks() {
            // arrange
            var template = couponTemplateJpaRepository.save(
                new com.loopers.domain.coupon.CouponTemplateModel("10% 할인", CouponType.RATE, 10L, null, LocalDateTime.now().plusDays(7)));
            var userCoupon = userCouponJpaRepository.save(
                new com.loopers.domain.coupon.UserCouponModel(savedUser.getId(), template.getId()));

            // act
            var response = testRestTemplate.exchange(
                BLOCK_URL, HttpMethod.PATCH,
                new HttpEntity<>(adminHeaders),
                Void.class,
                userCoupon.getId()
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            var blocked = userCouponJpaRepository.findById(userCoupon.getId()).orElseThrow();
            assertThat(blocked.isBlocked()).isTrue();
        }
    }
}
