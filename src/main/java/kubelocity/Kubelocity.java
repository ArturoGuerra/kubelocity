package kubelocity;

import java.io.IOException;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import kubelocity.kube.KubernetesListener;
import kubelocity.servermanager.ServerManager;
import kubelocity.config.Config;
import kubelocity.connections.ConnectionManager;

@Plugin(id="kubelocity", name="kubelocity",
 version="1.2.0", description = "Kubernetes service auto discovery",
 authors= {"Arturo Guerra"})
public class Kubelocity {
    private final ProxyServer proxyServer;
    private final ServerManager serverManager;
    private final Logger logger;
    private final Config config;

    @Inject
    public Kubelocity(ProxyServer proxyServer, Logger logger) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.config = new Config();
        this.serverManager = new ServerManager();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.logger.info("Removing Pre-Registered servers");
        for (RegisteredServer s : this.proxyServer.getAllServers()) this.proxyServer.unregisterServer(s.getServerInfo());
        this.logger.info("Registering Connection Manager");
        proxyServer.getEventManager().register(this, new ConnectionManager(this.serverManager, this.proxyServer, this.logger));
        kubernetesListener();
    }



    private void kubernetesListener()  {
        try {
            logger.info("Starting Kubernetes Watcher..");
            proxyServer.getScheduler().buildTask(this, new KubernetesListener(this.config, this.serverManager, this.proxyServer, this.logger)).schedule();
        } catch (IOException e) {
            logger.info(String.format("Error while loading KubernetesListener: %s", e.getMessage()));
        }
    }
}
