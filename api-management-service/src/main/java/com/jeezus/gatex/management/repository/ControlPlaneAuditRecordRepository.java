package com.jeezus.gatex.management.repository;

import com.jeezus.gatex.management.controller.ControlPlaneAuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ControlPlaneAuditRecordRepository extends JpaRepository<ControlPlaneAuditRecord, Long> {

    List<ControlPlaneAuditRecord> findAllByOrganizationIdOrderByCreatedAtDesc(Long organizationId);
}
