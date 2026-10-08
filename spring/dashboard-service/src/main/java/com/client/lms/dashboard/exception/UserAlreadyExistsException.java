package com.client.lms.dashboard.exception;

import com.client.lms.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends ApiException {

    public UserAlreadyExistsException(String email) {
        super(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", "A user with email " + email + " already exists");
    }
}
