package kubelocity.kube;

import java.util.logging.Logger;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.reflect.TypeToken;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServicePort;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Watch;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;



public class KubernetesListener {
    private final String namespace;
    private final String annotationBase = "io.ar2ro.kubelocity";
    private final String defaultServerAnnotation = annotationBase + "/" + "defaultServer";
    private final String hostAnnotation = annotationBase + "/" + "host";

    private ProxyServer proxyServer;
    private kubelocity.config.Config config;
    private Logger logger;
    private CoreV1Api api;
    private ApiClient kclient;
    private Watch<V1Service> watch;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public KubernetesListener(kubelocity.config.Config config, ProxyServer proxyServer, Logger logger) throws IOException, ApiException {
        this.proxyServer = proxyServer;
        this.config = config;
        this.logger = logger;
        this.namespace = config.getNamespace();
 
        this.kclient = (System.getenv("KUBEFILE") != null) ? Config.fromConfig(System.getenv("KUBEFILE")) : Config.fromCluster();
        Configuration.setDefaultApiClient(this.kclient);
        this.api = new CoreV1Api();
        this.watch = Watch.createWatch(
            this.kclient,
            this.api.listNamespacedServiceCall(this.namespace, "true", false, null, null, null, null, null, 10, true, null),
            new TypeToken<Watch.Response<V1Service>>() {
                    private static final long serialVersionUID = 1L;
                }.getType()
        );
        executorService.execute(new WatchHandler(this, watch, logger));

    }


    private void safelyAddServer(ServerInfo server) {
        Optional<RegisteredServer> registeredServer = this.proxyServer.getServer(server.getName());
        if (registeredServer.isPresent()) this.proxyServer.unregisterServer(registeredServer.get().getServerInfo());
        this.proxyServer.registerServer(server);
    }

    public void addServer(String event, V1Service service) {
        Map<String, String> annotations = service.getMetadata().getAnnotations();
        if (annotations != null) {
            Integer port = 25565;
            final String externalHost = annotations.get("io.ar2ro.kubelocity/host");
            final String name = service.getMetadata().getName();
            final String serviceNamespace = service.getMetadata().getNamespace();
            final String proxyDNS = String.format("%s.%s", name, serviceNamespace);
            final Boolean defaultServer = annotations.get(defaultServerAnnotation).contentEquals("true");
            
            for (V1ServicePort servicePort : service.getSpec().getPorts()) {
                if (servicePort.getName().equals("minecraft")) {
                    port = servicePort.getPort();
                }
            }

            final InetSocketAddress address = new InetSocketAddress(proxyDNS, port);
            final ServerInfo server = new ServerInfo(name, address);
            this.safelyAddServer(server);

            if (defaultServer.booleanValue()) {
                config.setDefaultServer(name);
            }

            if (externalHost != null) {
               config.addForcedHost(externalHost, name);
            }

            logger.info(
                String.format("Event: %s Service: %s ExternalHost: %s Default: %b ProxyDNS: %s",
                event,
                name,
                externalHost,
                defaultServer,
                address.getHostString()));

        }
    }

    public void removeServer(V1Service service) {
        final String name = service.getMetadata().getName();
        final Map<String, String> annotations = service.getMetadata().getAnnotations();
        String externalHost = "";
        Boolean defaultServer = false;

        if (annotations != null) {
            defaultServer = annotations.get(defaultServerAnnotation).contentEquals("true");
            externalHost = annotations.get(hostAnnotation);
            if (externalHost != null) config.removeForcedHost(externalHost);
        }

        Optional<RegisteredServer> registeredServer = this.proxyServer.getServer(name);
        if (registeredServer.isPresent()) {
            this.proxyServer.unregisterServer(registeredServer.get().getServerInfo());
        }

        logger.info(String.format(
            "Event: DELETED Service: %s ExternalHost: %s Default: %b",
            name,
            externalHost != null ? externalHost : "",
            defaultServer
        ));
    }
}