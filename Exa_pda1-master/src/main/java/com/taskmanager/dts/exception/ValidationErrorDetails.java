package com.taskmanager.dts.exception;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
public class ValidationErrorDetails extends ErrorDetails {
    private Map<String, String> validationErrors;

    public ValidationErrorDetails(LocalDateTime timestamp, int status, String error, String message, Map<String, String> validationErrors) {
        super(timestamp, status, error, message);
        this.validationErrors = validationErrors;
    }
}