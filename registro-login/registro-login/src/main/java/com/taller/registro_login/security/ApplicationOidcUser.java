package com.taller.registro_login.security;

import com.taller.registro_login.model.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.List;

public class ApplicationOidcUser extends DefaultOidcUser implements ApplicationUserPrincipal {

    private final Long userId;
    private final String fullName;

    public ApplicationOidcUser(User account, OidcUser oidcUser) {
        super(List.of(new SimpleGrantedAuthority(account.getRole())),
                oidcUser.getIdToken(), oidcUser.getUserInfo(), "sub");
        this.userId = account.getId();
        this.fullName = account.getFullName();
    }

    @Override
    public Long getUserId() {
        return userId;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public String getEmail() {
        return getAttribute("email");
    }
}
