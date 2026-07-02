package com.zynboot.kit.exception;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BaseExceptionTest {

    @Test
    void shouldNormalizeStatusCodeAndDefensivelyCopyDetails() {
        List<String> details = new ArrayList<>();
        details.add("name: invalid");

        BaseException exception = new BaseException(422, null, "Validation failed", null, details);
        details.add("age: invalid");

        assertThat(exception.getStatus()).isEqualTo(422);
        assertThat(exception.getCode()).isEqualTo(BaseException.BAD_REQUEST_CODE);
        assertThat(exception.getDetails()).containsExactly("name: invalid");
    }

    @Test
    void shouldCreateFactoryExceptionsWithExpectedDefaults() {
        BaseException businessException = BaseException.badRequest("Duplicate name");
        BaseException systemException = BaseException.internalError("Serialize failed", new IllegalStateException("boom"));

        assertThat(businessException.getStatus()).isEqualTo(400);
        assertThat(businessException.getCode()).isEqualTo(BaseException.BAD_REQUEST_CODE);
        assertThat(systemException.getStatus()).isEqualTo(500);
        assertThat(systemException.getCode()).isEqualTo(BaseException.INTERNAL_ERROR_CODE);
    }

    @Test
    void shouldProvideDefaultMessageAndPreserveFactoryDetails() {
        BaseException businessException = BaseException.badRequest("BIZ-001", null, List.of("name: duplicated"));
        BaseException systemException = BaseException.internalError("SYS-500", "", new IllegalStateException("boom"), List.of("trace: hidden"));

        assertThat(businessException.getMessage()).isEqualTo("Bad request");
        assertThat(businessException.getDetails()).containsExactly("name: duplicated");
        assertThat(systemException.getMessage()).isEqualTo("Internal server error");
        assertThat(systemException.getDetails()).containsExactly("trace: hidden");
    }
}
