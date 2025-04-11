package com.healthMini.exceptionHadler;

public class DataNotFoundException extends RuntimeException {
	
	public DataNotFoundException(String message) {
		super(message);
	}

}