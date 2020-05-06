package kubelocity.connections;

import kubelocity.config.Config;

import java.util.Optional;
import java.util.logging.Logger;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
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
    public void onPlayerChooseInitialServerEvent(PlayerChooseInitialServerEvent event) {
        if (event.getInitialServer().isPresent()) {
            this.logger.info(event.getInitialServer().get().toString());
            return;
        }

        final String hostName = event.getPlayer().getVirtualHost().get().getHostName();
        final String forcedHost = (hostName != null) ? this.config.getForcedHost(hostName) : null;
        Optional<RegisteredServer> registeredServer = null;

        if (hostName != null && forcedHost != null) {
            this.logger.info(String.format("Forced Host: %s Server: %s", hostName, forcedHost));
            registeredServer = this.proxyServer.getServer(forcedHost);
        }

        if (registeredServer == null) {
            this.logger.info("Getting Default Server");
            registeredServer = this.config.getDefaultServer() != null ? this.proxyServer.getServer(this.config.getDefaultServer()) : null;
        }

        if (registeredServer.isPresent()) {
            event.setInitialServer(registeredServer.get());
        }
    }
}