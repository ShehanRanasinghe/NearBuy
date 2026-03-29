package com.example.nearbuy.data.repository;

/**
 * OperationCallback – callback for Firestore write operations (create, update, delete)
 * that produce no return value.
 *
 * Usage:
 *   authRepository.register(name, email, phone, password, new OperationCallback() {
 *       public void onSuccess()              { ... }
 *       public void onError(Exception e)     { ... }
 *   });
 */
public interface OperationCallback {
    void onSuccess();
    void onError(Exception exception);
}

