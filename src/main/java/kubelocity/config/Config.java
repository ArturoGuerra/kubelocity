package kubelocity.config;

public class Config {
    private final String namespace;

    // Loads Config from environmental variables
    public Config() {
        this.namespace = (System.getenv("KUBE_NAMESPACE") != null) ? System.getenv("KUBE_NAMESPACE") : "minecraft";
    }

    public String getNamespace() {
        return this.namespace;
    }



}