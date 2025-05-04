package org.coding;

public interface BloomFilter<T> {

    void add(T value);

    boolean mightContain(T value);

    public void logMetrics();
}
