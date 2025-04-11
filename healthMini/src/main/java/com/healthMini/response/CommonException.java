package com.healthMini.response;

public class CommonException extends RuntimeException{

	String message;

	public CommonException(String message) {

		this.message = message;
	}
}
