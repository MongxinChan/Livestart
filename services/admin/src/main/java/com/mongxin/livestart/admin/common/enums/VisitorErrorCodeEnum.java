package com.mongxin.livestart.admin.common.enums;

import com.mongxin.livestart.admin.common.convention.errorCode.IErrorCode;

/**
 * 观演人业务错误码枚举
 *
 * @author Mongxin
 */
public enum VisitorErrorCodeEnum implements IErrorCode {

    VISITOR_NOT_FOUND("B003001", "观演人记录不存在"),

    VISITOR_CARD_DUPLICATE("B003002", "该证件已添加过，请勿重复添加"),

    VISITOR_CARD_FORMAT_ERROR("B003003", "证件号格式校验失败"),

    VISITOR_SAVE_ERROR("B003004", "观演人保存失败"),

    VISITOR_NOT_BELONG_TO_USER("B003005", "无权操作该观演人信息");

    private final String code;

    private final String errorMessage;

    VisitorErrorCodeEnum(String code, String errorMessage) {
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
