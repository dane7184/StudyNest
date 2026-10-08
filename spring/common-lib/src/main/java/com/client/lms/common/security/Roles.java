package com.client.lms.common.security;

/**
 * Role names shared by all services. They match the Keycloak realm roles.
 */
public final class Roles {

    public static final String ADMIN = "ADMIN";
    public static final String INSTRUCTOR = "INSTRUCTOR";
    public static final String STUDENT = "STUDENT";

    private Roles() {}
}
