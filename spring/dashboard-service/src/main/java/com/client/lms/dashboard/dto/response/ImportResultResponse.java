package com.client.lms.dashboard.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.client.lms.dashboard.entity.ImportJob;

public record ImportResultResponse(
        UUID jobId,
        String fileName,
        ImportJob.Status status,
        int totalRows,
        int createdRows,
        int skippedRows,
        int errorRows,
        List<RowResult> rows,
        String errorMessage,
        Instant createdAt,
        Instant finishedAt
) {
    public enum RowStatus { CREATED, SKIPPED, ERROR }

    public record RowResult(int row, String email, RowStatus status, String message) {}
}
