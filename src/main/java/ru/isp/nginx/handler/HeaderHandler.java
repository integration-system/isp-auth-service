package ru.isp.nginx.handler;

import nginx.clojure.java.Constants;
import nginx.clojure.java.NginxJavaHeaderFilter;
import ru.isp.nginx.utils.StringIdentification;

import java.util.Map;

import static nginx.clojure.MiniConstants.HEADERS;

public  class HeaderHandler implements NginxJavaHeaderFilter {
    private static String COOKIE_KEY_NAME = "pixel-id";

    @Override
    public Object[] doFilter(int status, Map<String, Object> request, Map<String, Object> responseHeaders) {
        //NginxRequest req = (NginxRequest) request;
        Map<String, String> map = (Map) request.get(HEADERS);
        if (!map.containsKey(COOKIE_KEY_NAME)) {
            responseHeaders.put(COOKIE_KEY_NAME, StringIdentification.generate());
        } else {
            responseHeaders.put(COOKIE_KEY_NAME, map.get(COOKIE_KEY_NAME));
        }
        return Constants.PHASE_DONE;
    }
}
