package com.timed.repositories;

public interface RepositoryCallback<T> {
    void onSuccess(T result);
    void onFailure(String errorMessage);
}
