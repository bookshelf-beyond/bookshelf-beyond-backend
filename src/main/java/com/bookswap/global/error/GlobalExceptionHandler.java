package com.bookswap.global.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
		ErrorCode errorCode = e.errorCode();
		return ResponseEntity.status(errorCode.status()).body(ErrorResponse.of(errorCode, e.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.orElse(ErrorCode.INVALID_INPUT.message());
		return ResponseEntity.status(ErrorCode.INVALID_INPUT.status())
				.body(ErrorResponse.of(ErrorCode.INVALID_INPUT, message));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException e) {
		return ResponseEntity.status(ErrorCode.INVALID_INPUT.status())
				.body(ErrorResponse.of(ErrorCode.INVALID_INPUT));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
		log.error("Unhandled exception", e);
		return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.status())
				.body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
	}
}
