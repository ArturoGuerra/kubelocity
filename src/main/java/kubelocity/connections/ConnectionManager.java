package kubelocity.connections;

import kubelocity.config.Config;

import java.util.Optional;
import java.util.logging.Logger;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;


public class ConnectionManager {
    private Config config;
    private Logger logger;
    private ProxyServer proxyServer;

    public ConnectionManager(Config config, ProxyServer proxyServer, Logger logger) {
        this.config = config;
        this.proxyServer = proxyServer;
        this.logger = logger;
    }

    @Subscribe
    public void onServerConnect(ServerPreConnectEvent event) {

        String hostName = event.getPlayer().getVirtualHost().get().getHostName();
        Optional<RegisteredServer> registeredServer = null;

        if (hostName != null) {
            String forcedServer = this.config.getForcedHost(hostName);
            logger.info(String.format("Forced Host: %s Server: %s", hostName, forcedServer));
            registeredServer = (forcedServer != null) ? this.proxyServer.getServer(forcedServer) : null;
        }

        if (registeredServer == null) {
            logger.info("Getting Default Server");
            registeredServer = this.config.getDefaultServer() != null ? this.proxyServer.getServer(this.config.getDefaultServer()) : null;
        }

        if (registeredServer != null) {
            event.setResult(ServerPreConnectEvent.ServerResult.allowed(registeredServer.get()));
        }
    }
}