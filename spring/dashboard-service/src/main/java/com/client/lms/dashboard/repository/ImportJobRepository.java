package com.client.lms.dashboard.repository;

import java.util.UUID;

import com.client.lms.dashboard.entity.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobRepository extends JpaRepository<ImportJob, UUID> {
}
