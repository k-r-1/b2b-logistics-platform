package com.boxoffice.ainotificationservice.ai.repository;

import com.boxoffice.ainotificationservice.ai.entity.prediction.PredictionLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PredictionLogRepository extends JpaRepository<PredictionLog, UUID> {

}
