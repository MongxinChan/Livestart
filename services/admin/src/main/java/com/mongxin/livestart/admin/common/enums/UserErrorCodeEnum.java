package com.mongxin.livestart.admin.common.enums;

import com.mongxin.livestart.admin.common.convention.errorCode.IErrorCode;

/**
 * @author Mongxin
 */

public enum UserErrorCodeEnum implements IErrorCode {

    USER_TOKEN_FAIL("A00200", "用户Token验证失败"),

    USER_EXIST("B002002", "用户记录已存在"),


    USER_NAME_EXIST("B002001", "用户名已存在"),

    USER_NULL("B000200", "用户记录不存在"),
    //枚举类型用,分割

    USER_SAVE_ERROR("B002003", "用户记录失败");

    private final String code;

    private final String errorMessage;

    UserErrorCodeEnum(String code, String errorMessage) {
        this.code = code;
        this.errorMessage = errorMessage;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String errorMessage() {
        return errorMessage;
    }
}
