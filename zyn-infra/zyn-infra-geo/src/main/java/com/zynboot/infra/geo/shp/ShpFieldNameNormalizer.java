package com.zynboot.infra.geo.shp;

import com.github.promeg.pinyinhelper.Pinyin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ShpFieldNameNormalizer {

    private ShpFieldNameNormalizer() {
    }

    static String normalize(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return "field";
        }

        List<String> tokens = tokenize(rawName);
        if (tokens.isEmpty()) {
            return "field";
        }

        StringBuilder normalized = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            String token = normalizeAsciiToken(tokens.get(i));
            if (token.isBlank()) {
                continue;
            }
            if (normalized.isEmpty()) {
                normalized.append(lowerFirst(token));
            } else {
                normalized.append(upperFirst(token));
            }
        }

        if (normalized.isEmpty()) {
            return "field";
        }
        if (Character.isDigit(normalized.charAt(0))) {
            return "field" + upperFirst(normalized.toString());
        }
        return normalized.toString();
    }

    private static List<String> tokenize(String rawName) {
        List<String> tokens = new ArrayList<>();
        StringBuilder asciiToken = new StringBuilder();
        for (int i = 0; i < rawName.length(); i++) {
            char current = rawName.charAt(i);
            if (Pinyin.isChinese(current)) {
                flushAsciiToken(asciiToken, tokens);
                tokens.add(Pinyin.toPinyin(current).toLowerCase(Locale.ROOT));
            } else if (Character.isLetterOrDigit(current)) {
                asciiToken.append(current);
            } else {
                flushAsciiToken(asciiToken, tokens);
            }
        }
        flushAsciiToken(asciiToken, tokens);
        return tokens;
    }

    private static void flushAsciiToken(StringBuilder asciiToken, List<String> tokens) {
        if (!asciiToken.isEmpty()) {
            tokens.add(asciiToken.toString());
            asciiToken.setLength(0);
        }
    }

    private static String normalizeAsciiToken(String token) {
        if (token.equals(token.toUpperCase(Locale.ROOT))) {
            return token.toLowerCase(Locale.ROOT);
        }
        return token;
    }

    private static String lowerFirst(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private static String upperFirst(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
