package org.coding;


@FunctionalInterface
public interface BloomFilterFactory<T> {

    BloomFilter<T> create();

}
