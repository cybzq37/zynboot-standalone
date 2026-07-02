package com.zynboot.kit.exception;

/**
 * 业务异常：在 {@link BaseException} 基础上补充 notFound 语义的快捷工厂方法。
 */
public class BizException extends BaseException {

    private static final long serialVersionUID = 1L;
    public static final String NOT_FOUND_CODE = "404";

    protected BizException(int statusCode, String code, String message) {
        super(statusCode, code, message);
    }

    public static BizException badRequest(String message) {
        return new BizException(400, BAD_REQUEST_CODE, message);
    }

    public static BizException notFound(String resource) {
        return new BizException(404, NOT_FOUND_CODE, resource + "不存在");
    }
}
