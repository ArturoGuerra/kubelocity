package kubelocity.kube;

import com.velocitypowered.api.proxy.server.ServerInfo;

import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServicePort;

import java.util.Optional;
import java.net.InetSocketAddress;
import java.util.Map;

public class ServerOptions {
    private final String name;
    private final Boolean enabled;
    private final InetSocketAddress proxyDNS;
    private final Boolean defaultServer;
    private final Boolean privateServer;
    private final Optional<String> externalHost;

    private final String baseAnnotation;
    private final String defaultServerAnnotation;
    private final String privateServerAnnotation;
    private final String hostAnnotation;
    private final String enabledAnnotation;
    private final String annnotationFormat;
    
    // Java is dogshit
    private final Boolean fake = false;


    public ServerOptions(V1Service service) {
        baseAnnotation = "io.ar2ro.kubelocity";
        annnotationFormat = "%s/%s";
        defaultServerAnnotation = String.format(annnotationFormat, baseAnnotation, "defaultServer");
        privateServerAnnotation = String.format(annnotationFormat, baseAnnotation, "private");
        enabledAnnotation = String.format(annnotationFormat, baseAnnotation, "enabled");
        hostAnnotation = String.format(annnotationFormat, baseAnnotation, "host");

        name = service.getMetadata().getName();
        String internalDNS = String.format("%s.%s", name, service.getMetadata().getNamespace());
        Integer port = 25565;

        for (V1ServicePort servicePort : service.getSpec().getPorts()) {
            if (servicePort.getName().equals("minecraft")) {
                port = servicePort.getPort();
            }
        }

        proxyDNS = new InetSocketAddress(internalDNS, port);

        Map<String, String> annotations = service.getMetadata().getAnnotations();
        if (annotations == null) {
            enabled = false;
            defaultServer = false;
            privateServer = false;
            externalHost = Optional.empty();
        } else {
            enabled = (annotations.get(enabledAnnotation) == null) ? fake : annotations.get(enabledAnnotation).contentEquals("true");
            defaultServer = (annotations.get(defaultServerAnnotation) == null) ? fake : annotations.get(defaultServerAnnotation).contentEquals("true");
            privateServer = (annotations.get(privateServerAnnotation) == null) ? fake : annotations.get(privateServerAnnotation).contentEquals("true");
            externalHost= (annotations.get(hostAnnotation) == null) ? Optional.empty() : Optional.of(annotations.get(hostAnnotation));
        }
    }

    public String getName() {
        return name;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public InetSocketAddress getProxyDNS() {
        return proxyDNS;
    }

    public Boolean getDefaultServer() {
        return defaultServer;
    }

    public Optional<String> getExternalHost() {
        return externalHost;
    }

    public ServerInfo getServerInfo() {
        return new ServerInfo(name, proxyDNS);
    }

    public Boolean isPrivateServer() {
        return externalHost.isPresent() && privateServer;
    }
}