package com.defistrategyarena.shared.infra.http.jetty;

import com.defistrategyarena.shared.infra.http.HttpResponse;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

record JettyResponseWriteData(Response response, Callback callback, HttpResponse httpResponse) {

    static JettyResponseWriteData create(JettyResponseWriteData draft) {
        return new JettyResponseWriteData(draft.response(), draft.callback(), draft.httpResponse());
    }
}
