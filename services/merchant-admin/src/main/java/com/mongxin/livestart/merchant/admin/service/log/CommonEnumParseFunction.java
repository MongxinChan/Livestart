package com.mongxin.livestart.merchant.admin.service.log;

import cn.hutool.core.text.CharSequenceUtil;
import com.mongxin.livestart.merchant.admin.common.enums.EventStatusEnum;
import com.mzt.logapi.service.IParseFunction;
import org.springframework.stereotype.Component;

/**
 * mzt-biz-log 自定义枚举解析函数
 * <p>
 * 将枚举 type 数值解析为可读的中文名称，用于操作日志模板中的动态值替换。
 * 用法示例：{COMMON_ENUM_PARSE{'EventStatusEnum_0'}} → "下架"
 */
@Component
public class CommonEnumParseFunction implements IParseFunction {

    private static final String EVENT_STATUS_ENUM_NAME = EventStatusEnum.class.getSimpleName();

    @Override
    public String functionName() {
        return "COMMON_ENUM_PARSE";
    }

    @Override
    public String apply(Object value) {
        try {
            String valueStr = value.toString();
            int lastUnderscoreIdx = valueStr.lastIndexOf('_');
            if (lastUnderscoreIdx < 0 || lastUnderscoreIdx >= valueStr.length() - 1) {
                throw new IllegalArgumentException("格式错误，需要 '枚举类_具体值' 的形式。");
            }

            String enumClassName = valueStr.substring(0, lastUnderscoreIdx);
            int enumValue = Integer.parseInt(valueStr.substring(lastUnderscoreIdx + 1));

            return findEnumValueByName(enumClassName, enumValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("下划线后面的值需要是整数。", e);
        }
    }

    private String findEnumValueByName(String enumClassName, int enumValue) {
        if (EVENT_STATUS_ENUM_NAME.equals(enumClassName)) {
            return findEventStatusDesc(enumValue);
        } else {
            return "未知枚举(" + enumClassName + ":" + enumValue + ")";
        }
    }

    /**
     * 根据 status 值查找演出状态描述
     */
    private String findEventStatusDesc(int status) {
        for (EventStatusEnum e : EventStatusEnum.values()) {
            if (e.getStatus() == status) {
                return e.getDescription();
            }
        }
        return "未知状态(" + status + ")";
    }
}
