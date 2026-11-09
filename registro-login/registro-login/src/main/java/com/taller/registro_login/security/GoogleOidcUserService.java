package com.taller.registro_login.security;

import com.taller.registro_login.model.User;
import com.taller.registro_login.service.UserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class GoogleOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcUserService delegate = new OidcUserService();
    private final UserService userService;

    public GoogleOidcUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser googleUser = delegate.loadUser(userRequest);
        if (!Boolean.TRUE.equals(googleUser.getAttribute("email_verified"))) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("unverified_email"),
                    "Google debe proporcionar una dirección de correo verificada.");
        }

        try {
            User account = userService.findOrCreateGoogleUser(
                    googleUser.getEmail(), googleUser.getFullName());
            return new ApplicationOidcUser(account, googleUser);
        } catch (IllegalArgumentException exception) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_google_account"), exception.getMessage(), exception);
        }
    }
}
