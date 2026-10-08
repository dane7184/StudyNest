package com.client.lms.dashboard.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.client.lms.common.exception.ResourceNotFoundException;
import com.client.lms.dashboard.dto.response.ImportResultResponse;
import com.client.lms.dashboard.dto.response.ImportResultResponse.RowResult;
import com.client.lms.dashboard.entity.AuditAction;
import com.client.lms.dashboard.entity.ImportJob;
import com.client.lms.dashboard.event.AuditEventPublisher;
import com.client.lms.dashboard.exception.ImportValidationException;
import com.client.lms.dashboard.repository.ImportJobRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Use case (Admin): create many students from one Excel file.
 *
 * Excel layout (assumed): first sheet, header row, then one student per row.
 *   A: Full name   B: Email   C: Student ID   D: Classroom code (optional)   E: Language km|en (optional)
 *
 * The whole file is checked first (header, empty rows, duplicate emails, bad emails).
 * If it passes, a job is saved and the rows are created in the background by
 * {@link StudentImportRunner}. Each row ends as CREATED, SKIPPED (already exists) or ERROR.
 * The admin polls GET /api/admin/users/import/{jobId} for the result.
 */
@Service
@RequiredArgsConstructor
public class StudentImportService {

    private static final List<String> EXPECTED_HEADERS =
            List.of("full name", "email", "student id", "classroom code", "language");
    private static final int REQUIRED_HEADER_COUNT = 3;
    private static final int MAX_ROWS = 2000; // (assumed)
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final ImportJobRepository jobs;
    private final StudentImportRunner runner;
    private final AuditEventPublisher audit;
    private final ObjectMapper objectMapper;

    /**
     * Not @Transactional on purpose: the job row must be committed before the
     * background runner (another thread) looks it up.
     */
    public ImportResultResponse start(MultipartFile file) {
        List<ParsedRow> rows = parse(file);

        ImportJob job = ImportJob.builder()
                .id(UUID.randomUUID())
                .fileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "students.xlsx")
                .status(ImportJob.Status.PENDING)
                .totalRows(rows.size())
                .build();
        job = jobs.save(job);
        audit.publish(AuditAction.STUDENT_IMPORT_STARTED, "IMPORT_JOB", job.getId(),
                Map.of("rows", rows.size(), "file", job.getFileName()));

        runner.run(job.getId(), rows);
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public ImportResultResponse get(UUID jobId) {
        return toResponse(jobs.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Import job", jobId)));
    }

    // ---------- parsing ----------

    List<ParsedRow> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImportValidationException("The file is empty");
        }
        String name = file.getOriginalFilename();
        if (name == null || !(name.endsWith(".xlsx") || name.endsWith(".xls"))) {
            throw new ImportValidationException("Only .xlsx or .xls files are accepted");
        }

        List<ParsedRow> rows = new ArrayList<>();
        Set<String> seenEmails = new HashSet<>();
        DataFormatter fmt = new DataFormatter();

        try (InputStream in = file.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            validateHeader(sheet.getRow(0), fmt);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlank(row, fmt)) {
                    continue;
                }
                int rowNumber = i + 1; // Excel numbering
                String fullName = cell(row, 0, fmt);
                String email = cell(row, 1, fmt).toLowerCase();
                String studentCode = cell(row, 2, fmt);
                String classroomCode = cell(row, 3, fmt);
                String language = cell(row, 4, fmt).toLowerCase();

                if (fullName.isBlank()) {
                    throw new ImportValidationException("Row " + rowNumber + ": Full name is missing");
                }
                if (!EMAIL.matcher(email).matches()) {
                    throw new ImportValidationException("Row " + rowNumber + ": Email is not valid");
                }
                if (!seenEmails.add(email)) {
                    throw new ImportValidationException("Row " + rowNumber + ": Email appears twice in the file");
                }
                if (!language.isBlank() && !language.equals("km") && !language.equals("en")) {
                    throw new ImportValidationException("Row " + rowNumber + ": Language must be km or en");
                }
                rows.add(new ParsedRow(rowNumber, fullName, email,
                        studentCode.isBlank() ? null : studentCode,
                        classroomCode.isBlank() ? null : classroomCode,
                        language.isBlank() ? "en" : language));
            }
        } catch (IOException ex) {
            throw new ImportValidationException("Could not read the Excel file");
        }

        if (rows.isEmpty()) {
            throw new ImportValidationException("The file has no student rows");
        }
        if (rows.size() > MAX_ROWS) {
            throw new ImportValidationException("The file has more than " + MAX_ROWS + " rows");
        }
        return rows;
    }

    private static void validateHeader(Row header, DataFormatter fmt) {
        if (header == null) {
            throw new ImportValidationException("The first row must be the header: "
                    + String.join(", ", EXPECTED_HEADERS));
        }
        for (int i = 0; i < REQUIRED_HEADER_COUNT; i++) {
            String actual = cell(header, i, fmt).toLowerCase();
            if (!actual.equals(EXPECTED_HEADERS.get(i))) {
                throw new ImportValidationException("Header column " + (i + 1) + " must be '"
                        + EXPECTED_HEADERS.get(i) + "' but is '" + actual + "'");
            }
        }
    }

    private static String cell(Row row, int idx, DataFormatter fmt) {
        Cell c = row.getCell(idx);
        return c == null ? "" : fmt.formatCellValue(c).trim();
    }

    private static boolean isBlank(Row row, DataFormatter fmt) {
        for (int i = 0; i < 5; i++) {
            if (!cell(row, i, fmt).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private ImportResultResponse toResponse(ImportJob job) {
        List<RowResult> rows = List.of();
        if (job.getResultJson() != null) {
            try {
                rows = objectMapper.readValue(job.getResultJson(), new TypeReference<>() {});
            } catch (IOException ignored) {
                // leave empty
            }
        }
        return new ImportResultResponse(job.getId(), job.getFileName(), job.getStatus(),
                job.getTotalRows(), job.getCreatedRows(), job.getSkippedRows(), job.getErrorRows(),
                rows, job.getErrorMessage(), job.getCreatedAt(), job.getFinishedAt());
    }

    /** One validated Excel row. */
    public record ParsedRow(int rowNumber, String fullName, String email, String studentCode,
                            String classroomCode, String language) {}
}
