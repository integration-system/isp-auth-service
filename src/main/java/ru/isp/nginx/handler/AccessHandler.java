package ru.isp.nginx.handler;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import nginx.clojure.java.ArrayMap;
import nginx.clojure.java.Constants;
import nginx.clojure.java.NginxJavaRequest;
import nginx.clojure.java.NginxJavaRingHandler;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.isp.nginx.entity.Endpoint;
import ru.isp.nginx.entity.RedisResponse;
import ru.isp.nginx.service.RedisService;
import ru.isp.nginx.utils.AppConfig;
import ru.isp.nginx.utils.RemoteConfig;

import java.net.URI;
import java.util.*;
import java.util.stream.Stream;

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
            Map<String, String> headers = (Map<String, String>) request.get(HEADERS);
            if (!headers.containsKey(RemoteConfig.APPLICATION_TOKEN_HEADER)) {
                return UNAUTHORIZED_RESPONSE;
            }
            String applicationToken = headers.get(RemoteConfig.APPLICATION_TOKEN_HEADER);
            Map<String, String> applicationIdentities = RedisService
                    .getApplicationIdentities(getRedisKey(applicationToken, AppConfig.INSTANCE_UUID));
            // In redis doesn't exist record with received token
            if (applicationIdentities == null || applicationIdentities.isEmpty()) {
                return UNAUTHORIZED_RESPONSE;
            }
            // headers.remove(RemoteConfig.APPLICATION_TOKEN_HEADER);
            // In record had received from redis don't exist all required fields with identities
            if (!setSystemIdentityHeaders(applicationIdentities, request)) {
                return UNAUTHORIZED_RESPONSE;
            }

            String appIdentity = applicationIdentities.get(APPLICATION_IDENTITY_FIELD_IN_DB);
            String uri = request.uri();
            String userToken = headers.get(RemoteConfig.USER_TOKEN_HEADER);
            String domainIdentity = applicationIdentities.get(DOMAIN_IDENTITY_FIELD_IN_DB);
            String deviceToken = headers.get(RemoteConfig.DEVICE_TOKEN_HEADER);
            String deviceIdentity = null;
            String userIdentity = null;
            RedisResponse permissions = RedisService.getPermissions(
                    getRedisKey(appIdentity, uri),
                    getRedisKey(uri),
                    getRedisKey(userToken, domainIdentity),
                    getRedisKey(deviceToken, domainIdentity)
            );
            //String applicationPermission = RedisService.getApplicationPermission(getRedisKey(appIdentity, uri));
            // It is not permitted to call this methodParts
            String applicationPermission = permissions.getApplicationPermission().get();
            if (applicationPermission != null && applicationPermission.equals("0")) {
                return UNAUTHORIZED_RESPONSE;
            }

            // ===== CHECK USER TOKEN =====
            if (!Strings.isBlank(userToken)) {
                userIdentity = permissions.getUserIdentity().get();
                // headers.remove(RemoteConfig.USER_TOKEN_HEADER);
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
                // headers.remove(RemoteConfig.DEVICE_TOKEN_HEADER);
            }
            String devicePermission = permissions.getDevicePermission().get();
            if (Strings.isBlank(deviceIdentity) && devicePermission != null && devicePermission.equals("0")) {
                return UNAUTHORIZED_RESPONSE;
            }
            request.setVariable(NGINX_PARAM_DEVICE_IDENTITY_HEADER_VALUE,
                    deviceIdentity != null ? deviceIdentity : BLANK_IDENTITY_HEADER_VALUE);

            String[] pathParts = new URI(uri).getPath().split("/");
            List<String> methodParts = Arrays.asList(pathParts).subList(1, pathParts.length - 1);
            String method = Strings.join(methodParts, '/');
            Endpoint endpoint = AppConfig.ENDPOINTS_PATH_MAP.get(method);
            if (endpoint != null && endpoint.isInner()) {
                String adminToken = headers.get(RemoteConfig.ADMIN_TOKEN_HEADER);
                if (Strings.isBlank(adminToken)) {
                    return UNAUTHORIZED_RESPONSE;
                }
                try {
                    Claims claims = Jwts.parser()
                            .setSigningKey(RemoteConfig.JWT_SECRET.getBytes())
                            .parseClaimsJws(adminToken)
                            .getBody();
                    Date expiration = claims.getExpiration();
                    if (expiration != null && expiration.before(new Date())) {
                        throw new RuntimeException("token expired");
                    }
                } catch (Exception ex) {
                    LOGGER.warn("received invalid admin token", ex);
                    return UNAUTHORIZED_RESPONSE;
                }
            }
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
