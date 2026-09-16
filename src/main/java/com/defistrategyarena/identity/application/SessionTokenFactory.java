package com.defistrategyarena.identity.application;

public interface SessionTokenFactory {

    IssuedToken issue();

    String hash(RawToken token);

    record RawToken(String value) {}

    record IssuedToken(String rawToken, String tokenHash) {}
}
