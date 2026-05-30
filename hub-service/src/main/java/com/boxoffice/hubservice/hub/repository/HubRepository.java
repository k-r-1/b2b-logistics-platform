package com.boxoffice.hubservice.hub.repository;

import com.boxoffice.hubservice.hub.entity.Hub;
import com.boxoffice.hubservice.hub.entity.HubType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;
import java.util.UUID;

public interface HubRepository extends JpaRepository<Hub, UUID>, QuerydslPredicateExecutor<Hub> {
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, UUID id);
    List<Hub> findAllByHubType(HubType hubType);
}
