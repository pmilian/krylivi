package se.kry.codetest.persistence;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import org.junit.jupiter.api.Test;
import se.kry.codetest.services.Service;
import se.kry.codetest.services.ServiceRepository;

import java.net.URI;
import java.time.Instant;
import java.util.Collection;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.list;
import static org.mockito.Mockito.*;

class SqliteServiceRepositoryTest {

    private DBConnector dbConnector = mock(DBConnector.class);
    private ServiceRepository repository = new SqliteServiceRepository(dbConnector);

    @Test
    void should_add() {
        // given
        Service service = new Service(URI.create("http://localhost"), "some name", Instant.ofEpochMilli(1234L));
        when(dbConnector.query(any(), any())).thenReturn(succeededFuture());

        // when
        Future<Service> added = repository.add(service);

        // then
        verify(dbConnector).query(
                "insert into service (url, name, created_at) values (?, ?, ?)",
                new JsonArray().add("http://localhost").add("some name").add(1234L)
        );
        assertThat(added.isComplete()).isTrue();
        assertThat(added.result()).isEqualTo(service);
    }

    @Test
    void should_add_with_empty_name() {
        // given
        Service service = new Service(URI.create("http://localhost"), null, Instant.ofEpochMilli(1234L));
        when(dbConnector.query(any(), any())).thenReturn(succeededFuture());

        // when
        Future<Service> added = repository.add(service);

        // then
        verify(dbConnector).query(
                "insert into service (url, name, created_at) values (?, ?, ?)",
                new JsonArray().add("http://localhost").addNull().add(1234L)
        );
        assertThat(added.isComplete()).isTrue();
        assertThat(added.result()).isEqualTo(service);
    }

    @Test
    void should_propagate_failure_on_add() {
        // given
        RuntimeException exception = new RuntimeException("test exception");
        when(dbConnector.query(any(), any())).thenReturn(failedFuture(exception));

        // when
        Future<Service> added = repository.add(new Service(URI.create("http://localhost"), "some name", now()));

        // then
        assertThat(added.failed()).isTrue();
        assertThat(added.cause()).isEqualTo(exception);
    }

    @Test
    void should_delete() {
        // given
        Service service = new Service(URI.create("http://localhost"), "some name", now());
        when(dbConnector.query(any(), any())).thenReturn(succeededFuture());

        // when
        Future<Void> deleted = repository.delete(service);

        // then
        verify(dbConnector).query(
                "delete from service where url = ?",
                new JsonArray().add("http://localhost")
        );
        assertThat(deleted.isComplete()).isTrue();
    }

    @Test
    void should_propagate_failure_on_delete() {
        // given
        RuntimeException exception = new RuntimeException("test exception");
        when(dbConnector.query(any(), any())).thenReturn(failedFuture(exception));

        // when
        Future<Void> deleted = repository.delete(new Service(URI.create("http://localhost"), "some name", now()));

        // then
        assertThat(deleted.failed()).isTrue();
        assertThat(deleted.cause()).isEqualTo(exception);
    }

    @Test
    void shouldFindAll() {
        // given
        ResultSet resultSet = mock(ResultSet.class);

        when(resultSet.getRows()).thenReturn(list(
                new JsonObject().put("url", "http://service-1.com").put("name", "some name").put("created_at", 1234L),
                new JsonObject().put("url", "http://service-2.com").put("name", (String)null).put("created_at", 5678L),
                new JsonObject().put("url", "http://service-3.com").put("name", "another name").put("created_at", 111L)
        ));
        when(dbConnector.query("select * from service")).thenReturn(succeededFuture(resultSet));

        // when
        Future<Collection<Service>> services = repository.findAll();

        // then
        assertThat(services.isComplete()).isTrue();
        assertThat(services.result()).containsExactlyInAnyOrder(
                new Service(URI.create("http://service-1.com"), "some name", Instant.ofEpochMilli(1234L)),
                new Service(URI.create("http://service-2.com"), null, Instant.ofEpochMilli(5678L)),
                new Service(URI.create("http://service-3.com"), "another name", Instant.ofEpochMilli(111L))
        );
    }

    @Test
    void should_propagate_failure_on_findAll() {
        // given
        RuntimeException exception = new RuntimeException("test exception");
        when(dbConnector.query(any())).thenReturn(failedFuture(exception));

        // when
        Future<Collection<Service>> services = repository.findAll();

        // then
        assertThat(services.failed()).isTrue();
        assertThat(services.cause()).isEqualTo(exception);
    }

}