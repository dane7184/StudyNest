package com.client.lms.dashboard.controller;

import java.util.UUID;

import com.client.lms.dashboard.dto.response.ImportResultResponse;
import com.client.lms.dashboard.service.StudentImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Student import", description = "Create many students from one Excel file")
@RestController
@RequestMapping("/api/admin/users/import")
@RequiredArgsConstructor
public class StudentImportController {

    private final StudentImportService service;

    @Operation(summary = "Upload an .xlsx (Full name, Email, Student ID, Classroom code, Language). "
            + "Returns 202 with a job id; poll GET /{jobId} for the per-row result.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResultResponse> upload(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.start(file));
    }

    @GetMapping("/{jobId}")
    public ImportResultResponse get(@PathVariable UUID jobId) {
        return service.get(jobId);
    }
}
