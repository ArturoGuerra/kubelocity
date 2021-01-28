package kubelocity.kube;

import java.util.logging.Logger;

import java.io.IOException;

import com.google.common.reflect.TypeToken;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Watch;

import java.util.Optional;

import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

import kubelocity.servermanager.ServerManager;

public class KubernetesListener implements Runnable {
    private final String namespace;
    private final ProxyServer proxyServer;
    private final Logger logger;
    private final CoreV1Api api;
    private final ApiClient kclient;
    private final ServerManager serverManager;

    public KubernetesListener(kubelocity.config.Config config, ServerManager serverManager, ProxyServer proxyServer, Logger logger) throws IOException {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.serverManager = serverManager;
        this.namespace = config.getNamespace();
 
        this.kclient = (System.getenv("KUBEFILE") != null) ? Config.fromConfig(System.getenv("KUBEFILE")) : Config.fromCluster();
        OkHttpClient httpClient = this.kclient.getHttpClient().newBuilder().readTimeout(0, TimeUnit.SECONDS).build();
        this.kclient.setHttpClient(httpClient);
        this.kclient.setVerifyingSsl(false);
        Configuration.setDefaultApiClient(this.kclient);
        this.api = new CoreV1Api();
    }

    public void run() {
        while (true) {
            logger.info("Watching services..");
            try {
                Watch<V1Service> watch = Watch.createWatch(
                    this.kclient,
                    this.api.listNamespacedServiceCall(this.namespace, "true", false, null, null, null, null, null, null, true, null),
                    new TypeToken<Watch.Response<V1Service>>() {
                        private static final long serialVersionUID = 1L;
                    }.getType()
                );
                try {
                    for (Watch.Response<V1Service> service : watch) {
                        ServerOptions options = new ServerOptions(service.object);
                        switch(service.type) {
                            case "ADDED":
                            case "MODIFIED":
                                addServer(service.type, options);
                                break;
                            case "DELETED":
                                removeServer(options);
                                break;
                            default:
                                logger.info(String.format("Action: %s%n", service.type));
                                break;
                        }
                    }

                } finally {
                    watch.close();
                }
            } catch (ApiException | IOException e) {
                logger.info(e.getMessage());
            }
        }  
        
    }


    private void safelyAddServer(ServerInfo server) {
        Optional<RegisteredServer> registeredServer = this.proxyServer.getServer(server.getName());
        if (registeredServer.isPresent()) this.proxyServer.unregisterServer(registeredServer.get().getServerInfo());
        this.proxyServer.registerServer(server);
    }

    public void addServer(String event, ServerOptions options) {
        if (options.isEnabled().booleanValue()) {
            this.safelyAddServer(options.getServerInfo());
            if (options.getDefaultServer().booleanValue()) {
                serverManager.setDefaultServer(options.getName());
            }

            Optional<String> externalHost = options.getExternalHost();
            if (externalHost.isPresent()) {
                serverManager.addForcedHost(externalHost.get(), options.getName());
            }
            
            logger.info(
                String.format("Event: %s Service: %s ExternalHost: %s Default: %b ProxyDNS: %s",
                event,
                options.getName(),
                externalHost,
                options.getDefaultServer(),
                options.getProxyDNS().getHostString()));
        }
    }

    public void removeServer(ServerOptions options) {
        Optional<String> externalHost = options.getExternalHost();
        if (externalHost.isPresent()) {
            serverManager.removeForcedHost(externalHost.get());
        }

        Optional<RegisteredServer> registeredServer = this.proxyServer.getServer(options.getName());
        if (registeredServer.isPresent()) {
            this.proxyServer.unregisterServer(registeredServer.get().getServerInfo());
        }

        String extHost = (externalHost.isPresent()) ? externalHost.get() : "";

        logger.info(String.format(
            "Event: DELETED Service: %s ExternalHost: %s Default: %b",
            options.getName(),
            extHost,
            options.getDefaultServer()
        ));
    }
}