package com.progolf.app.auth.dto;

/** Refresh response (spec: authentication): a freshly issued access token. */
public record AccessTokenResponse(String accessToken) {
}
