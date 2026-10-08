package com.client.lms.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.util.List;

import com.client.lms.dashboard.exception.ImportValidationException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class StudentImportServiceParseTest {

    private final StudentImportService service = new StudentImportService(null, null, null, null);

    @Test
    void parse_readsValidRows_andSkipsBlankLines() throws Exception {
        MockMultipartFile file = excel(
                row("Full name", "Email", "Student ID", "Classroom code", "Language"),
                row("Sok Dara", "dara@school.edu", "S001", "ABC123", "km"),
                row("", "", "", "", ""),
                row("Chan Lina", "LINA@school.edu", "S002", "", ""));

        List<StudentImportService.ParsedRow> rows = service.parse(file);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).email()).isEqualTo("dara@school.edu");
        assertThat(rows.get(0).classroomCode()).isEqualTo("ABC123");
        assertThat(rows.get(1).email()).isEqualTo("lina@school.edu");
        assertThat(rows.get(1).language()).isEqualTo("en");
        assertThat(rows.get(1).rowNumber()).isEqualTo(4);
    }

    @Test
    void parse_rejectsDuplicateEmailInFile() throws Exception {
        MockMultipartFile file = excel(
                row("Full name", "Email", "Student ID"),
                row("A", "same@school.edu", "S1"),
                row("B", "same@school.edu", "S2"));

        assertThatThrownBy(() -> service.parse(file))
                .isInstanceOf(ImportValidationException.class)
                .hasMessageContaining("Row 3");
    }

    @Test
    void parse_rejectsWrongHeader() throws Exception {
        MockMultipartFile file = excel(row("Name", "Mail", "ID"), row("A", "a@b.co", "1"));

        assertThatThrownBy(() -> service.parse(file))
                .isInstanceOf(ImportValidationException.class)
                .hasMessageContaining("Header column 1");
    }

    // ---- helpers ----

    private static String[] row(String... cells) {
        return cells;
    }

    private static MockMultipartFile excel(String[]... rows) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Students");
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < rows[r].length; c++) {
                    row.createCell(c).setCellValue(rows[r][c]);
                }
            }
            wb.write(out);
            return new MockMultipartFile("file", "students.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }
}
