package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

/**
 * 数据脱敏工具类。
 * <p>
 * 对敏感信息进行掩码处理，保护隐私。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DesensitizeUtils {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^(.+)@(.+)$");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{17}[\\dXx]$");

    /**
     * 手机号脱敏：138****1234
     */
    public static String phone(String phone) {
        if (StringUtils.isBlank(phone)) return phone;
        return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

    /**
     * 邮箱脱敏：z***@example.com
     */
    public static String email(String email) {
        if (StringUtils.isBlank(email)) return email;
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return email;
        String prefix = email.substring(0, 1);
        String suffix = email.substring(atIndex);
        return prefix + "***" + suffix;
    }

    /**
     * 身份证脱敏：110***********1234
     */
    public static String idCard(String idCard) {
        if (StringUtils.isBlank(idCard)) return idCard;
        if (idCard.length() < 8) return idCard;
        return idCard.substring(0, 3) + "*".repeat(idCard.length() - 7) + idCard.substring(idCard.length() - 4);
    }

    /**
     * 姓名脱敏：张*、张*明
     */
    public static String name(String name) {
        if (StringUtils.isBlank(name)) return name;
        if (name.length() == 1) return "*";
        if (name.length() == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
    }

    /**
     * 银行卡号脱敏：6222 **** **** 1234
     */
    public static String bankCard(String cardNo) {
        if (StringUtils.isBlank(cardNo)) return cardNo;
        if (cardNo.length() < 8) return cardNo;
        return cardNo.substring(0, 4) + " **** **** " + cardNo.substring(cardNo.length() - 4);
    }

    /**
     * 地址脱敏：保留前6个字符
     */
    public static String address(String address) {
        if (StringUtils.isBlank(address)) return address;
        if (address.length() <= 6) return address;
        return address.substring(0, 6) + "****";
    }

    /**
     * 密码脱敏：全部替换为 ****
     */
    public static String password(String password) {
        if (StringUtils.isBlank(password)) return password;
        return "****";
    }

    /**
     * 通用脱敏：保留前N位和后N位，中间替换为 *
     *
     * @param value    原始值
     * @param prefixLen 保留前N位
     * @param suffixLen 保留后N位
     */
    public static String mask(String value, int prefixLen, int suffixLen) {
        if (StringUtils.isBlank(value)) return value;
        int len = value.length();
        if (prefixLen + suffixLen >= len) return value;
        return value.substring(0, prefixLen)
                + "*".repeat(len - prefixLen - suffixLen)
                + value.substring(len - suffixLen);
    }

    /**
     * 通用脱敏：保留前N位，其余替换为 *
     */
    public static String maskPrefix(String value, int prefixLen) {
        return mask(value, prefixLen, 0);
    }
}
