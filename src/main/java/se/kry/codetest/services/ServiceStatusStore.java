package se.kry.codetest.services;

import io.vertx.core.Future;
import se.kry.codetest.Status;

import java.io.Serializable;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static io.vertx.core.Future.succeededFuture;
import static java.util.Optional.ofNullable;
import static se.kry.codetest.Status.UNKNOWN;

public class ServiceStatusStore {

    private final Map<URI, ServiceStatus> statuses;
    private final ServiceRepository repository;

    public ServiceStatusStore(ServiceRepository repository, Map<URI, ServiceStatus> statuses) {
        this.repository = repository;
        this.statuses = statuses;
    }

    public void init() {
        repository.findAll().setHandler(event -> {
            if (event.succeeded()) {
                event.result().forEach(service ->
                        statuses.put(service.uri(), new ServiceStatus(service, UNKNOWN))
                );
            }
        });
    }

    public Future<ServiceStatus> add(Service service) {
        return repository.add(service).map(persisted -> {
            ServiceStatus status = new ServiceStatus(persisted, UNKNOWN);
            statuses.put(persisted.uri(), status);
            return status;
        });
    }

    public Future<Void> delete(URI uri) {
        Optional<ServiceStatus> optionalStatus = ofNullable(statuses.get(uri));
        if (optionalStatus.isPresent()) {
            ServiceStatus serviceStatus = optionalStatus.get();
            return repository
                    .delete(serviceStatus.service())
                    .map(aVoid -> {
                        statuses.remove(uri);
                        return null;
                    });
        }
        // idempotent
        return succeededFuture();
    }

    public Collection<ServiceStatus> getAll() {
        return statuses.values();
    }

    public void update(URI uri, ServiceStatus status) {
        statuses.put(uri, status);
    }
}
