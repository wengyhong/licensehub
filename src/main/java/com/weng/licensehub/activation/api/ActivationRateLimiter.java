package com.weng.licensehub.activation.api;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
@Component
public class ActivationRateLimiter {

    private final long requestsPerMinute;

    private final Cache<String, Bucket> buckets;



    public ActivationRateLimiter(

        @Value("${licensehub.rate-limit.activation.requests-per-minute}")

        long requestsPerMinute) {
        if (requestsPerMinute <= 0) {
            throw new IllegalArgumentException(
                    "requestsPerMinute must be positive");
        }

        this.requestsPerMinute = requestsPerMinute;

        buckets = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(Duration.ofMinutes(10))
                .build();
    }



    public boolean tryConsume(String clientAddress) {
        Bucket bucket = buckets.get(
                clientAddress,
                ignored -> createBucket());

        return bucket.tryConsume(1);
    }

    private Bucket createBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(requestsPerMinute)
                        .refillGreedy(
                                requestsPerMinute,
                                Duration.ofMinutes(1)))
                .build();
    }
}