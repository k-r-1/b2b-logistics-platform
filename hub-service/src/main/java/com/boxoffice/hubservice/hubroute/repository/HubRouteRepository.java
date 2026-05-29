package com.boxoffice.hubservice.hubroute.repository;

import com.boxoffice.hubservice.hubroute.entity.HubRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HubRouteRepository extends JpaRepository<HubRoute, UUID> {

    boolean existsByOriginHubIdAndDestinationHubId(UUID originHubId, UUID destinationHubId);

}

