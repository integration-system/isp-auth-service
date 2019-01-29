package ru.isp.nginx.entity;

import java.util.List;

public class Requirements {
    private List<String> requiredModules;
    private boolean requireRoutes;

    public Requirements() {
    }

    public Requirements(List<String> requiredModules, boolean requireRoutes) {
        this.requiredModules = requiredModules;
        this.requireRoutes = requireRoutes;
    }

    public List<String> getRequiredModules() {
        return requiredModules;
    }

    public void setRequiredModules(List<String> requiredModules) {
        this.requiredModules = requiredModules;
    }

    public boolean isRequireRoutes() {
        return requireRoutes;
    }

    public void setRequireRoutes(boolean requireRoutes) {
        this.requireRoutes = requireRoutes;
    }
}
