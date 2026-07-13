package com.progolf.app.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Refresh request body (spec: authentication): the refresh token to exchange for a new access token. */
public record RefreshRequest(@NotBlank String refreshToken) {
}
