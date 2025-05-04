package org.coding;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import lombok.extern.java.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;


@Log
public class InMemoryBloomFilter<T> implements BloomFilter<T>{

    /*
    number of independent hash functions required to meet the expected false positivity probability (fpp)
    */

    private final int numHashFunctions; // optimal number of hash functions
    private final long bitSize; //total number of bits
    private final AtomicLongArray bitsArray;  // thread-safe bit array
    private final long expectedInsertions; //expected number of elements that can be inserted
    private final double fpp;  // false positivity probability
    private final AtomicLong insertionCount = new AtomicLong(0); // counter for tracking number of insertions
    private static final int LONG_SIZE = 64; //64 bits
    private static final double SCALING_THRESHOLD = 1.2; // 120% capacity triggers warning
    private static final HashFunction MURMUR3_128 = Hashing.murmur3_128();

    public InMemoryBloomFilter(long expectedInsertions, double fpp) {
        this.expectedInsertions = expectedInsertions;
        this.fpp = fpp;
        this.bitSize = (long) (-expectedInsertions * Math.log(fpp) / (Math.pow(Math.log(2), 2)));
        this.numHashFunctions = Math.max(1, (int) Math.round((double) bitSize / expectedInsertions * Math.log(2)));
        int arraySize = (int) ((bitSize + LONG_SIZE - 1) / LONG_SIZE);
        this.bitsArray = new AtomicLongArray(arraySize);
    }

    @Override
    public void add(T value) {
        long count = insertionCount.incrementAndGet();
        if(expectedInsertions > 0 && count > (expectedInsertions * SCALING_THRESHOLD)) {
            log.info("Bloom filter over capacity. FPP likely higher than expected!");
        }

        long[] hash = murmurHash128(value);
        long hash1 = hash[0];
        long hash2 = hash[1];
        for(int i=0;i<numHashFunctions;i++){
            long combinedHash = (hash1 + i*hash2) & Long.MAX_VALUE;
            long bitIndex = combinedHash%bitSize;
            setBit(bitIndex);
        }
    }

    @Override
    public boolean mightContain(T value) {
        long[] hash = murmurHash128(value);
        long hash1 = hash[0];
        long hash2 = hash[1];
        for (int i = 0; i < numHashFunctions; i++) {
            long combinedHash = (hash1 + i * hash2) & Long.MAX_VALUE;
            long bitIndex = combinedHash % bitSize;
            if (!getBit(bitIndex)) return false;
        }
        return true;
    }

    private boolean getBit(long bitIndex) {
        int longIndex = (int) (bitIndex / LONG_SIZE);
        long mask = 1L << (bitIndex % LONG_SIZE);
        return (bitsArray.get(longIndex) & mask) != 0;
    }

    private void setBit(long bitIndex) {
        int longIndex = (int) (bitIndex / LONG_SIZE);
        long mask = 1L << (bitIndex % LONG_SIZE);
        long oldVal, newVal;
        do {
            oldVal = bitsArray.get(longIndex);
            newVal = oldVal | mask;
        } while (!bitsArray.compareAndSet(longIndex, oldVal, newVal));
    }

    private static long[] murmurHash128(Object value) {
        byte[] data = value.toString().getBytes(StandardCharsets.UTF_8);
        HashCode hashCode = MURMUR3_128.hashBytes(data);

        ByteBuffer buffer = ByteBuffer.wrap(hashCode.asBytes()).order(ByteOrder.LITTLE_ENDIAN);
        long h1 = buffer.slice(0,8).getLong();
        long h2 = buffer.slice(8,8).getLong();
        return new long[]{h1, h2};
    }

    @Override
    public void logMetrics() {
        log.info("The expected insertions are: " + this.expectedInsertions);
        log.info("the fpp is: " + this.fpp);
        log.info("the bitsize is: " + this.bitSize);
        log.info("The number of hash functions is: " + this.numHashFunctions);
        log.info("the bits array size is: " + bitsArray.length());
        log.info("total memory needed: " + (bitSize/(8*1024*1024)) + " MB");
    }

}
