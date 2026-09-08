package com.sihan.local_review_platform.controller;

import com.sihan.local_review_platform.common.BusinessException;
import com.sihan.local_review_platform.common.GlobalExceptionHandler;
import com.sihan.local_review_platform.dto.SeckillResponse;
import com.sihan.local_review_platform.service.AuthService;
import com.sihan.local_review_platform.service.UserService;
import com.sihan.local_review_platform.service.VoucherOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CoreApiControllerTest {
    private AuthService authService;
    private VoucherOrderService voucherOrderService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        voucherOrderService = mock(VoucherOrderService.class);
        UserController userController = new UserController(authService, mock(UserService.class));
        VoucherOrderController orderController = new VoucherOrderController(voucherOrderService);
        mockMvc = MockMvcBuilders.standaloneSetup(userController, orderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void loginUsesUnifiedSuccessResponse() throws Exception {
        when(authService.login(any())).thenReturn("test-token");

        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"0412345678\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.token").value("test-token"));
    }

    @Test
    void businessFailureUsesUnifiedErrorResponse() throws Exception {
        when(authService.login(any())).thenThrow(
                new BusinessException("INVALID_CODE", "Invalid or expired verification code", HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"0412345678\",\"code\":\"000000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_CODE"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void seckillReturnsProcessingThroughUnifiedResponse() throws Exception {
        when(voucherOrderService.seckillVoucher(9L))
                .thenReturn(new SeckillResponse("request-1", "PROCESSING"));

        mockMvc.perform(post("/api/voucher/seckill/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requestId").value("request-1"))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));
    }
}
