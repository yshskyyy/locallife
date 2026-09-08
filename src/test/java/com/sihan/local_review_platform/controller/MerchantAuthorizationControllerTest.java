package com.sihan.local_review_platform.controller;

import com.sihan.local_review_platform.common.GlobalExceptionHandler;
import com.sihan.local_review_platform.dto.BrandResponse;
import com.sihan.local_review_platform.dto.UserSession;
import com.sihan.local_review_platform.entity.UserRole;
import com.sihan.local_review_platform.service.BrandService;
import com.sihan.local_review_platform.service.RoleGuard;
import com.sihan.local_review_platform.service.VoucherManagementService;
import com.sihan.local_review_platform.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MerchantAuthorizationControllerTest {
    private final BrandService brands = mock(BrandService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new BrandController(brands, new RoleGuard()))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void userCannotCreateBrand() throws Exception {
        UserContext.set(new UserSession(1L, "0499000001", "user_0001", UserRole.USER));

        mvc.perform(post("/api/brands").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Forbidden Brand\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Merchant role required"));
    }

    @Test
    void merchantCanCreateBrand() throws Exception {
        UserContext.set(new UserSession(2L, "0499000099", "merchant", UserRole.MERCHANT));
        when(brands.create(any())).thenReturn(new BrandResponse(1L, "Allowed Brand", null));

        mvc.perform(post("/api/brands").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Allowed Brand\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Allowed Brand"));
    }

    @Test
    void userCannotDeleteVoucher() throws Exception {
        UserContext.set(new UserSession(1L, "0499000001", "user_0001", UserRole.USER));
        MockMvc voucherMvc = MockMvcBuilders.standaloneSetup(
                        new VoucherAdminController(mock(VoucherManagementService.class), new RoleGuard()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        voucherMvc.perform(delete("/api/vouchers/7"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

}
