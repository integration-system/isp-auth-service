package ru.isp.nginx.entity;

public class Address {
    private String ip;
    private String port;
    private String address;

    public Address() {
    }

    public Address(String ip, String port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getAddress() {
        if (address == null) address = ip + ":" + port;
        return address;
    }
}
