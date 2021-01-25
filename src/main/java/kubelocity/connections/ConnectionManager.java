package kubelocity.connections;

import kubelocity.config.Config;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.logging.Logger;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent.ServerResult;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

public class ConnectionManager {
    private Config config;
    private Logger logger;
    private ProxyServer proxyServer;
    private final Boolean fake = false;

    public ConnectionManager(Config config, ProxyServer proxyServer, Logger logger) {
        this.config = config;
        this.proxyServer = proxyServer;
        this.logger = logger;
    }

    @Subscribe
    public void onServerPreConnectEvent(ServerPreConnectEvent event) {
        RegisteredServer registeredServer = event.getOriginalServer();
        String serverName = registeredServer.getServerInfo().getName();

        if (this.config.isPrivateHost(serverName).booleanValue()) {
            logger.info(String.format("Checking if %s is allowed in %s", event.getPlayer().getUsername(), serverName));
            Optional<InetSocketAddress> virtualHost = event.getPlayer().getVirtualHost();
            if (virtualHost.isPresent()) {
                String hostName = virtualHost.get().getHostName();
                Optional<String> forcedHost = this.config.getForcedHost(hostName);
                Boolean notAllowed = (forcedHost.isPresent()) ? !forcedHost.get().equals(serverName) : fake;
                if (notAllowed.booleanValue()) {
                    event.setResult(ServerResult.denied());
                }
            }
        }
    }

    @Subscribe
    public void onPlayerChooseInitialServerEvent(PlayerChooseInitialServerEvent event) {
        Optional<RegisteredServer> registeredServer = event.getInitialServer();
        Optional<InetSocketAddress> virtualHost = event.getPlayer().getVirtualHost();

        // Forced host checks
        if (virtualHost.isPresent()) {
            String hostName = virtualHost.get().getHostName();
            Optional<String> forcedHost = this.config.getForcedHost(hostName);
            if (forcedHost.isPresent()) {
                logger.info(String.format("Forced Host: %s Server: %s", hostName, forcedHost.get()));
                registeredServer = this.proxyServer.getServer(forcedHost.get());
            }
        }

        if (registeredServer.isEmpty() ) {
            this.logger.info("Getting Default Server");
            Optional<String> defaultServer = this.config.getDefaultServer();
            registeredServer = (defaultServer.isPresent()) ? this.proxyServer.getServer(defaultServer.get()) : Optional.empty();
        }

        if (registeredServer.isPresent()) {
            event.setInitialServer(registeredServer.get());
        }
    }
}