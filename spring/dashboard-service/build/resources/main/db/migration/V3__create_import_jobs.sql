-- One row per Excel student import.
CREATE TABLE import_jobs (
    id            UUID         PRIMARY KEY,
    file_name     VARCHAR(255) NOT NULL,
    status        VARCHAR(20)  NOT NULL CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED')),
    total_rows    INT          NOT NULL DEFAULT 0,
    created_rows  INT          NOT NULL DEFAULT 0,
    skipped_rows  INT          NOT NULL DEFAULT 0,
    error_rows    INT          NOT NULL DEFAULT 0,
    result_json   TEXT,
    error_message VARCHAR(500),
    started_by    VARCHAR(64)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    finished_at   TIMESTAMPTZ
);

CREATE INDEX idx_import_jobs_created_at ON import_jobs (created_at DESC);
