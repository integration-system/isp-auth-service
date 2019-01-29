package ru.isp.nginx;

import nginx.clojure.java.NginxJavaRingHandler;
import nginx.clojure.util.NginxSharedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.isp.nginx.utils.AppConfig;
import ru.isp.nginx.utils.SocketIOClient;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static ru.isp.nginx.utils.Constant.BALANCING_SHARED_MAP;
import static ru.isp.nginx.utils.Constant.BALANCING_SHARED_MDM_MAP;

public class JvmInitHandler implements NginxJavaRingHandler {
    public static final String VERSION = "0.3.0";

    public static final NginxSharedHashMap<String, Integer> BALANCING_MAP = NginxSharedHashMap.build(BALANCING_SHARED_MAP);

    private static final Logger LOGGER = LoggerFactory.getLogger(JvmInitHandler.class);

    private static final RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
    private static final List<String> arguments = runtimeMxBean.getInputArguments();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static boolean monitorInitialized = false;

    @Override
    public Object[] invoke(Map<String, Object> fakeReq) {
        BALANCING_MAP.put(BALANCING_SHARED_MAP, 0);
        BALANCING_MAP.put(BALANCING_SHARED_MDM_MAP, 0);
        AppConfig.initParameters(initArguments(arguments));
        if (AppConfig.isVariablesInitialized()) {
            SocketIOClient.initClient(AppConfig.CONFIG_SERVICE_IP, AppConfig.INSTANCE_UUID, AppConfig.MODULE_NAME);
            if (!monitorInitialized) {
                scheduler.scheduleAtFixedRate(() -> {
                    if (!SocketIOClient.isConnected()) {
                        LOGGER.error("Config service is unreachable, ip: {}, system_uuid: {}, module_name: {}",
                                AppConfig.CONFIG_SERVICE_IP, AppConfig.INSTANCE_UUID, AppConfig.MODULE_NAME);
                    }
                }, 10, 10, TimeUnit.MINUTES);
                monitorInitialized = true;
            }
        }
        return new Object[0];
    }

    private Map<String, String> initArguments(List<String> arguments) {
        Map<String, String> result = new HashMap<>();
        for (String arg : arguments) {
            if (arg.startsWith("-D") && arg.contains("=")) {
                String[] strings = arg.substring(2).split("=");
                if (strings.length == 2) {
                    result.put(strings[0], strings[1]);
                }
            }
        }
        return result;
    }
}
