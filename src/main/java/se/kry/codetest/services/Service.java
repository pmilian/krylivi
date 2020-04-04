package se.kry.codetest.services;

import javax.annotation.Nullable;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.hash;
import static java.util.Objects.nonNull;

public class Service {
    private final URI uri;
    private final Optional<String> name;
    private final Instant createdAt;

    public Service(URI uri, @Nullable String name, Instant createdAt) {
        checkArgument(nonNull(uri), "URI must be non-null");
        checkArgument(name == null || !name.trim().isEmpty(), "Name must be null or non-blank");
        checkArgument(nonNull(createdAt), "Created at must be non-null");
        this.uri = uri;
        this.name = Optional.ofNullable(name);
        this.createdAt = createdAt;
    }

    public URI uri() {
        return uri;
    }

    public Optional<String> name() {
        return name;
    }

    public Instant createdAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Service service = (Service) o;
        return uri.equals(service.uri) &&
                name.equals(service.name) &&
                createdAt.equals(service.createdAt);
    }

    @Override
    public int hashCode() {
        return hash(uri, name, createdAt);
    }
}
