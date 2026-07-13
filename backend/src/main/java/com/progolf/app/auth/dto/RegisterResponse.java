package com.progolf.app.auth.dto;

/** Registration response (spec: authentication): the new account's id and username — never the password. */
public record RegisterResponse(String id, String username) {
}
