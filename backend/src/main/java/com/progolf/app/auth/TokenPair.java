package com.progolf.app.auth;

/** An issued JWT pair (spec: authentication): a short-lived access token and a longer-lived refresh token. */
public record TokenPair(String accessToken, String refreshToken) {
}
