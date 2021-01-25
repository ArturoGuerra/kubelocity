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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;



public class KubernetesListener {
    private final String namespace;
    private final ProxyServer proxyServer;
    private final kubelocity.config.Config config;
    private final Logger logger;
    private final CoreV1Api api;
    private final ApiClient kclient;
    private final Watch<V1Service> watch;
    private final ExecutorService executorService;

    public KubernetesListener(kubelocity.config.Config config, ProxyServer proxyServer, Logger logger) throws IOException, ApiException {
        this.proxyServer = proxyServer;
        this.config = config;
        this.logger = logger;
        this.namespace = config.getNamespace();
        this.executorService = Executors.newSingleThreadExecutor();
 
        this.kclient = (System.getenv("KUBEFILE") != null) ? Config.fromConfig(System.getenv("KUBEFILE")) : Config.fromCluster();
        OkHttpClient httpClient = this.kclient.getHttpClient().newBuilder().readTimeout(0, TimeUnit.SECONDS).build();
        this.kclient.setHttpClient(httpClient);
        this.kclient.setVerifyingSsl(false);
        Configuration.setDefaultApiClient(this.kclient);
        this.api = new CoreV1Api();
        this.watch = Watch.createWatch(
            this.kclient,
            this.api.listNamespacedServiceCall(this.namespace, "true", false, null, null, null, null, null, null, true, null),
            new TypeToken<Watch.Response<V1Service>>() {
                    private static final long serialVersionUID = 1L;
                }.getType()
        );
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
                config.setDefaultServer(options.getName());
            }

            Optional<String> externalHost = options.getExternalHost();
            if (externalHost.isPresent()) {
                config.addForcedHost(externalHost.get(), options.getName());
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
            config.removeForcedHost(externalHost.get());
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

    public void startWatcher() {
        executorService.execute(new WatchHandler(this, watch, logger));
    }
}