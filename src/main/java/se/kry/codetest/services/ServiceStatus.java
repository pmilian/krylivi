package se.kry.codetest.services;

import se.kry.codetest.Status;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

public class ServiceStatus {
    private final Service service;
    private final Status status;

    public ServiceStatus(Service service, Status status) {
        checkArgument(nonNull(service), "Service must be non-null");
        checkArgument(nonNull(status), "Status must be non-null");
        this.service = service;
        this.status = status;
    }

    public Service service() {
        return service;
    }

    public Status status() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceStatus that = (ServiceStatus) o;
        return service.equals(that.service) &&
                status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(service, status);
    }
}
