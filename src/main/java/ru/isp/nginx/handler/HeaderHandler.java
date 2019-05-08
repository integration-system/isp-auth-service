package ru.isp.nginx.handler;

import nginx.clojure.java.Constants;
import nginx.clojure.java.NginxJavaHeaderFilter;

import java.util.Map;

public  class HeaderHandler implements NginxJavaHeaderFilter {
    @Override
    public Object[] doFilter(int status, Map<String, Object> request, Map<String, Object> responseHeaders) {
        return Constants.PHASE_DONE;
    }
}
