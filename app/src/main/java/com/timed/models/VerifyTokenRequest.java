package com.timed.models;

/**
 * Request model for verifying a refresh token
 */
public class VerifyTokenRequest {
    private String refreshToken;

    public VerifyTokenRequest() {
    }

    public VerifyTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
