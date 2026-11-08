package com.ifg.warehouse.order.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Produces human-friendly order numbers such as {@code ORD-20261108-7K2QXM}.
 * Uniqueness is additionally enforced by a database constraint.
 */
@Component
public class OrderNumberGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O, 1/I
    private static final int SUFFIX_LENGTH = 6;

    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    public OrderNumberGenerator(Clock clock) {
        this.clock = clock;
    }

    public String next() {
        StringBuilder suffix = new StringBuilder(SUFFIX_LENGTH);
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            suffix.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return "ORD-" + LocalDate.now(clock).format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + suffix;
    }
}
