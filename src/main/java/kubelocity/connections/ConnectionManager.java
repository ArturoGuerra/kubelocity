package kubelocity.connections;

import kubelocity.servermanager.ServerManager;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent.ServerResult;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;

public class ConnectionManager {
    private ServerManager serverManager;
    private Logger logger;
    private ProxyServer proxyServer;
    private final Boolean fake = false;

    public ConnectionManager(ServerManager serverManager, ProxyServer proxyServer, Logger logger) {
        this.serverManager = serverManager;
        this.proxyServer = proxyServer;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyPingEvent(ProxyPingEvent event) {
        Optional<InetSocketAddress> con =  event.getConnection().getVirtualHost();
        if (con.isPresent()) {
            Optional<String> forcedHost = serverManager.getForcedHost(con.get().getHostName());
            Optional<String> serverName = (forcedHost.isPresent()) ? Optional.of(forcedHost.get()) : serverManager.getDefaultServer();
            if (serverName.isPresent()) {
                Optional<RegisteredServer> registeredServer = proxyServer.getServer(serverName.get());
                if (registeredServer.isPresent()) {
                    try {
                        CompletableFuture<ServerPing> futurePing = registeredServer.get().ping();
                        ServerPing ping = futurePing.get();
                        event.setPing(ping);
                    } catch(ExecutionException | InterruptedException e) {
                        logger.info(e.getMessage());
                    }
               
                }
            }
        }
    }

    @Subscribe
    public void onServerPreConnectEvent(ServerPreConnectEvent event) {
        RegisteredServer registeredServer = event.getOriginalServer();
        String serverName = registeredServer.getServerInfo().getName();

        if (this.serverManager.isPrivateHost(serverName).booleanValue()) {
            logger.info(String.format("Checking if %s is allowed in %s", event.getPlayer().getUsername(), serverName));
            Optional<InetSocketAddress> virtualHost = event.getPlayer().getVirtualHost();
            if (virtualHost.isPresent()) {
                String hostName = virtualHost.get().getHostName();
                Optional<String> forcedHost = this.serverManager.getForcedHost(hostName);
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
            Optional<String> forcedHost = this.serverManager.getForcedHost(hostName);
            if (forcedHost.isPresent()) {
                logger.info(String.format("Forced Host: %s Server: %s", hostName, forcedHost.get()));
                registeredServer = this.proxyServer.getServer(forcedHost.get());
            }
        }

        if (registeredServer.isEmpty() ) {
            this.logger.info("Getting Default Server");
            Optional<String> defaultServer = this.serverManager.getDefaultServer();
            registeredServer = (defaultServer.isPresent()) ? this.proxyServer.getServer(defaultServer.get()) : Optional.empty();
        }

        if (registeredServer.isPresent()) {
            event.setInitialServer(registeredServer.get());
        }
    }
}