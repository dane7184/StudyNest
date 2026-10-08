package com.client.lms.dashboard.exception;

import com.client.lms.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ImportValidationException extends ApiException {

    public ImportValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, "IMPORT_INVALID", message);
    }
}
