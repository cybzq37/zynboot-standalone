package com.zynboot.kit.jackson.plugins.sensitive;

import com.zynboot.kit.util.DesensitizeUtils;

import java.util.function.Function;

/**
 * 脱敏策略，统一委托 {@link DesensitizeUtils}。
 */
public enum SensitiveStrategy {

    ID_CARD(DesensitizeUtils::idCard),
    PHONE(DesensitizeUtils::phone),
    ADDRESS(DesensitizeUtils::address),
    EMAIL(DesensitizeUtils::email),
    BANK_CARD(DesensitizeUtils::bankCard),
    NAME(DesensitizeUtils::name),
    PASSWORD(DesensitizeUtils::password);

    private final Function<String, String> desensitizer;

    SensitiveStrategy(Function<String, String> desensitizer) {
        this.desensitizer = desensitizer;
    }

    public Function<String, String> desensitizer() {
        return desensitizer;
    }

    public String mask(String value) {
        return desensitizer.apply(value);
    }
}
