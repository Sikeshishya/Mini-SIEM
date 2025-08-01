package com.miniSIEM.repository;

import com.miniSIEM.model.LogEntry;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface LogRepository extends MongoRepository<LogEntry, String> {
    // Custom queries (add later)
}