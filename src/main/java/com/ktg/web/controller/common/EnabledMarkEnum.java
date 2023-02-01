package com.ktg.web.controller.common;

public enum  EnabledMarkEnum {
    NOENABLED(1, "未启用"),
    ENABLED(0, "已启用");

    final int code;
    final String value;

    public int getCode() {
        return this.code;
    }

    public String getValue() {
        return this.value;
    }

    private EnabledMarkEnum(final int code, final String message) {
        this.code = code;
        this.value = message;
    }
}