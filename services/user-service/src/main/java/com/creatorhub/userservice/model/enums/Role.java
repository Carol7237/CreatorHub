package com.creatorhub.userservice.model.enums;

/**
 * Single role per user (kept simple for this project).
 * Persisted as a STRING (see {@code @Enumerated(EnumType.STRING)} on the field)
 * so reordering the constants never corrupts existing data.
 */
public enum Role {
    USER,
    ADMIN
}
