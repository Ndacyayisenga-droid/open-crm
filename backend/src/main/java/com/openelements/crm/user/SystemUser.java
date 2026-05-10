package com.openelements.crm.user;

import java.util.UUID;

/**
 * Well-known SYSTEM-USER inserted by Flyway migration V30. Used as the author
 * for comments created by automated processes (legacy backfill, Brevo import,
 * scheduled jobs) where no human user is available.
 *
 * <p>The admin user list filters this row out via {@link #SUB} so it does not
 * appear in the UI.</p>
 */
public final class SystemUser {

    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public static final String SUB = "system";

    private SystemUser() {
    }
}
