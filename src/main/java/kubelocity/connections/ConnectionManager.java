package kubelocity.connections;

import kubelocity.servermanager.ServerManager;
import net.kyori.adventure.text.Component;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent.DisconnectPlayer;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;

public class ConnectionManager {
    private ServerManager serverManager;
    private Logger logger;
    private ProxyServer proxyServer;

    public ConnectionManager(ServerManager serverManager, ProxyServer proxyServer, Logger logger) {
        this.serverManager = serverManager;
        this.proxyServer = proxyServer;
        this.logger = logger;
    }

    /*
    Sets the current server info based on their forced host
    */
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

    /*
    Ensures the player is fully kicked from the proxy if their first server kicked during initial connection
    */
    @Subscribe
    public void onKickedFromServerEvent(KickedFromServerEvent event) {
        Optional<ServerConnection> serverConnection = event.getPlayer().getCurrentServer();
        if (serverConnection.isEmpty() && !event.kickedDuringServerConnect()) {
            Optional<Component> optionalComponent = event.getServerKickReason();
            Component text = (optionalComponent.isPresent()) ? optionalComponent.get() : Component.text("Unknown");
            DisconnectPlayer dc = KickedFromServerEvent.DisconnectPlayer.create(text);
            event.setResult(dc);
        }
    }
    
    /*
    Ensures the player connects to the correct server based on forced hosts
    */
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