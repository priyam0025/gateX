package com.jeezus.gatex.management.repository;

import com.jeezus.gatex.management.entity.Organization;
import com.jeezus.gatex.management.entity.OrganizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsBySlugIgnoreCase(String slug);

    List<Organization> findAllByStatusNot(OrganizationStatus status);

    Optional<Organization> findByIdAndStatusNot(Long id, OrganizationStatus status);
}
