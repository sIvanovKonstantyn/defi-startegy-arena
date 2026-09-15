package com.defistrategyarena.shared.infra.http;

public record HttpRouteLookup(String method, String path) {

    public static HttpRouteLookup create(HttpRouteLookup draft) {
        return new HttpRouteLookup(draft.method(), draft.path());
    }
}
