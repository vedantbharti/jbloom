package org.coding;

import lombok.extern.java.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Log
public class BloomFilterTest {

    BloomFilter<String> bloomFilter = new InMemoryBloomFilter<>(1000000, 0.01);

    int falsePostives = 0;

    public void addData() {
        for(int i=0;i<1300000;i++) {
            String uuid = generateTimeBasedUUID();
            bloomFilter.add(uuid.toString());
        }
    }

    public double checkFalsePositivityRate() {
        for(int i=0;i<1000000;i++) {
            String uuid = generateTimeBasedUUID();
            if(bloomFilter.mightContain(uuid.toString())==true) {
                falsePostives++;
            };
        }

        double falsePositivity = falsePostives/1000000.0;

        if(falsePositivity<0.01) {
            log.info("False positives are within limits");
        } else {
            log.info("false positivity is increasing");
        }

        return falsePositivity;
    }

    public static String generateTimeBasedUUID() {
        long timestamp = System.currentTimeMillis();
        return String.valueOf(timestamp) + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

}
