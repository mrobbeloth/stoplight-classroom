package com.stoplight.classroom.service;

import com.stoplight.classroom.dto.TeacherSignupRequest;
import com.stoplight.classroom.dto.TeacherSignupResponse;
import com.stoplight.classroom.model.User;

import java.util.List;

/**
 * Pluggable backend for the public teacher signup + admin-approval workflow.
 *
 * <p>The current implementation is {@link LocalTeacherSignupProvider}, which writes to
 * the application's own {@code users} table. A future {@code CognitoTeacherSignupProvider}
 * can implement the same contract against an AWS Cognito User Pool without changing any
 * controllers or callers. The two are selected via the
 * {@code stoplight.auth.teacher-provider} property.</p>
 *
 * <p>All implementations are expected to:</p>
 * <ul>
 *   <li>Create new accounts in {@link com.stoplight.classroom.model.UserStatus#PENDING}.</li>
 *   <li>Apply the {@code .edu} (or otherwise configured) email-domain policy on signup.</li>
 *   <li>Reject duplicate usernames or emails with {@link IllegalArgumentException}.</li>
 *   <li>Treat approve/reject on a non-{@code PENDING} record as an
 *       {@link IllegalArgumentException}.</li>
 * </ul>
 */
public interface TeacherSignupProvider {

    /**
     * Create a new teacher account in {@code PENDING} status. The returned record exposes
     * only non-sensitive fields (no password hash, no role — role is always TEACHER for
     * this flow).
     */
    TeacherSignupResponse requestSignup(TeacherSignupRequest request);

    /** List all signups in the given status (typically {@code PENDING}) for admin review. */
    List<TeacherSignupResponse> listByStatus(com.stoplight.classroom.model.UserStatus status);

    /** Approve a pending signup. The account becomes able to log in. */
    TeacherSignupResponse approve(Long userId);

    /** Reject a pending signup. The account remains in the database but cannot log in. */
    TeacherSignupResponse reject(Long userId);

    /**
     * Look up a {@link User} by username. Returned to {@link AuthService} so it can
     * enforce status checks at login time. Implementations that don't store users locally
     * (e.g. a future Cognito provider) may return {@code null} and instead enforce status
     * inside their own login path.
     */
    User findForLogin(String username);
}
