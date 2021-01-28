package kubelocity.servermanager;

import java.util.HashMap;
import java.util.Optional;

public class ServerManager {
    private Optional<String> defaultServer;
    private HashMap<String, String> forcedHosts;

    public ServerManager() {
        this.forcedHosts = new HashMap<>();
        this.defaultServer = Optional.empty();
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
}
