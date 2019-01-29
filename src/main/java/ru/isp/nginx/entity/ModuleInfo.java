package ru.isp.nginx.entity;

import java.util.ArrayList;
import java.util.List;

public class ModuleInfo {
    private String moduleName;
    private String version;
    private String libVersion = "";
    private Address address;
    private List<Endpoint> endpoints = new ArrayList<>();

    public ModuleInfo() {
    }

    public ModuleInfo(String moduleName, String version, Address address, List<Endpoint> endpoints) {
        this.moduleName = moduleName;
        this.version = version;
        this.address = address;
        this.endpoints = endpoints;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLibVersion() {
        return libVersion;
    }

    public void setLibVersion(String libVersion) {
        this.libVersion = libVersion;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }
}
