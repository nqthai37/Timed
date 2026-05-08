package com.timed.models;

/**
 * Request model for exchanging authorization code for tokens
 */
public class AuthCodeRequest {
    private String code;

    public AuthCodeRequest() {
    }

    public AuthCodeRequest(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
