package com.backend1.backend1.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

@RestControllerAdvice(basePackages = "com.backend1.backend1.controller")
public class ApiExceptionHandler {

    @ExceptionHandler(CustomerServiceUnavailableException.class)
    public ProblemDetail handleServiceUnavailable(CustomerServiceUnavailableException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        problem.setTitle("Kundtjänsten otillgänglig");
        problem.setType(URI.create("https://pensionat.se/errors/service-unavailable"));
        problem.setProperty("errorCode", "CUSTOMER_SERVICE_UNAVAILABLE");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(BookingConflictException.class)
    public ProblemDetail handleConflict(BookingConflictException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setTitle("Bokningskonflikt");
        problem.setType(URI.create("https://pensionat.se/errors/conflict"));
        problem.setProperty("errorCode", "ROOM_ALREADY_BOOKED");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(BookingValidationException.class)
    public ProblemDetail handleValidation(BookingValidationException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setTitle("Ogiltig bokning");
        problem.setType(URI.create("https://pensionat.se/errors/bad-request"));
        problem.setProperty("errorCode", "VALIDATION_FAILED");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleNotFound(IllegalArgumentException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Hittades inte");
        problem.setType(URI.create("https://pensionat.se/errors/not-found"));
        problem.setProperty("errorCode", "NOT_FOUND");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleBeanValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Ogiltig inmatning");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Valideringsfel");
        problem.setType(URI.create("https://pensionat.se/errors/bad-request"));
        problem.setProperty("errorCode", "VALIDATION_FAILED");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}