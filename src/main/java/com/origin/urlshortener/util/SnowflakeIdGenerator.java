package com.origin.urlshortener.util;

import org.springframework.stereotype.Component;

@Component
public class SnowflakeIdGenerator {
    private static final long EPOCH = 1288834974657L; // Twitter snowflake epoch (Nov 04, 2010, 01:42:54 UTC)
    
    // Classic Twitter Snowflake bit allocation
    // private static final long SIGN_BITS = 1L;        // Always 0, reserved for future use
    private static final long TIMESTAMP_BITS = 41L;  // Milliseconds since epoch
    private static final long DATACENTER_ID_BITS = 5L;  // 32 datacenters
    private static final long MACHINE_ID_BITS = 5L;  // 32 machines per datacenter
    private static final long SEQUENCE_BITS = 12L;   // 4096 sequences per millisecond

    // Maximum values for each component
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);  // 31
    private static final long MAX_MACHINE_ID = ~(-1L << MACHINE_ID_BITS);        // 31
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);            // 4095

    // Bit shifts for each component
    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS + DATACENTER_ID_BITS;
    private static final String BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    // Maximum timestamp value (69 years from epoch)
    private static final long MAX_TIMESTAMP = ~(-1L << TIMESTAMP_BITS);

    private final long datacenterId;
    private final long machineId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator() {
        // For simplicity, we'll use fixed values for datacenter and machine IDs
        // In a real distributed system, these would be configured per instance
        this.datacenterId = 1L;
        this.machineId = 1L;

        validateIds();
    }

    public String generateShortCode() {
        long id = nextId();
        return toBase62(id);
    }

    public synchronized long nextId() {
        long currentTimestamp = timeGen();

        // Handle clock drift
        if (currentTimestamp < lastTimestamp) {
            throw new RuntimeException(
                String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", 
                    lastTimestamp - currentTimestamp));
        }

        // If we're in the same millisecond, increment sequence
        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // Sequence overflow, wait for next millisecond
                currentTimestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // New millisecond, reset sequence
            sequence = 0;
        }

        lastTimestamp = currentTimestamp;
        return generateId(currentTimestamp, sequence);
    }

    private void validateIds() {
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(
                    String.format("Datacenter ID can't be greater than %d or less than 0", MAX_DATACENTER_ID));
        }
        if (machineId > MAX_MACHINE_ID || machineId < 0) {
            throw new IllegalArgumentException(
                    String.format("Machine ID can't be greater than %d or less than 0", MAX_MACHINE_ID));
        }
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long generateId(long timestamp, long sequence) {
        // Calculate timestamp bits (milliseconds since epoch)
        long timestampBits = timestamp - EPOCH;
        
        // Validate timestamp range
        if (timestampBits > MAX_TIMESTAMP) {
            throw new RuntimeException(
                String.format("Timestamp bits overflow. Maximum timestamp is %d milliseconds from epoch", 
                    MAX_TIMESTAMP));
        }

        // Generate ID with all components
        return (timestampBits << TIMESTAMP_SHIFT) |
               (datacenterId << DATACENTER_ID_SHIFT) |
               (machineId << MACHINE_ID_SHIFT) |
               sequence;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

    private String toBase62(long num) {
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(BASE62_CHARS.charAt((int) (num % 62)));
            num /= 62;
        }
        // Pad with leading zeros if necessary to maintain consistent length
        while (sb.length() < 11) {  // Updated to 11 for classic Snowflake (64 bits)
            sb.append('0');
        }
        return sb.reverse().toString();
    }
} 