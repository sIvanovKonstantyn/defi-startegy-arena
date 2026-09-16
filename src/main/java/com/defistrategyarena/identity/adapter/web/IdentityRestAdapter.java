package com.defistrategyarena.identity.adapter.web;

import com.defistrategyarena.identity.application.AccessTokenQuery;
import com.defistrategyarena.identity.application.AuthSessionResult;
import com.defistrategyarena.identity.application.DuplicateEmailException;
import com.defistrategyarena.identity.application.GetCurrentUser;
import com.defistrategyarena.identity.application.InvalidCredentialsException;
import com.defistrategyarena.identity.application.LoginWithPassword;
import com.defistrategyarena.identity.application.LoginWithPasswordCommand;
import com.defistrategyarena.identity.application.Logout;
import com.defistrategyarena.identity.application.RegisterWithPassword;
import com.defistrategyarena.identity.application.RegisterWithPasswordCommand;
import com.defistrategyarena.identity.application.UnauthorizedException;
import com.defistrategyarena.identity.domain.User;

public final class IdentityRestAdapter {

    private static final int STATUS_OK = 200;
    private static final int STATUS_CREATED = 201;
    private static final int STATUS_NO_CONTENT = 204;
    private static final int STATUS_BAD_REQUEST = 400;
    private static final int STATUS_UNAUTHORIZED = 401;
    private static final int STATUS_CONFLICT = 409;

    private final RegisterWithPassword registerWithPassword;
    private final LoginWithPassword loginWithPassword;
    private final Logout logout;
    private final GetCurrentUser getCurrentUser;

    public IdentityRestAdapter(IdentityRestAdapterDeps deps) {
        this.registerWithPassword = deps.registerWithPassword();
        this.loginWithPassword = deps.loginWithPassword();
        this.logout = deps.logout();
        this.getCurrentUser = deps.getCurrentUser();
    }

    public SessionHttpResponse signup(SignupHttpRequest request) {
        try {
            AuthSessionResult result =
                    registerWithPassword.execute(
                            RegisterWithPasswordCommand.create(
                                    new RegisterWithPasswordCommand(
                                            request.email(),
                                            request.password(),
                                            request.displayName())));
            return toSession(new SessionStatus(STATUS_CREATED, result));
        } catch (DuplicateEmailException exception) {
            return SessionHttpResponse.empty(new SessionHttpResponse.StatusCode(STATUS_CONFLICT));
        } catch (IllegalArgumentException exception) {
            return SessionHttpResponse.empty(new SessionHttpResponse.StatusCode(STATUS_BAD_REQUEST));
        }
    }

    public SessionHttpResponse login(LoginHttpRequest request) {
        try {
            AuthSessionResult result =
                    loginWithPassword.execute(
                            LoginWithPasswordCommand.create(
                                    new LoginWithPasswordCommand(
                                            request.email(), request.password())));
            return toSession(new SessionStatus(STATUS_OK, result));
        } catch (InvalidCredentialsException exception) {
            return SessionHttpResponse.empty(
                    new SessionHttpResponse.StatusCode(STATUS_UNAUTHORIZED));
        } catch (IllegalArgumentException exception) {
            return SessionHttpResponse.empty(new SessionHttpResponse.StatusCode(STATUS_BAD_REQUEST));
        }
    }

    public LogoutHttpResponse logout(AccessTokenHttpRequest request) {
        try {
            logout.execute(new AccessTokenQuery(request.accessToken()));
            return new LogoutHttpResponse(STATUS_NO_CONTENT);
        } catch (UnauthorizedException | IllegalArgumentException exception) {
            return new LogoutHttpResponse(STATUS_UNAUTHORIZED);
        }
    }

    public UserHttpResponse me(AccessTokenHttpRequest request) {
        try {
            User user = getCurrentUser.execute(new AccessTokenQuery(request.accessToken()));
            return new UserHttpResponse(STATUS_OK, user.displayName());
        } catch (UnauthorizedException | IllegalArgumentException exception) {
            return UserHttpResponse.empty(new UserHttpResponse.StatusCode(STATUS_UNAUTHORIZED));
        }
    }

    private static SessionHttpResponse toSession(SessionStatus status) {
        return new SessionHttpResponse(status.status(), status.result().accessToken());
    }

    private record SessionStatus(int status, AuthSessionResult result) {}

    public record IdentityRestAdapterDeps(
            RegisterWithPassword registerWithPassword,
            LoginWithPassword loginWithPassword,
            Logout logout,
            GetCurrentUser getCurrentUser) {}
}
