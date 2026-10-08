package com.client.lms.dashboard.entity;

public enum UserStatus {
    /** Can log in. */
    ACTIVE,
    /** Admin disabled the account. Cannot log in. */
    DISABLED,
    /** Soft-deleted (assumed). Hidden from lists, data kept. */
    DELETED
}
