package com.progolf.app.auth.dto;

/** Login response (spec: authentication): the issued access and refresh tokens. */
public record TokenResponse(String accessToken, String refreshToken) {
}
