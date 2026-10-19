package com.nirkap.consumption.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.nirkap.consumption.dto.CreateReservationRequest;
import com.nirkap.consumption.service.BalanceResult;
import com.nirkap.consumption.service.ReservationService;

import jakarta.validation.Valid;

@RestController
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    // 201 when reserved, 200 when this eventId was already recorded, 409 when
    // there is not enough credit.
    @PostMapping("/api/v1/reservations")
    public ResponseEntity<Map<String, Long>> reserve(@Valid @RequestBody CreateReservationRequest request) {
        BalanceResult result = reservationService.reserve(request.eventId(),
                request.userId(), request.serviceName(), request.tokens());
        HttpStatus status = result.duplicate() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(Map.of("remainingCredit", result.remainingCredit()));
    }
}
