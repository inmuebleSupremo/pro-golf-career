package com.progolf.app.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Registration request body (spec: authentication): a desired username and password, both required. */
public record RegisterRequest(@NotBlank String username, @NotBlank String password) {
}
