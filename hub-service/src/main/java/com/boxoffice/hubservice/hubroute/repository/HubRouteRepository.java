package com.boxoffice.hubservice.hubroute.repository;

import com.boxoffice.hubservice.hubroute.entity.HubRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.UUID;

public interface HubRouteRepository extends JpaRepository<HubRoute, UUID>, QuerydslPredicateExecutor<HubRoute> {

    boolean existsByOriginHubIdAndDestinationHubId(UUID originHubId, UUID destinationHubId);

}

