package com.nirkap.consumption.service;

// What a usage report or a grant returns. duplicate is true when its eventId was
// already recorded and nothing changed.
public record BalanceResult(long remainingCredit, boolean duplicate) {
}
