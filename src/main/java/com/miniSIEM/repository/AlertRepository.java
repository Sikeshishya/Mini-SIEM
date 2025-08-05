package com.miniSIEM.repository;

import com.miniSIEM.model.Alert;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface AlertRepository extends MongoRepository<Alert, String> {
    List<Alert> findBySourceIpAndResolvedFalse(String ip);
    List<Alert> findByResolvedFalse();
}