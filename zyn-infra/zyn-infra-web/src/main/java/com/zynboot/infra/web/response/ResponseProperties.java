package com.zynboot.infra.web.response;

import com.zynboot.kit.response.ApiResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "zyn.web.response")
public class ResponseProperties {

    private boolean enabled = true;
    private String successCode = ApiResponse.DEFAULT_SUCCESS_CODE;
    private String successMessage = ApiResponse.DEFAULT_SUCCESS_MESSAGE;

    public void setSuccessCode(String successCode) {
        this.successCode = successCode == null || successCode.isBlank()
                ? ApiResponse.DEFAULT_SUCCESS_CODE
                : successCode;
    }

    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage == null || successMessage.isBlank()
                ? ApiResponse.DEFAULT_SUCCESS_MESSAGE
                : successMessage;
    }
}
