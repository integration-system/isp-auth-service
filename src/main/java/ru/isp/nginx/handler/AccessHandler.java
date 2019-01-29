package ru.isp.nginx.handler;

import nginx.clojure.java.ArrayMap;
import nginx.clojure.java.Constants;
import nginx.clojure.java.NginxJavaRequest;
import nginx.clojure.java.NginxJavaRingHandler;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.isp.nginx.entity.RedisResponse;
import ru.isp.nginx.service.RedisService;
import ru.isp.nginx.utils.AppConfig;
import ru.isp.nginx.utils.RemoteConfig;

import java.util.Map;

import static nginx.clojure.MiniConstants.*;
import static nginx.clojure.java.Constants.PHASE_DONE;
import static ru.isp.nginx.service.RedisService.*;
import static ru.isp.nginx.utils.Constant.*;

public class AccessHandler implements NginxJavaRingHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessHandler.class);

    private static final Object[] UNAUTHORIZED_RESPONSE = new Object[3];

    static {
        UNAUTHORIZED_RESPONSE[0] = NGX_HTTP_UNAUTHORIZED;
        UNAUTHORIZED_RESPONSE[1] = ArrayMap.create(CONTENT_TYPE, "text/html");
    }

    @Override
    public Object[] invoke(Map<String, Object> req) {
        try {
            NginxJavaRequest request = (NginxJavaRequest) req;
            UNAUTHORIZED_RESPONSE[2] = RemoteConfig.UNAUTHORIZED_HTML;
            // Remote config hadn't received or doesn't contain all required fields
            if (!RemoteConfig.isConfigReceivedAndValid() || !RedisService.isRedisClientInitialized()) {
                return UNAUTHORIZED_RESPONSE;
            }
            // ===== CHECK APPLICATION TOKEN =====
            // There isn't required header with application token
            Map<String, String> map = (Map<String, String>) request.get(HEADERS);
            if (!map.containsKey(RemoteConfig.APPLICATION_TOKEN_HEADER)) {
                return UNAUTHORIZED_RESPONSE;
            }
            String applicationToken = map.get(RemoteConfig.APPLICATION_TOKEN_HEADER);
            Map<String, String> applicationIdentities = RedisService
                    .getApplicationIdentities(getRedisKey(applicationToken, AppConfig.INSTANCE_UUID));
            // In redis doesn't exist record with received token
            if (applicationIdentities == null || applicationIdentities.isEmpty()) {
                return UNAUTHORIZED_RESPONSE;
            }
            // map.remove(RemoteConfig.APPLICATION_TOKEN_HEADER);
            // In record had received from redis don't exist all required fields with identities
            if (!setSystemIdentityHeaders(applicationIdentities, request)) {
                return UNAUTHORIZED_RESPONSE;
            }

            String appIdentity = applicationIdentities.get(APPLICATION_IDENTITY_FIELD_IN_DB);
            String uri = (String) request.get(Constants.URI);
            String userToken = map.get(RemoteConfig.USER_TOKEN_HEADER);
            String domainIdentity = applicationIdentities.get(DOMAIN_IDENTITY_FIELD_IN_DB);
            String deviceToken = map.get(RemoteConfig.DEVICE_TOKEN_HEADER);
            String deviceIdentity = null;
            String userIdentity = null;
            RedisResponse permissions = RedisService.getPermissions(
                    getRedisKey(appIdentity, uri),
                    getRedisKey(uri),
                    getRedisKey(userToken, domainIdentity),
                    getRedisKey(deviceToken, domainIdentity)
            );
            //String applicationPermission = RedisService.getApplicationPermission(getRedisKey(appIdentity, uri));
            // It is not permitted to call this method
            String applicationPermission = permissions.getApplicationPermission().get();
            if (applicationPermission != null && applicationPermission.equals("0")) {
                return UNAUTHORIZED_RESPONSE;
            }

            // ===== CHECK USER TOKEN =====
            if (!Strings.isBlank(userToken)) {
                userIdentity = permissions.getUserIdentity().get();
                // map.remove(RemoteConfig.USER_TOKEN_HEADER);
            }
            String userPermission = permissions.getUserPermission().get();
            if (Strings.isBlank(userIdentity) && userPermission != null && userPermission.equals("0")) {
                return UNAUTHORIZED_RESPONSE;
            }
            request.setVariable(NGINX_PARAM_USER_IDENTITY_HEADER_VALUE,
                    userIdentity != null ? userIdentity : BLANK_IDENTITY_HEADER_VALUE);

            // ===== CHECK DEVICE TOKEN =====
            if (!Strings.isBlank(deviceToken)) {
                deviceIdentity = permissions.getDeviceIdentity().get();
                // map.remove(RemoteConfig.DEVICE_TOKEN_HEADER);
            }
            String devicePermission = permissions.getDevicePermission().get();
            if (Strings.isBlank(deviceIdentity) && devicePermission != null && devicePermission.equals("0")) {
                return UNAUTHORIZED_RESPONSE;
            }
            request.setVariable(NGINX_PARAM_DEVICE_IDENTITY_HEADER_VALUE,
                    deviceIdentity != null ? deviceIdentity : BLANK_IDENTITY_HEADER_VALUE);
        } catch (Exception e) {
            LOGGER.error("CHECK ACCESS ERROR: ", e);
        }

        return PHASE_DONE;
    }

    private static String getRedisKey(String identity, String secondPart) {
        return identity + "|" + getRedisKey(secondPart);
    }

    private static String getRedisKey(String secondPart) {
        return secondPart.toLowerCase().replaceAll("/", "");
    }

    private static boolean setSystemIdentityHeaders(Map<String, String> appIdentities, NginxJavaRequest req) {
        if (!appIdentities.containsKey(SYSTEM_IDENTITY_FIELD_IN_DB)
                || !appIdentities.containsKey(DOMAIN_IDENTITY_FIELD_IN_DB)
                || !appIdentities.containsKey(SERVICE_IDENTITY_FIELD_IN_DB)
                || !appIdentities.containsKey(APPLICATION_IDENTITY_FIELD_IN_DB)) {
            LOGGER.warn("Record received from redis doesn't contain required fields, exists fields: {}",
                    appIdentities);
            return false;
        }
        req.setVariable(NGINX_PARAM_SYSTEM_IDENTITY_HEADER_VALUE, appIdentities.get(SYSTEM_IDENTITY_FIELD_IN_DB));
        req.setVariable(NGINX_PARAM_DOMAIN_IDENTITY_HEADER_VALUE, appIdentities.get(DOMAIN_IDENTITY_FIELD_IN_DB));
        req.setVariable(NGINX_PARAM_SERVICE_IDENTITY_HEADER_VALUE, appIdentities.get(SERVICE_IDENTITY_FIELD_IN_DB));
        req.setVariable(NGINX_PARAM_APP_IDENTITY_HEADER_VALUE, appIdentities.get(APPLICATION_IDENTITY_FIELD_IN_DB));
        req.setVariable(NGINX_PARAM_INSTANCE_HEADER_VALUE, AppConfig.INSTANCE_UUID);

        return true;
    }
}
