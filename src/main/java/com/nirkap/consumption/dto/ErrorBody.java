package com.nirkap.consumption.dto;

// The body of every error answer, e.g. {"status": 404, "error": "User not found: ..."}.
public record ErrorBody(int status, String error) {
}
