package ru.isp.nginx.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.isp.nginx.annotation.Value;

import java.lang.reflect.Field;
import java.util.Map;

public class RemoteConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteConfig.class);

    @Value("redis.ip")
    public static String REDIS_IP;
    @Value("redis.port")
    public static Integer REDIS_PORT;

    @Value("unauthorized.html")
    public static String UNAUTHORIZED_HTML;

    @Value("header.token.application")
    public static String APPLICATION_TOKEN_HEADER;
    @Value("header.token.user")
    public static String USER_TOKEN_HEADER;
    @Value("header.token.device")
    public static String DEVICE_TOKEN_HEADER;
    @Value("header.admin.token")
    public static String ADMIN_TOKEN_HEADER;

    @Value("header.identity.system")
    public static String SYSTEM_IDENTITY_HEADER;
    @Value("header.identity.domain")
    public static String DOMAIN_IDENTITY_HEADER;
    @Value("header.identity.service")
    public static String SERVICE_IDENTITY_HEADER;
    @Value("header.identity.application")
    public static String APPLICATION_IDENTITY_HEADER;
    @Value("header.identity.user")
    public static String USER_IDENTITY_HEADER;
    @Value("header.identity.device")
    public static String DEVICE_IDENTITY_HEADER;

    @Value("jwt.secret")
    public static String JWT_SECRET;

    private static boolean configReceivedAndValid;

    public static void initParameters(Map<String, Object> params) {
        Field[] allFields = RemoteConfig.class.getDeclaredFields();

        for (Field field : allFields) {
            if (field.isAnnotationPresent(Value.class)) {
                Value annotation = field.getAnnotation(Value.class);
                String value = annotation.value();
                Object variable = getConfigVariable(params, value);
                if (variable == null) {
                    LOGGER.error(createErrorMessage(value));
                    return;
                }

                field.setAccessible(true);
                try {
                    field.set(null, variable);
                } catch (IllegalAccessException e) {
                    LOGGER.error(createTypeErrorMessage(value, field.getType().getTypeName(),
                            variable.getClass().getSimpleName()));
                    return;
                }
            }
        }
        configReceivedAndValid = true;
    }

    private static Object getConfigVariable(Map<String, Object> params, String path) {
        if (params.containsKey(path)) return params.get(path);
        String[] strings = path.split("\\.");
        Object result = params;
        for (int i = 0; i < strings.length; i++) {
            if (!(result instanceof Map) || !((Map)result).containsKey(strings[i])) {
                return null;
            }
            result = ((Map)result).get(strings[i]);
        }
        return result;
    }

    public static boolean isConfigReceivedAndValid() {
        return configReceivedAndValid;
    }

    private static String createErrorMessage(String paramName) {
        return "Required remote config variable: <" + paramName + "> hadn't been specified, Config isn't acceptable";
    }

    private static String createTypeErrorMessage(String paramName, String type, Object receivedVar) {
        return "Something wrong with type of config variable: <" + paramName + ">, it must have type: <" + type
                + ">, but was received: <" + receivedVar + ">";
    }
}
