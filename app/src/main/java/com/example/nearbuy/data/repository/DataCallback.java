package com.example.nearbuy.data.repository;

/**
 * DataCallback – generic callback for Firestore read operations that return a
 * typed result {@code <T>}.
 *
 * Usage:
 *   productRepository.getProductsNearby(lat, lng, radius, new DataCallback<List<Product>>() {
 *       public void onSuccess(List<Product> products) { ... }
 *       public void onError(Exception e)              { ... }
 *   });
 */
public interface DataCallback<T> {
    void onSuccess(T data);
    void onError(Exception exception);
}

