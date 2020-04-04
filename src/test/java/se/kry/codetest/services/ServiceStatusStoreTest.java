package se.kry.codetest.services;

import io.vertx.core.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.emptyList;
import static org.assertj.core.util.Lists.list;
import static org.mockito.Mockito.*;
import static se.kry.codetest.Status.*;

class ServiceStatusStoreTest {

    private ServiceRepository repository = mock(ServiceRepository.class);
    private Map<URI, ServiceStatus> statusMap = new HashMap<>();
    private ServiceStatusStore store = new ServiceStatusStore(repository, statusMap);

    private Service service = new Service(URI.create("https://www.kry.se"), "test service", now());

    @BeforeEach
    void setup() {
        when(repository.findAll()).thenReturn(succeededFuture(emptyList()));
    }

    @Test
    void should_load_services_on_init() {
        // given
        Collection<Service> persistedServices = list(
                new Service(URI.create("https://www.kry-1.se"), "test service 1", now()),
                new Service(URI.create("https://www.kry-2.se"), "test service 2", now()),
                new Service(URI.create("https://www.kry-3.se"), "test service 3", now())
        );
        when(repository.findAll()).thenReturn(succeededFuture(persistedServices));

        // when
        store.init();

        // then
        Collection<ServiceStatus> statuses = store.getAll();
        assertThat(statuses)
                .extracting(ServiceStatus::service)
                .containsExactlyInAnyOrderElementsOf(persistedServices);
        assertThat(statuses)
                .extracting(ServiceStatus::status)
                .containsOnly(UNKNOWN);
    }

    @Test
    void should_get_all() {
        // given
        ServiceStatus status1 = new ServiceStatus(mock(Service.class), OK);
        ServiceStatus status2 = new ServiceStatus(mock(Service.class), FAIL);
        ServiceStatus status3 = new ServiceStatus(mock(Service.class), UNKNOWN);
        statusMap.put(URI.create("https://www.kry-1.se"), status1);
        statusMap.put(URI.create("https://www.kry-2.se"), status2);
        statusMap.put(URI.create("https://www.kry-3.se"), status3);

        // when
        Collection<ServiceStatus> statuses = store.getAll();

        // then
        assertThat(statuses).containsExactlyInAnyOrder(
                status1,
                status2,
                status3
        );
    }

    @Test
    void should_ignore_failure_on_init() {
        // given
        when(repository.findAll()).thenReturn(failedFuture(new RuntimeException("test exception")));

        // when
        store.init();

        // then
        assertThat(store.getAll()).isEmpty();
    }

    @Test
    void should_add_service_and_persist() {
        // given
        when(repository.add(any())).thenReturn(succeededFuture(service));

        // when
        Future<ServiceStatus> added = store.add(service);

        // then
        assertThat(added.isComplete()).isTrue();
        assertThat(added.result()).isEqualTo(new ServiceStatus(service, UNKNOWN));
        assertThat(statusMap).containsEntry(service.uri(), new ServiceStatus(service, UNKNOWN));
        verify(repository).add(service);
    }

    @Test
    void should_propagate_failure_on_add() {
        // given
        RuntimeException exception = new RuntimeException("test exception");
        when(repository.add(service)).thenReturn(failedFuture(exception));

        // when
        Future<ServiceStatus> added = store.add(service);

        // then
        assertThat(added.failed()).isTrue();
        assertThat(added.cause()).isEqualTo(exception);
        assertThat(statusMap).isEmpty();
    }

    @Test
    void should_delete_service_and_persist() {
        // given
        ServiceStatus status = new ServiceStatus(service, OK);
        statusMap.put(service.uri(), status);
        when(repository.delete(service)).thenReturn(succeededFuture());

        // when
        Future<Void> deleted = store.delete(service.uri());

        // then
        assertThat(deleted.isComplete()).isTrue();
        assertThat(statusMap).doesNotContainEntry(service.uri(), status);
        verify(repository).delete(service);
    }

    @Test
    void should_ignore_when_not_exists_on_delete() {
        // when
        Future<Void> deleted = store.delete(URI.create("udp://not-present"));

        // then
        assertThat(deleted.isComplete()).isTrue();
        verify(repository, never()).delete(service);
    }

    @Test
    void should_propagate_failure_on_delete() {
        // given
        RuntimeException exception = new RuntimeException("test exception");
        ServiceStatus status = new ServiceStatus(service, OK);
        statusMap.put(service.uri(), status);
        when(repository.delete(service)).thenReturn(failedFuture(exception));

        // when
        Future<Void> deleted = store.delete(service.uri());

        // then
        assertThat(deleted.failed()).isTrue();
        assertThat(deleted.cause()).isEqualTo(exception);
        assertThat(statusMap).containsEntry(service.uri(), status);
    }


    @Test
    void should_update() {
        // given
        ServiceStatus status = new ServiceStatus(service, OK);
        statusMap.put(service.uri(), status);

        // when
        store.update(service.uri(), new ServiceStatus(service, FAIL));

        // then
        assertThat(statusMap).containsEntry(service.uri(), new ServiceStatus(service, FAIL));
    }

}