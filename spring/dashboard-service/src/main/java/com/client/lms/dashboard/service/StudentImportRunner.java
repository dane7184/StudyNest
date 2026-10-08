package com.client.lms.dashboard.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.client.lms.dashboard.dto.response.ImportResultResponse.RowResult;
import com.client.lms.dashboard.dto.response.ImportResultResponse.RowStatus;
import com.client.lms.dashboard.entity.AuditAction;
import com.client.lms.dashboard.entity.ImportJob;
import com.client.lms.dashboard.entity.Role;
import com.client.lms.dashboard.entity.User;
import com.client.lms.dashboard.event.AuditEventPublisher;
import com.client.lms.dashboard.exception.UserAlreadyExistsException;
import com.client.lms.dashboard.integration.client.ClassroomServiceClient;
import com.client.lms.dashboard.repository.ImportJobRepository;
import com.client.lms.dashboard.service.StudentImportService.ParsedRow;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Runs one student import in the background (separate bean so @Async works).
 * Each row is its own transaction, so one bad row does not undo the others.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudentImportRunner {

    private final ImportJobRepository jobs;
    private final UserAdminService userAdmin;
    private final ClassroomServiceClient classrooms;
    private final AuditEventPublisher audit;
    private final ObjectMapper objectMapper;

    @Async("importExecutor")
    public void run(UUID jobId, List<ParsedRow> rows) {
        ImportJob job = jobs.findById(jobId).orElseThrow();
        job.setStatus(ImportJob.Status.RUNNING);
        jobs.save(job);

        List<RowResult> results = new ArrayList<>();
        int created = 0, skipped = 0, errors = 0;

        for (ParsedRow r : rows) {
            try {
                User user = userAdmin.create(r.email(), r.fullName(), r.studentCode(), Role.STUDENT, r.language());
                if (r.classroomCode() != null) {
                    classrooms.enrolByCode(user.getId(), r.classroomCode());
                }
                results.add(new RowResult(r.rowNumber(), r.email(), RowStatus.CREATED, null));
                created++;
            } catch (UserAlreadyExistsException ex) {
                results.add(new RowResult(r.rowNumber(), r.email(), RowStatus.SKIPPED, "Already exists"));
                skipped++;
            } catch (Exception ex) {
                log.warn("Import row {} failed: {}", r.rowNumber(), ex.getMessage());
                results.add(new RowResult(r.rowNumber(), r.email(), RowStatus.ERROR, ex.getMessage()));
                errors++;
            }
        }

        job.setCreatedRows(created);
        job.setSkippedRows(skipped);
        job.setErrorRows(errors);
        job.setStatus(ImportJob.Status.COMPLETED);
        job.setFinishedAt(Instant.now());
        try {
            job.setResultJson(objectMapper.writeValueAsString(results));
        } catch (JsonProcessingException e) {
            job.setErrorMessage("Could not save row results");
        }
        jobs.save(job);
        audit.publish(AuditAction.STUDENT_IMPORT_FINISHED, "IMPORT_JOB", jobId,
                Map.of("created", created, "skipped", skipped, "errors", errors));
    }
}
