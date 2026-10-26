package com.bookswap.global.error;

public record ErrorResponse(String code, String message) {

	public static ErrorResponse of(ErrorCode errorCode) {
		return new ErrorResponse(errorCode.name(), errorCode.message());
	}

	public static ErrorResponse of(ErrorCode errorCode, String message) {
		return new ErrorResponse(errorCode.name(), message);
	}
}
