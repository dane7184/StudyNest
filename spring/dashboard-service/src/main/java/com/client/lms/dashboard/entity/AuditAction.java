package com.client.lms.dashboard.entity;

/** Admin actions recorded in the local audit log. Login events come from Keycloak. */
public enum AuditAction {
    USER_CREATED,
    USER_UPDATED,
    USER_DISABLED,
    USER_ENABLED,
    USER_DELETED,
    USER_ROLE_CHANGED,
    USER_UNLOCKED,
    USER_MFA_RESET,
    USER_PASSWORD_RESET_FORCED,
    STUDENT_IMPORT_STARTED,
    STUDENT_IMPORT_FINISHED,
    PASSWORD_POLICY_UPDATED,
    MFA_POLICY_UPDATED,
    CLASSROOM_ARCHIVED
}
