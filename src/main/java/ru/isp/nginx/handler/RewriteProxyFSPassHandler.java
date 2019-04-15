package ru.isp.nginx.handler;

import nginx.clojure.java.Constants;
import nginx.clojure.java.NginxJavaRequest;
import nginx.clojure.java.NginxJavaRingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.isp.nginx.utils.AppConfig;

import java.io.IOException;
import java.util.Map;

import static nginx.clojure.java.Constants.PHASE_DONE;

public class RewriteProxyFSPassHandler implements NginxJavaRingHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RewriteProxyFSPassHandler.class);

    @Override
    public Object[] invoke(Map<String, Object> req) throws IOException {
        try {
            if (AppConfig.PROXY_FILE_STORAGE_ADDRESS.isEmpty()) {
                LOGGER.warn("No one files storage service addresses was found");
                return PHASE_DONE;
            }
            String uri = (String) req.get(Constants.URI);
            String  proxyUrl = AppConfig.PROXY_FILE_STORAGE_ADDRESS.get(0);
            ((NginxJavaRequest) req).setVariable("proxyFilesIp", proxyUrl + uri);
        } catch (Exception e) {
            LOGGER.error("PROXY ERROR: ", e);
        }
        return PHASE_DONE;
    }
}
