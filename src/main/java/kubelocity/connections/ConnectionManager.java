package kubelocity.connections;

import kubelocity.config.Config;

import java.net.InetSocketAddress;
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
        Optional<RegisteredServer> registeredServer = event.getInitialServer();
        if (registeredServer.isPresent()) {
            logger.info(registeredServer.toString());
        }

        Optional<InetSocketAddress> virtualHost = event.getPlayer().getVirtualHost();
        final String hostName = (virtualHost.isPresent()) ? virtualHost.get().getHostName() : null;
        final String forcedHost = (hostName != null) ? this.config.getForcedHost(hostName) : null;

        if (hostName != null && forcedHost != null) {
            logger.info(String.format("Forced Host: %s Server: %s", hostName, forcedHost));
            registeredServer = this.proxyServer.getServer(forcedHost);
        }

        if (registeredServer.isEmpty()) {
            this.logger.info("Getting Default Server");
            registeredServer = this.config.getDefaultServer() != null ? this.proxyServer.getServer(this.config.getDefaultServer()) : Optional.empty();
        }

        if (registeredServer.isPresent()) {
            event.setInitialServer(registeredServer.get());
        }
    }
}