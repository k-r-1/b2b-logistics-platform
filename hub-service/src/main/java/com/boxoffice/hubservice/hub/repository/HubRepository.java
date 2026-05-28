package com.boxoffice.hubservice.hub.repository;

import com.boxoffice.hubservice.hub.entity.Hub;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HubRepository extends JpaRepository<Hub, UUID> {

    boolean existsByName(String name);

}
