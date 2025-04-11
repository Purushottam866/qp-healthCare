package com.healthMini.exceptionHadler;


import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private int statusCode;
    private String message;
}
