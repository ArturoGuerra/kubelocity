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
    private final String proxyDNS;
    private final Boolean defaultServer;
    private final InetSocketAddress address;
    private final Optional<String> externalHost;

    private final String baseAnnotation;
    private final String defaultServerAnnotation;
    private final String hostAnnotation;
    private final String enabledAnnotation;
    private final String annnotationFormat;
    
    // Java is dogshit
    private final Boolean fake = false;


    public ServerOptions(V1Service service) {
        baseAnnotation = "io.ar2ro.kubelocity";
        annnotationFormat = "%s/%s";
        defaultServerAnnotation = String.format(annnotationFormat, baseAnnotation, "defaultServer");
        hostAnnotation = String.format(annnotationFormat, baseAnnotation, "host");
        enabledAnnotation = String.format(annnotationFormat, baseAnnotation, "enabled");

        name = service.getMetadata().getName();
        proxyDNS = String.format("%s.%s", name, service.getMetadata().getNamespace());
        Integer port = 25565;

        for (V1ServicePort servicePort : service.getSpec().getPorts()) {
            if (servicePort.getName().equals("minecraft")) {
                port = servicePort.getPort();
            }
        }

        address = new InetSocketAddress(proxyDNS, port);

        Map<String, String> annotations = service.getMetadata().getAnnotations();
        if (annotations == null) {
            enabled = false;
            defaultServer = false;
            externalHost = Optional.empty();
        } else {
            enabled = (annotations.get(enabledAnnotation) == null) ? fake : annotations.get(enabledAnnotation).contentEquals("true");
            defaultServer = (annotations.get(defaultServerAnnotation) == null) ? fake : annotations.get(defaultServerAnnotation).contentEquals("true");
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
        return address;
    }

    public Boolean getDefaultServer() {
        return defaultServer;
    }

    public Optional<String> getExternalHost() {
        return externalHost;
    }

    public ServerInfo getServerInfo() {
        return new ServerInfo(name, address);
    }
}