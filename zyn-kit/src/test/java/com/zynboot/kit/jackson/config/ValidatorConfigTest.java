package com.zynboot.kit.jackson.config;

import com.zynboot.kit.jackson.plugins.xss.Xss;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ValidatorConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class, ValidatorConfig.class));

    @Test
    void shouldUseFailFastValidation() {
        contextRunner.run(context -> {
            Validator validator = context.getBean(Validator.class);

            Set<ConstraintViolation<ValidationPayload>> violations =
                    validator.validate(new ValidationPayload("", "<script>alert(1)</script>"));

            assertThat(violations).hasSize(1);
        });
    }

    @Test
    void shouldRejectXssContent() {
        contextRunner.run(context -> {
            Validator validator = context.getBean(Validator.class);

            Set<ConstraintViolation<ValidationPayload>> violations =
                    validator.validate(new ValidationPayload("valid", "javascript:alert(1)"));

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("content");
        });
    }

    private record ValidationPayload(
            @NotBlank String name,
            @Xss String content
    ) {
    }
}
