package com.sihan.local_review_platform.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
public class LoginRequest {
    @NotBlank
    @Pattern(regexp = "^[0-9+][0-9]{5,19}$")
    private String phone;
    @NotBlank
    private String code;
    @Pattern(regexp = "^(USER|MERCHANT)$")
    private String loginMode = "USER";
    public String getPhone() {
        return phone;
    }

    public String getCode() {
        return code;
    }

    public String getLoginMode() { return loginMode; }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setLoginMode(String loginMode) { this.loginMode = loginMode; }
}
