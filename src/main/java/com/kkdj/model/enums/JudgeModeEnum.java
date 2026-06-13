package com.kkdj.model.enums;

import lombok.Getter;

/**
 * 判题模式枚举
 */
@Getter
public enum JudgeModeEnum {

    DEFAULT("DEFAULT", "默认（精确匹配）"),
    IGNORE_SPACE("IGNORE_SPACE", "忽略多余空格"),
    IGNORE_CASE("IGNORE_CASE", "忽略大小写"),
    FLOAT("FLOAT", "浮点数精度比较"),
    MULTI_ANSWER("MULTI_ANSWER", "多解判断"),
    TESTLIB("TESTLIB", "自定义SPJ判题");

    private final String value;

    private final String text;

    JudgeModeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     */
    public static JudgeModeEnum getEnumByValue(String value) {
        if (value == null) {
            return DEFAULT;
        }
        for (JudgeModeEnum mode : JudgeModeEnum.values()) {
            if (mode.getValue().equals(value)) {
                return mode;
            }
        }
        return DEFAULT;
    }
}
