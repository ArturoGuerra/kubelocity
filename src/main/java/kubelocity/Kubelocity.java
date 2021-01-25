package kubelocity;

import java.io.IOException;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import io.kubernetes.client.openapi.ApiException;
import kubelocity.kube.KubernetesListener;
import kubelocity.config.Config;
import kubelocity.connections.ConnectionManager;

@Plugin(id="kubelocity", name="kubelocity",
 version="1.1.0", description = "Kubernetes service auto discovery",
 authors= {"Ar2ro_"})
public class Kubelocity {
    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Config config;
    private KubernetesListener kubeListener;

    @Inject
    public Kubelocity(ProxyServer proxyServer, Logger logger) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.config = new Config();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        removePreRegistered();
        connectionManager();
        kubernetesListener();
        startWatcher();
    }

    private void removePreRegistered() {
        this.logger.info("Removing Pre-Registered servers");
        for (RegisteredServer s : this.proxyServer.getAllServers()) {
            this.proxyServer.unregisterServer(s.getServerInfo());
        }
    }

    private void connectionManager() {
        proxyServer.getEventManager().register(this, new ConnectionManager(this.config, this.proxyServer, this.logger));
    }

    private void kubernetesListener()  {
        try {
           this.kubeListener = new KubernetesListener(this.config, this.proxyServer, this.logger);
        } catch (IOException | ApiException e) {
            logger.info(String.format("Error while loading KubernetesListener: %s", e.getMessage()));
        }
    }

    private void startWatcher() {
        this.kubeListener.startWatcher();
    }
}
