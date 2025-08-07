package com.miniSIEM.repository;

import com.miniSIEM.model.LogEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface LogRepository extends MongoRepository<LogEntry, String> {

    // Find logs by IP address with pagination
    List<LogEntry> findByIp(String ip, Pageable pageable);

    // Find logs by log level with pagination
    List<LogEntry> findByLogLevel(String logLevel, Pageable pageable);

    // Find logs by source with pagination
    List<LogEntry> findBySource(String source, Pageable pageable);

    // Count methods for statistics
    long countByLogLevel(String logLevel);
    long countByTimestampAfter(Instant timestamp);
    long countByIp(String ip);
    long countBySource(String source);

    // Find logs after timestamp (for dashboard)
    List<LogEntry> findByTimestampAfter(Instant timestamp);

    // Custom query to handle multiple optional filters with pagination
    @Query("{ " +
            "$and: [ " +
            "  { $or: [ { 'ip': ?0 }, { $expr: { $eq: [?0, null] } } ] }, " +
            "  { $or: [ { 'logLevel': ?1 }, { $expr: { $eq: [?1, null] } } ] }, " +
            "  { $or: [ { 'source': ?2 }, { $expr: { $eq: [?2, null] } } ] } " +
            "] }")
    List<LogEntry> findByFilters(@Param("ip") String ip,
                                 @Param("logLevel") String logLevel,
                                 @Param("source") String source,
                                 Pageable pageable);

    // Find logs within date range
    @Query("{'timestamp': {$gte: ?0, $lte: ?1}}")
    List<LogEntry> findByDateRange(Instant start, Instant end, Pageable pageable);

    // Find recent logs by level
    @Query("{'logLevel': ?0, 'timestamp': {$gte: ?1}}")
    List<LogEntry> findRecentByLevel(String level, Instant since);

    // Aggregation query for top sources
    @Query(value = "{ }",
            fields = "{ 'source': 1, 'count': 1 }")
    List<Map<String, Object>> findTopSources();

    // Find suspicious activity (multiple entries from same IP)
    @Query("{ 'ip': ?0, 'timestamp': { $gte: ?1 } }")
    List<LogEntry> findByIpSince(String ip, Instant since);

    // Find failed login attempts
    @Query("{ 'message': { $regex: ?0, $options: 'i' }, 'timestamp': { $gte: ?1 } }")
    List<LogEntry> findByMessageContaining(String pattern, Instant since);

    // Find error logs in time range
    @Query("{ 'logLevel': 'ERROR', 'timestamp': { $gte: ?0, $lte: ?1 } }")
    List<LogEntry> findErrorsInRange(Instant start, Instant end);

    // Dashboard specific queries
    @Query("{ 'timestamp': { $gte: ?0 } }")
    List<LogEntry> findByTimestampAfter(Instant timestamp, Pageable pageable);

    // Get log count by hour for trends
    @Query(value = "{ 'timestamp': { $gte: ?0 } }", count = true)
    long countLogsSince(Instant since);

    // Find logs with high frequency from same IP (potential DDoS)
    @Query("{ 'ip': ?0, 'timestamp': { $gte: ?1, $lte: ?2 } }")
    List<LogEntry> findByIpInTimeRange(String ip, Instant start, Instant end);
}