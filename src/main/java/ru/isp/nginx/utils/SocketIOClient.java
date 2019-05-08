package ru.isp.nginx.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.isp.nginx.entity.Address;
import ru.isp.nginx.entity.Endpoint;
import ru.isp.nginx.entity.BackendDeclaration;
import ru.isp.nginx.entity.Requirements;
import ru.isp.nginx.service.RedisService;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import static ru.isp.nginx.JvmInitHandler.VERSION;
import static ru.isp.nginx.utils.AppConfig.*;

public class SocketIOClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SocketIOClient.class);

    private static Socket socket;
    private static final String SEND_REQUIREMENTS_AFTER_CONNECT = "MODULE:SEND_REQUIREMENTS";

    private static final String CONFIG_AFTER_CONNECTION = "CONFIG:SEND_CONFIG_WHEN_CONNECTED";
    private static final String CONFIG_CHANGED = "CONFIG:SEND_CONFIG_CHANGED";

    private static final String SEND_MODULE_IS_READY = "MODULE:READY";

    private static final String SEND_ROUTES_AFTER_CONNECT = "CONFIG:SEND_ROUTES_WHEN_CONNECTED";
    private static final String SEND_ROUTES_CHANGES = "CONFIG:SEND_ROUTES_CHANGED";

    private static final String ERROR_CONNECTION = "ERROR_CONNECTION";
    private static ObjectMapper objectMapper = new ObjectMapper();

    private static boolean moduleIsReady = false;

    private static BackendDeclaration backendDeclaration;
    private static Requirements requirements;

    static {
        List<String> requiredModules = new ArrayList<>();
        List<Endpoint> endpoints = new ArrayList<>();
        if (Strings.isNotBlank(MDM_API_EVENT_NAME)) {
            requiredModules.add(MDM_API_EVENT_NAME);
            endpoints.add(new Endpoint("mdm/api"));
        }
        if (Strings.isNotBlank(ISP_CONVERTER_EVENT_NAME)) {
            requiredModules.add(ISP_CONVERTER_EVENT_NAME);
            endpoints.add(new Endpoint("/api"));
        }
        if (Strings.isNotBlank(ISP_FILE_STORAGE_EVENT_NAME)) {
            requiredModules.add(ISP_FILE_STORAGE_EVENT_NAME);
            endpoints.add(new Endpoint("/files"));
        }
        requirements = new Requirements(requiredModules, true);
        backendDeclaration = new BackendDeclaration(
                MODULE_NAME,
                VERSION,
                new Address(CONFIG_SERVICE_HOST, CONFIG_SERVICE_PORT),
                endpoints
        );
    }

    private static final Emitter.Listener onReceivedConfig = args -> {
        LOGGER.info("CONFIG RECEIVED: {}", Arrays.asList(args));
        if (args.length == 0) {
            LOGGER.error("onReceivedConfig: empty payload");
            return;
        }
        try {
            TypeReference<Map<String, Object>> valueTypeRef = new TypeReference<Map<String, Object>>() {
            };
            Map<String, Object> config = objectMapper.readValue(String.valueOf(args[0]), valueTypeRef);
            RemoteConfig.initParameters(config);
            if (RemoteConfig.isConfigReceivedAndValid()) {
                RedisService.init(RemoteConfig.REDIS_IP, RemoteConfig.REDIS_PORT);
                RedisService.setRedisClientInitialized(true);
            } else {
                LOGGER.error("Config isn't valid", config);
            }
        } catch (IOException e) {
            LOGGER.error("Error occurred when config was parsing", e);
        }

        if (args[args.length - 1] instanceof Ack) {
            LOGGER.info("CONFIG RECEIVED ACK");
            Ack ack = (Ack) args[args.length - 1];
            ack.call();
        }
        try {
            String requirementsAsString = objectMapper.writeValueAsString(requirements);
            LOGGER.info("Send requirements: {}", requirementsAsString);
            socket.emit(SEND_REQUIREMENTS_AFTER_CONNECT, requirementsAsString);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error occurred when send module requirements to config service", e);
        }
    };

    private static final Emitter.Listener onReceivedConvertAddresses = args -> {
        LOGGER.info("CONVERT ADDRESSES RECEIVED: {}", Arrays.asList(args));
        if (args.length == 0) {
            LOGGER.error("onReceivedConvertAddresses: empty payload");
            return;
        }

        AppConfig.PROXY_ADDRESS = parseAddresses(String.valueOf(args[0]), "CONVERTER")
                .stream().map(Address::getAddress).collect(Collectors.toList());
        if (!AppConfig.PROXY_ADDRESS.isEmpty() && !moduleIsReady) {
            try {
                String moduleInfoAsString = objectMapper.writeValueAsString(backendDeclaration);
                LOGGER.info("Send module info: {}", moduleInfoAsString);
                socket.emit(SEND_MODULE_IS_READY, moduleInfoAsString);
                moduleIsReady = true;
            } catch (JsonProcessingException e) {
                LOGGER.error("Error occurred when send module info to config service", e);
            }
        }

        if (args[args.length - 1] instanceof Ack) {
            Ack ack = (Ack) args[args.length - 1];
            ack.call();
        }
    };

    private static final Emitter.Listener onReceiveMdmApiAddresses = args -> {
        LOGGER.info("MDM ADAPTER ADDRESSES RECEIVED: {}", Arrays.asList(args));
        if (args.length == 0) {
            LOGGER.error("onReceiveMdmApiAddresses: empty payload");
            return;
        }

        AppConfig.PROXY_MDM_ADDRESS = parseAddresses(String.valueOf(args[0]), "MDM").stream()
                .map(Address::getAddress)
                .map(addr -> {
                    if (addr.startsWith("http://") || addr.startsWith("https://")) {
                        return addr;
                    }
                    return "http://" + addr;
                })
                .collect(Collectors.toList());

        if (args[args.length - 1] instanceof Ack) {
            Ack ack = (Ack) args[args.length - 1];
            ack.call();
        }
    };

    private static final Emitter.Listener onReceiveFSAddresses = args -> {
        LOGGER.info("FILE STORAGE ADDRESSES RECEIVED: {}", Arrays.asList(args));
        if (args.length == 0) {
            LOGGER.error("onReceiveFSAddresses: empty payload");
            return;
        }

        PROXY_FILE_STORAGE_ADDRESS = parseAddresses(String.valueOf(args[0]), "MDM").stream()
                .map(Address::getAddress)
                .map(addr -> {
                    if (addr.startsWith("http://") || addr.startsWith("https://")) {
                        return addr;
                    }
                    return "http://" + addr;
                }).collect(Collectors.toList());
        if (args[args.length - 1] instanceof Ack) {
            Ack ack = (Ack) args[args.length - 1];
            ack.call();
        }
    };

    private static final Emitter.Listener onReceiveBackends = args -> {
        LOGGER.info("NEW ROUTES RECEIVED");
        if (args.length == 0) {
            LOGGER.error("onReceiveBackends: empty payload");
            return;
        }

        TypeReference<List<BackendDeclaration>> type = new TypeReference<List<BackendDeclaration>>() {
        };
        try {
            List<BackendDeclaration> list = objectMapper.readValue(String.valueOf(args[0]), type);
            ENDPOINTS_PATH_MAP = list.stream()
                    .flatMap(declaration -> declaration.getEndpoints().stream())
                    .collect(Collectors.toMap(Endpoint::getPath, e -> e, (e1, e2) -> e1));
        } catch (IOException e) {
            LOGGER.error("Error occurred when routes was parsing", e);
        }

        if (args[args.length - 1] instanceof Ack) {
            Ack ack = (Ack) args[args.length - 1];
            ack.call();
        }
    };

    private static List<Address> parseAddresses(String addressJson, String type) {
        try {
            TypeReference<List<Address>> valueTypeRef = new TypeReference<List<Address>>() {
            };
            List<Address> addresses = objectMapper.readValue(addressJson, valueTypeRef);
            Iterator<Address> iterator = addresses.iterator();
            while (iterator.hasNext()) {
                Address address = iterator.next();
                if (Strings.isBlank(address.getIp()) || Strings.isBlank(address.getPort())) {
                    LOGGER.warn("Converter address is invalid, ip or port is empty, ip: {}, port: {}",
                            address.getIp(), address.getPort());
                    iterator.remove();
                }
            }
            return addresses;
        } catch (IOException e) {
            LOGGER.error("Error occurred when {} addresses was parsing", type, e);
        }
        return new ArrayList<>();
    }

    public static boolean isConnected() {
        return socket.connected();
    }

    public static void initClient(String address, String instanceUUID, String moduleName) {
        try {
            socket = IO.socket(address + "?instance_uuid=" + instanceUUID + "&module_name=" + moduleName);
        } catch (URISyntaxException e) {
            LOGGER.error("Address for connect to a config service is not valid", e);
        }
        socket.on(Socket.EVENT_CONNECT, args -> LOGGER.info("CONFIG SERVER CONNECT, {}", args));
        socket.on(Socket.EVENT_DISCONNECT, args -> LOGGER.info("CONFIG SERVER DISCONNECTED, {}", args));
        socket.on(Socket.EVENT_CONNECTING, args -> LOGGER.info("CONFIG SERVER CONNECTING, {}", args));
        socket.on(Socket.EVENT_MESSAGE, args -> LOGGER.info("CONFIG SERVER MESSAGE, {}", args));
        if (Strings.isNotBlank(MDM_API_EVENT_NAME)) {
            socket.on(MDM_API_EVENT_NAME, onReceiveMdmApiAddresses);
            LOGGER.info("MDM_API_EVENT_NAME is: {}", MDM_API_EVENT_NAME);
        } else {
            LOGGER.info("MDM_API_EVENT_NAME is empty");
        }
        if (Strings.isNotBlank(ISP_CONVERTER_EVENT_NAME)) {
            socket.on(ISP_CONVERTER_EVENT_NAME, onReceivedConvertAddresses);
            LOGGER.info("ISP_CONVERTER_EVENT_NAME is: {}", ISP_CONVERTER_EVENT_NAME);
        } else {
            LOGGER.error("ISP_CONVERTER_EVENT_NAME is empty");
        }
        if (Strings.isNotBlank(ISP_FILE_STORAGE_EVENT_NAME)) {
            socket.on(ISP_FILE_STORAGE_EVENT_NAME, onReceiveFSAddresses);
            LOGGER.info("ISP_FILE_STORAGE_EVENT_NAME is: {}", ISP_FILE_STORAGE_EVENT_NAME);
        } else {
            LOGGER.info("ISP_FILE_STORAGE_EVENT_NAME is empty");
        }
        socket.on(CONFIG_AFTER_CONNECTION, onReceivedConfig);
        socket.on(CONFIG_CHANGED, onReceivedConfig);
        socket.on(SEND_ROUTES_AFTER_CONNECT, onReceiveBackends);
        socket.on(SEND_ROUTES_CHANGES, onReceiveBackends);
        socket.on(ERROR_CONNECTION, args -> LOGGER.warn("ERROR SOCKET CONNECT: {}", Arrays.asList(args)));
        socket = socket.connect();
        LOGGER.info("SOCKET CONNECTED");
    }
}
