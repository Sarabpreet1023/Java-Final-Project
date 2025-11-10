package com.moviebooking.util;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * SeatLockManager keeps track of all active seat locks.
 * It ensures thread-safe updates and automatic cleanup of expired locks.
 */
public class SeatLockManager {

    // showId -> ReentrantLock (so only one thread can modify a show's XML at a time)
    private final ConcurrentHashMap<String, ReentrantLock> showLocks = new ConcurrentHashMap<>();

    // lockToken -> LockInfo
    private final ConcurrentHashMap<String, LockInfo> lockRegistry = new ConcurrentHashMap<>();

    /**
     * Represents a lock record.
     */
    private static class LockInfo {
        final String showId;
        final List<String> seats;
        final Instant expiry;

        LockInfo(String showId, List<String> seats, Instant expiry) {
            this.showId = showId;
            this.seats = seats;
            this.expiry = expiry;
        }
    }

    /**
     * Get or create a lock object for a show.
     */
    public ReentrantLock getLock(String showId) {
        return showLocks.computeIfAbsent(showId, k -> new ReentrantLock());
    }

    /**
     * Register a new lock after successful seat lock operation.
     */
    public void registerLock(String token, String showId, List<String> seats, Instant expiry) {
        lockRegistry.put(token, new LockInfo(showId, seats, expiry));
    }

    /**
     * Check if a lock token is still valid.
     */
    public boolean isLockValid(String token) {
        LockInfo info = lockRegistry.get(token);
        if (info == null) return false;
        if (Instant.now().isAfter(info.expiry)) {
            lockRegistry.remove(token);
            return false;
        }
        return true;
    }

    /**
     * Get all seats locked by a given token.
     */
    public List<String> getLockedSeats(String token) {
        LockInfo info = lockRegistry.get(token);
        return (info != null) ? info.seats : Collections.emptyList();
    }

    /**
     * Remove a lock (after booking is confirmed or expired).
     */
    public void releaseLock(String token) {
        lockRegistry.remove(token);
    }

    /**
     * Cleanup expired locks periodically (optional, can be called by scheduler).
     */
    public void cleanupExpiredLocks() {
        Instant now = Instant.now();
        lockRegistry.entrySet().removeIf(e -> now.isAfter(e.getValue().expiry));
    }
}
