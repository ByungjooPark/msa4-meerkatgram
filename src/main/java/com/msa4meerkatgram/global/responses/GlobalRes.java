package com.msa4meerkatgram.global.responses;

public record GlobalRes<T>(
    String code
    , String message
    , T data
) {
    public static <T> GlobalRes<T> from(CustomResponseCode customResponseCode, T data) {
        return new GlobalRes<>(customResponseCode.getCode(), customResponseCode.name(), data);
    }

    public static GlobalRes<Void> from(CustomResponseCode customResponseCode) {
        return new GlobalRes<>(customResponseCode.getCode(), customResponseCode.name(), null);
    }

    public static <T> GlobalRes<T> success(T data) {
        return GlobalRes.<T>from(CustomResponseCode.SUCCESS, data);
    }

    public static GlobalRes<Void> success() {
        return GlobalRes.<Void>from(CustomResponseCode.SUCCESS);
    }
}
