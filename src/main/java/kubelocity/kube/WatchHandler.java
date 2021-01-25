package kubelocity.kube;

import java.io.IOException;
import java.util.logging.Logger;

import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.util.Watch;

public class WatchHandler implements Runnable {
    private KubernetesListener listener;
    private Watch<V1Service> watch;
    private Logger logger;
    public WatchHandler(KubernetesListener listener, Watch<V1Service> watch, Logger logger) {
        this.listener = listener;
        this.watch = watch;
        this.logger = logger;
    }

    @Override
    public void run() {
        try {
            for (Watch.Response<V1Service> service : watch) {
                ServerOptions options = new ServerOptions(service.object);
                switch(service.type) {
                    case "ADDED":
                    case "MODIFIED":
                        listener.addServer(service.type, options);
                        break;
                    case "DELETED":
                        listener.removeServer(options);
                        break;
                    default:
                        logger.info(String.format("Action: %s%n", service.type));
                        break;
                }
            }
        } finally {
            try {
                watch.close();
            } catch (IOException e) {
                logger.info(e.getMessage());
            }
        }        

    }
}