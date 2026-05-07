package com.timed.models;

import com.google.gson.annotations.SerializedName;

/**
 * Model cho Zoom OAuth Token Response
 */
public class ZoomOAuthToken {

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("token_type")
    private String tokenType; // Usually "Bearer"

    @SerializedName("expires_in")
    private long expiresIn; // Time in seconds

    @SerializedName("scope")
    private String scope;

    // Local timestamp to track token expiration
    private long expirationTime;

    public ZoomOAuthToken() {
    }

    public ZoomOAuthToken(String accessToken, String tokenType, long expiresIn) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.expirationTime = System.currentTimeMillis() + (expiresIn * 1000);
    }

    // Getters and Setters
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
        this.expirationTime = System.currentTimeMillis() + (expiresIn * 1000);
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
    }

    /**
     * Kiểm tra xem token đã hết hạn hay chưa
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }

    /**
     * Kiểm tra xem token có hợp lệ hay không
     */
    public boolean isValid() {
        return accessToken != null && !isExpired();
    }
}
