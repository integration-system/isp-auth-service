package ru.isp.nginx.handler;

import nginx.clojure.java.Constants;
import nginx.clojure.java.NginxJavaRequest;
import nginx.clojure.java.NginxJavaRingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.isp.nginx.service.RedisService;
import ru.isp.nginx.utils.AppConfig;

import java.util.Map;

import static nginx.clojure.java.Constants.PHASE_DONE;
import static ru.isp.nginx.JvmInitHandler.BALANCING_MAP;
import static ru.isp.nginx.utils.Constant.BALANCING_SHARED_MAP;

public class RewriteProxyPassHandler implements NginxJavaRingHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RewriteProxyPassHandler.class);

    @Override
    public Object[] invoke(Map<String, Object> req) {
        try {
            if (!RedisService.isRedisClientInitialized()) {
                LOGGER.warn("REDIS client isn't ready");
                return PHASE_DONE;
            }
            if (AppConfig.PROXY_ADDRESS.isEmpty()) {
                LOGGER.warn("No one CONVERT service addresses was found");
                return PHASE_DONE;
            }
            String uri = (String) req.get(Constants.URI);
            String  proxyUrl = AppConfig.PROXY_ADDRESS.size() == 1 ? AppConfig.PROXY_ADDRESS.get(0) : getProxyServer();
            ((NginxJavaRequest) req).setVariable("proxyIp", proxyUrl + uri);
        } catch (Exception e) {
            LOGGER.error("PROXY ERROR: ", e);
        }
        return PHASE_DONE;
    }

    public static synchronized String getProxyServer() {
        int oldValue = BALANCING_MAP.getInt(BALANCING_SHARED_MAP);
        String route = AppConfig.PROXY_ADDRESS.get(oldValue);
        BALANCING_MAP.put(BALANCING_SHARED_MAP, (oldValue + 1) % AppConfig.PROXY_ADDRESS.size());
        return route;
    }
}
