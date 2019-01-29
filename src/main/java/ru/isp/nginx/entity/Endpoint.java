package ru.isp.nginx.entity;

public class Endpoint {
    private String path;
    private boolean inner;
    private boolean ignoreOnRouter = true;

    public Endpoint() {
    }

    public Endpoint(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isInner() {
        return inner;
    }

    public void setInner(boolean inner) {
        this.inner = inner;
    }

    public boolean isIgnoreOnRouter() {
        return ignoreOnRouter;
    }

    public void setIgnoreOnRouter(boolean ignoreOnRouter) {
        this.ignoreOnRouter = ignoreOnRouter;
    }
}
