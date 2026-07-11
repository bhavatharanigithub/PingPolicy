package com.pingpolicy.monitor.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Cache-aside layer around each poll cycle.
 *
 * PingPolicy hashes each observed response body and stores the hash in
 * Redis under {@code poll:{contractId}:lastHash} with a TTL. On the next
 * poll, if the new response hashes to the same value, we know the payload
 * — and therefore its shape — hasn't changed, so we can skip running it
 * through the (more expensive) JSON diff engine entirely.
 *
 * This is what drives the reduction in redundant work: stable endpoints
 * that don't change between polls no longer pay the diff cost every cycle.
 */
@Service
public class PollCacheService {

    private static final String HASH_KEY_PREFIX = "poll:";
    private static final String HASH_KEY_SUFFIX = ":lastHash";

    /** How long a cached hash is trusted before we force a full re-check anyway. */
    private static final Duration CACHE_TTL = Duration.ofHours(6);

    private final StringRedisTemplate redisTemplate;

    public PollCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Returns true if the given response body hashes to the same value we
     * saw last poll (i.e. this poll can be treated as a cache hit and the
     * diff engine can be skipped).
     */
    public boolean isUnchangedSinceLastPoll(String contractId, String responseBody) {
        String newHash = sha256(responseBody);
        String cachedHash = redisTemplate.opsForValue().get(cacheKey(contractId));
        return newHash.equals(cachedHash);
    }

    /** Persists the latest response hash for this contract, resetting the TTL. */
    public void recordHash(String contractId, String responseBody) {
        String hash = sha256(responseBody);
        redisTemplate.opsForValue().set(cacheKey(contractId), hash, CACHE_TTL);
    }

    public void evict(String contractId) {
        redisTemplate.delete(cacheKey(contractId));
    }

    private String cacheKey(String contractId) {
        return HASH_KEY_PREFIX + contractId + HASH_KEY_SUFFIX;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
