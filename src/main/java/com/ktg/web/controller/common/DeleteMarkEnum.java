package com.ktg.web.controller.common;

public enum  DeleteMarkEnum {
    NODELETE(0, "未删除"),
    DELETED(2, "已删除");

    final int code;
    final String value;

    public int getCode() {
        return this.code;
    }

    public String getValue() {
        return this.value;
    }

    private DeleteMarkEnum(final int code, final String message) {
        this.code = code;
        this.value = message;
    }
}
