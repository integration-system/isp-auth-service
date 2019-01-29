package ru.isp.nginx.entity;

import redis.clients.jedis.Response;

public class RedisResponse {

    public RedisResponse() {
    }

    public RedisResponse(Response<String> applicationPermission,
                         Response<String> userIdentity,
                         Response<String> userPermission,
                         Response<String> deviceIdentity,
                         Response<String> devicePermission) {

        this.applicationPermission = applicationPermission;
        this.userIdentity = userIdentity;
        this.userPermission = userPermission;
        this.deviceIdentity = deviceIdentity;
        this.devicePermission = devicePermission;
    }

    private Response<String> applicationPermission;
    private Response<String> userIdentity;
    private Response<String> userPermission;
    private Response<String> deviceIdentity;
    private Response<String> devicePermission;

    public Response<String> getApplicationPermission() {
        return applicationPermission;
    }

    public void setApplicationPermission(Response<String> applicationPermission) {
        this.applicationPermission = applicationPermission;
    }

    public Response<String> getUserIdentity() {
        return userIdentity;
    }

    public void setUserIdentity(Response<String> userIdentity) {
        this.userIdentity = userIdentity;
    }

    public Response<String> getUserPermission() {
        return userPermission;
    }

    public void setUserPermission(Response<String> userPermission) {
        this.userPermission = userPermission;
    }

    public Response<String> getDeviceIdentity() {
        return deviceIdentity;
    }

    public void setDeviceIdentity(Response<String> deviceIdentity) {
        this.deviceIdentity = deviceIdentity;
    }

    public Response<String> getDevicePermission() {
        return devicePermission;
    }

    public void setDevicePermission(Response<String> devicePermission) {
        this.devicePermission = devicePermission;
    }
}
