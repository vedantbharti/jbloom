package org.coding;

import lombok.extern.java.Log;

@Log
public class Main {
    public static void main(String[] args) {

        BloomFilter<String> inMemoryFactory = new InMemoryBloomFilter<>(1000000, 0.01);

        inMemoryFactory.logMetrics();

        inMemoryFactory.add("hello");
        inMemoryFactory.add("world");

        log.info("result for hello is " + inMemoryFactory.mightContain("hello")); // true
        log.info("result for world is " + inMemoryFactory.mightContain("world")); // true
        log.info("result for namaste is " + inMemoryFactory.mightContain("namaste")); // probably false

        BloomFilterTest bloomFilterTest = new BloomFilterTest();
        bloomFilterTest.addData();
        log.info("the false positivity rate is: " + bloomFilterTest.checkFalsePositivityRate());

    }
}