package com.github.sunnysuperman.serverpub.loadbalance;

public class BackendServer {
    private String id;
    private String ip;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String toString() {
        return id + ": " + ip;
    }

}
