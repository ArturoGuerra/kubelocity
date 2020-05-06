package kubelocity;

import java.io.IOException;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;

import io.kubernetes.client.ApiException;
import kubelocity.kube.KubernetesListener;
import kubelocity.config.Config;
import kubelocity.connections.ConnectionManager;

@Plugin(id="kubelocity", name="kubelocity",
 version="1.0.0", description = "Kubernetes service auto discovery",
 authors= {"Ar2ro_"})
public class Kubelocity {
    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Config config;

    @Inject
    public Kubelocity(ProxyServer proxyServer, Logger logger) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.config = new Config();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        connectionManager();
        kubernetesListener();
    }


    private void connectionManager() {
        proxyServer.getEventManager().register(this, new ConnectionManager(this.config, this.proxyServer, this.logger));
    }

    private void kubernetesListener()  {
        try {
           new KubernetesListener(this.config, this.proxyServer, this.logger);
        } catch (IOException | ApiException e) {
            logger.info(String.format("Error while loading KubernetesListener: %s", e.getMessage()));
        }
    }
}
