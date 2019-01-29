package ru.isp.nginx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import ru.isp.nginx.entity.RedisResponse;

import java.util.Map;

public class RedisService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisService.class);

    private static final int APPLICATION_TOKEN_DB = 0;
    private static final int APPLICATION_PERMISSION_DB = 1;
    private static final int USER_TOKEN_DB = 2;
    private static final int USER_PERMISSION_DB = 3;
    private static final int DEVICE_TOKEN_DB = 4;
    private static final int DEVICE_PERMISSION_DB = 5;

    public static final String SYSTEM_IDENTITY_FIELD_IN_DB = "1";
    public static final String DOMAIN_IDENTITY_FIELD_IN_DB = "2";
    public static final String SERVICE_IDENTITY_FIELD_IN_DB = "3";
    public static final String APPLICATION_IDENTITY_FIELD_IN_DB = "4";

    private static JedisPool jedisPool;

    private static boolean redisClientInitialized;

    public static void init(String host, Integer port) {
        if (jedisPool != null && !jedisPool.isClosed()) jedisPool.close();

        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        //jedisPoolConfig.setMaxTotal(Integer.parseInt(4));
        jedisPoolConfig.setMaxIdle(15);
        jedisPoolConfig.setMinIdle(1);
        jedisPoolConfig.setMaxWaitMillis(10000);
        //jedisPoolConfig.setTestOnBorrow(true);
        jedisPool = new JedisPool(jedisPoolConfig, host, port, 2000);
    }

    public static Map<String, String> getApplicationIdentities(String key) {
        Jedis resource;
        if ((resource = getConnection()) == null) return null;
        resource.select(APPLICATION_TOKEN_DB);
        try {
            return resource.hgetAll(key);
        } catch (Exception e) {
            LOGGER.warn("jedis error returned data, expected Map<String, String>, key: " + key
                    + ", db: " + APPLICATION_TOKEN_DB, e);
        } finally {
            try {
                resource.close();
            } catch (Exception e) {
                LOGGER.warn("jedis return Resource Exception**********", e);
            }
        }
        return null;
    }

    public static RedisResponse getPermissions(String appIdentityKey,
                                               String uri,
                                               String userIdentityKey,
                                               String deviceIdentityKey) {

        Jedis resource;
        if ((resource = getConnection()) == null) return null;
        try {
            Pipeline pipelined = resource.pipelined();

            pipelined.select(APPLICATION_PERMISSION_DB);
            Response<String> applicationPermissionDb = pipelined.get(appIdentityKey);

            pipelined.select(USER_TOKEN_DB);
            Response<String> userTokenDb = pipelined.get(userIdentityKey);

            pipelined.select(USER_PERMISSION_DB);
            Response<String> userPermissionDb = pipelined.get(uri);

            pipelined.select(DEVICE_TOKEN_DB);
            Response<String> deviceTokenDb = pipelined.get(deviceIdentityKey);

            pipelined.select(DEVICE_PERMISSION_DB);
            Response<String> devicePermissionDb = pipelined.get(uri);

            pipelined.sync();

            return new RedisResponse(
                    applicationPermissionDb,
                    userTokenDb,
                    userPermissionDb,
                    deviceTokenDb,
                    devicePermissionDb
            );

        } catch (Exception e) {
            LOGGER.warn(
                    "jedis error returned data, appIdentityKey: {}, uri: {}, userIdentityKey: {}, deviceIdentityKey: {}",
                    appIdentityKey,
                    uri,
                    userIdentityKey,
                    deviceIdentityKey,
                    e
            );
        } finally {
            try {
                resource.close();
            } catch (Exception e) {
                LOGGER.warn("jedis return Resource Exception**********", e);
            }
        }
        return null;
    }

    private static Jedis getConnection() {
        if (!redisClientInitialized) return null;
        Jedis resource = null;
        try {
            resource = jedisPool.getResource();
            LOGGER.debug("Server is running: " + resource.ping());
            if (resource.isConnected()) {
                LOGGER.debug("connected");
            }
            return resource;
        } catch (JedisConnectionException jex) {
            LOGGER.warn("Got jedis connection exception ", jex);
            try {
                if (resource != null) resource.close();
            } catch (Exception e) {
                LOGGER.warn("jedis return broken Resource Exception**********", e);
            }
        }
        return null;
    }

    public static boolean isRedisClientInitialized() {
        return redisClientInitialized;
    }

    public static void setRedisClientInitialized(boolean redisClientInitialized) {
        RedisService.redisClientInitialized = redisClientInitialized;
    }
}
