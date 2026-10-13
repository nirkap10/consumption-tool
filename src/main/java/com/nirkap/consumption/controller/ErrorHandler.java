package com.nirkap.consumption.controller;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import com.nirkap.consumption.dto.ErrorBody;

import jakarta.servlet.http.HttpServletRequest;

// Spring sends every exception thrown while handling a request here, for all
// controllers. This is the one place that decides the error answer and logs it.
@RestControllerAdvice
public class ErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);

    // @Valid failed, e.g. tokensUsed is 0 or eventId is missing.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> invalidFields(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(field -> field.getField() + ": " + field.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return answer(HttpStatus.BAD_REQUEST, message, request);
    }

    // The body is missing, is not JSON, or has a value of the wrong type.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorBody> unreadableBody(HttpMessageNotReadableException e, HttpServletRequest request) {
        return answer(HttpStatus.BAD_REQUEST,
                "Request body is missing, is not valid JSON, or has a value of the wrong type", request);
    }

    // A path or query value of the wrong type, e.g. a userId that is not a UUID.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorBody> wrongType(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        return answer(HttpStatus.BAD_REQUEST, e.getName() + ": not a valid value", request);
    }

    // Everything else. Our own 404/409/400 (ResponseStatusException) and
    // Spring's errors such as an unknown URL or wrong method already carry a
    // status, so we keep it. Anything without one is a real failure: 500, the
    // full stack trace in the log, and no internals in the answer.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> other(Exception e, HttpServletRequest request) {
        if (e instanceof ResponseStatusException known) {
            return answer(known.getStatusCode(), known.getReason(), request);
        }
        if (e instanceof ErrorResponse known) {
            return answer(known.getStatusCode(), known.getBody().getDetail(), request);
        }
        log.error("{} {} -> 500", request.getMethod(), request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorBody(500, "Internal error"));
    }

    private ResponseEntity<ErrorBody> answer(HttpStatusCode status, String message, HttpServletRequest request) {
        log.warn("{} {} -> {}: {}", request.getMethod(), request.getRequestURI(), status.value(), message);
        return ResponseEntity.status(status).body(new ErrorBody(status.value(), message));
    }
}
