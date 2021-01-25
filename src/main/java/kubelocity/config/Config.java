package kubelocity.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

public class Config {
    private HashMap<String, String> forcedHosts;
    private HashSet<String> privateHosts;
    private Optional<String> defaultServer;
    private String namespace;

    // Loads Config from environmental variables
    public Config() {
        this.forcedHosts = new HashMap<>();
        this.privateHosts = new HashSet<>();
        this.namespace = (System.getenv("KUBE_NAMESPACE") != null) ? System.getenv("KUBE_NAMESPACE") : "minecraft";
        this.defaultServer = Optional.empty();
    }

    public String getNamespace() {
        return this.namespace;
    }

    public Optional<String> getDefaultServer() {
        return this.defaultServer;
    }

    public void setDefaultServer(String server) {
        this.defaultServer = Optional.of(server);
    }

    public void addForcedHost(String host, String server) {
        this.forcedHosts.put(host, server);
    }
    
    public Optional<String> getForcedHost(String host) {
        String value = this.forcedHosts.get(host);
        return (value == null) ? Optional.empty() : Optional.of(value);
    }

    public void removeForcedHost(String host) {
        this.forcedHosts.remove(host);
    }

    public void addPrivateHost(String server) {
        this.privateHosts.add(server);
    }

    public void removePrivateHost(String server) {
        this.privateHosts.remove(server);
    }

    public Boolean isPrivateHost(String server) {
        return this.privateHosts.contains(server);
    }

}