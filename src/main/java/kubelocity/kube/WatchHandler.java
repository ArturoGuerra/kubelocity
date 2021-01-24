package kubelocity.kube;

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
            watch.forEach(service -> {
                switch(service.type) {
                    case "ADDED":
                        listener.addServer(service.type, service.object);
                        break;
                    case "MODIFIED":
                        listener.addServer(service.type, service.object);
                        break;
                    case "DELETED":
                        listener.removeServer(service.object);
                        break;
                    default:
                        logger.info(String.format("Action: %s%n", service.type));
                        break;
                }
            });
        } catch (Throwable e) {
            logger.info(String.format("Error while watching: %s", e.getMessage()));
            try {
                Thread.sleep(1000*5);
            } catch (InterruptedException error) {
                logger.info(error.getMessage());
            }
        }
    }
}