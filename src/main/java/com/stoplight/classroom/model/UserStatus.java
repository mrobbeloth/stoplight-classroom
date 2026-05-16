package com.stoplight.classroom.model;

/**
 * Lifecycle state of a {@link User} account.
 *
 * <p>Existing users (created by {@code AdminSeedConfig} or via the admin user-management
 * API) default to {@link #APPROVED} so their login continues to work without migration.
 * Users created via the public teacher signup flow start as {@link #PENDING} and require
 * admin approval before they can log in.</p>
 */
public enum UserStatus {
    /** Awaiting admin review. The user cannot log in. */
    PENDING,
    /** Approved by an admin (or created directly by one). The user can log in normally. */
    APPROVED,
    /** Rejected by an admin. The user cannot log in. */
    REJECTED
}
