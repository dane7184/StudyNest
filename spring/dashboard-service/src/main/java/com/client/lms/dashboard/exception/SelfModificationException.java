package com.client.lms.dashboard.exception;

import com.client.lms.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/** Admin may not disable, delete or demote their own account (assumed). */
public class SelfModificationException extends ApiException {

    public SelfModificationException() {
        super(HttpStatus.FORBIDDEN, "SELF_MODIFICATION", "You cannot change your own account here");
    }
}
