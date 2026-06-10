package com.kkdj.model.enums;

import lombok.Getter;

/**
 * 题目难度枚举
 */
@Getter
public enum QuestionDifficultyEnum {

    EASY(0, "简单"),
    MEDIUM(1, "中等"),
    HARD(2, "困难");

    private final Integer value;

    private final String text;

    QuestionDifficultyEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     */
    public static QuestionDifficultyEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (QuestionDifficultyEnum difficultyEnum : QuestionDifficultyEnum.values()) {
            if (difficultyEnum.getValue().equals(value)) {
                return difficultyEnum;
            }
        }
        return null;
    }

    /**
     * 根据 value 获取文本
     */
    public static String getTextByValue(Integer value) {
        QuestionDifficultyEnum difficultyEnum = getEnumByValue(value);
        return difficultyEnum != null ? difficultyEnum.getText() : "未知";
    }
}
