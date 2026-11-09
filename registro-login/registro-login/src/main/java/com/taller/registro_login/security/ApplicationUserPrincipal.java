package com.taller.registro_login.security;

public interface ApplicationUserPrincipal {

    Long getUserId();

    String getFullName();

    String getEmail();
}
