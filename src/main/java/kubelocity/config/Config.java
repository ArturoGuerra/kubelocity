package kubelocity.config;

import java.util.HashMap;
import java.lang.System;

public class Config {
    private String motd;
    private String[] admins;
    private HashMap<String, String> forcedHosts;
    private String defaultServer;
    private String namespace;
    public  Boolean keepPreRegistered;

    // Loads Config from environmental variables
    public Config() {
        this.forcedHosts = new HashMap<String, String>();
        this.admins = (System.getenv("ADMINS") != null) ? System.getenv("ADMINS").split(",") : new String[]{};
        this.motd = (System.getenv("MOTD") != null) ? System.getenv("MOTD") : "Welcome to northern uwu";
        this.namespace = (System.getenv("KUBE_NAMESPACE") != null) ? System.getenv("KUBE_NAMESPACE") : "minecraft";
        this.defaultServer = null;
        this.admins = new String[]{};

        this.keepPreRegistered = (System.getenv("KEEP_PREREGISTERED") != null) ? System.getenv("KEEP_PREREGISTERED").contentEquals("true") : false;
    }

    public String getMotd() {
        return this.motd;
    }

    public String[] getAdmins() {
        return this.admins;
    }

    public String getDefaultServer() {
        return this.defaultServer;
    }

    public void setDefaultServer(String server) {
        this.defaultServer = server;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public void addForcedHost(String host, String server) {
        this.forcedHosts.put(host, server);
    }

    public void removeForcedHost(String host) {
        this.forcedHosts.remove(host);
    }

    public String getForcedHost(String host) {
        return this.forcedHosts.get(host);
    }
}