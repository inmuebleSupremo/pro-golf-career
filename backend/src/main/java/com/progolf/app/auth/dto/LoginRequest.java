package com.progolf.app.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Login request body (spec: authentication): username and password, both required. */
public record LoginRequest(@NotBlank String username, @NotBlank String password) {
}
