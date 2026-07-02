package com.zynboot.kit.jackson.plugins.xss;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * 自定义xss校验注解实现
 *
 * @author lichunqing
 */
public class XssValidator implements ConstraintValidator<Xss, String> {

    private static final Pattern XSS_PATTERN = Pattern.compile(
            "(?i)"
                    + "(<\\s*script[^>]*>)"                   // <script...>
                    + "|(<\\s*img[^>]*>)"                     // <img...>
                    + "|(<\\s*iframe[^>]*>)"                   // <iframe...>
                    + "|(<\\s*object[^>]*>)"                   // <object...>
                    + "|(<\\s*embed[^>]*>)"                    // <embed...>
                    + "|(<\\s*link[^>]*>)"                     // <link...>
                    + "|(<\\s*style[^>]*>)"                    // <style...>
                    + "|(<\\s*svg[^>]*>)"                      // <svg...>
                    + "|(<\\s*marquee[^>]*>)"                  // <marquee...>
                    + "|(<\\s*base[^>]*>)"                     // <base...>
                    + "|(<\\s*form[^>]*>)"                     // <form...>
                    + "|(javascript\\s*:)"                     // javascript:
                    + "|(vbscript\\s*:)"                       // vbscript:
                    + "|(data\\s*:.*?(?:text/html|application/xhtml))"  // data:text/html
                    + "|((?:on[a-z]+)\\s*=)"                   // on*= event handlers
                    + "|(expression\\s*\\()"                   // CSS expression()
                    + "|(<[^>]*\\bon\\w+\\s*=)"                // <tag on*=>
                    + "|(<[^>]*\\bstyle\\s*=\\s*[\"'].*?expression)" // style with expression
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return !XSS_PATTERN.matcher(value).find();
    }

}
