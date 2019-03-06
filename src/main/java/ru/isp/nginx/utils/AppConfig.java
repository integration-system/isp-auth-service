package ru.isp.nginx.utils;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AppConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);

    public static List<String> PROXY_ADDRESS = new ArrayList<>();
    public static List<String> PROXY_MDM_ADDRESS = new ArrayList<>();

    public static String CONFIG_SERVICE_IP;
    public static String CONFIG_SERVICE_HOST;
    public static String CONFIG_SERVICE_PORT = "9000";
    private static final String CONFIG_SERVICE_IP_PARAM = "config.server.ip";

    public static String INSTANCE_UUID;
    private static final String INSTANCE_UUID_PARAM = "instance.uuid";

    public static String ISP_CONVERTER_EVENT_NAME;
    private static final String ISP_CONVERTER_EVENT_NAME_PARAM = "isp.converter.event";

    public static String MDM_API_EVENT_NAME;
    private static final String MDM_API_EVENT_NAME_PARAM = "mdm.adapter.event";

    public static String MODULE_NAME;
    private static final String MODULE_NAME_PARAM = "module.name";
    private static final String EVENT_MODULE_CONNECT_SUFFIX = "_MODULE_CONNECTED";

    private static boolean variablesInitialized;

    public static void initParameters(Map<String, String> params) {
        CONFIG_SERVICE_IP = params.get(CONFIG_SERVICE_IP_PARAM);
        INSTANCE_UUID = params.get(INSTANCE_UUID_PARAM);
        MODULE_NAME = params.get(MODULE_NAME_PARAM);
        MDM_API_EVENT_NAME = params.get(MDM_API_EVENT_NAME_PARAM);
        ISP_CONVERTER_EVENT_NAME = params.get(ISP_CONVERTER_EVENT_NAME_PARAM);
        if (MDM_API_EVENT_NAME != null && !MDM_API_EVENT_NAME.isEmpty()) {
            MDM_API_EVENT_NAME += EVENT_MODULE_CONNECT_SUFFIX;
        }
        if (ISP_CONVERTER_EVENT_NAME != null && !ISP_CONVERTER_EVENT_NAME.isEmpty()) {
            ISP_CONVERTER_EVENT_NAME += EVENT_MODULE_CONNECT_SUFFIX;
        }

        if (Strings.isBlank(CONFIG_SERVICE_IP)) {
            LOGGER.error(createErrorMessage(CONFIG_SERVICE_IP_PARAM, "config.server.ip=10.250.9.114:5000"));
        } else if (Strings.isBlank(INSTANCE_UUID)) {
            LOGGER.error(createErrorMessage(INSTANCE_UUID_PARAM, "instance.uuid=bf482806-0c3d-4e0d-b9d4-12c037b12d70"));
        } else if (Strings.isBlank(MODULE_NAME)) {
            LOGGER.error(createErrorMessage(MODULE_NAME_PARAM, "module.name=auth"));
        } else {
            variablesInitialized = true;
        }

        String[] split = CONFIG_SERVICE_IP.split(":");
        if (split.length == 3) {
            CONFIG_SERVICE_HOST = split[0] + ":" + split[1];
        } else if (split.length == 2 && !split[0].startsWith("http")) {
            CONFIG_SERVICE_HOST = split[0];
        } else if (split.length == 2 && split[0].startsWith("http")) {
            CONFIG_SERVICE_HOST = CONFIG_SERVICE_IP;
        } else if (split.length == 1) {
            CONFIG_SERVICE_HOST = CONFIG_SERVICE_IP;
        }
    }

    public static boolean isVariablesInitialized() {
        return variablesInitialized;
    }

    private static String createErrorMessage(String paramName, String example) {
        return "Required config variable: <" + paramName + "> hadn't been specified, "
                + "You can do it by adding line into the nginx config, example: <jvm_options \"-D" + example + "\";>";
    }
}
