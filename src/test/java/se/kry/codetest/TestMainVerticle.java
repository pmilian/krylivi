package se.kry.codetest;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import se.kry.codetest.services.Service;
import se.kry.codetest.services.ServiceRepository;
import se.kry.codetest.services.ServiceStatus;
import se.kry.codetest.services.ServiceStatusStore;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.vertx.core.Future.succeededFuture;
import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.list;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static se.kry.codetest.Status.*;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

    private ServiceStatusStore statusStore = mock(ServiceStatusStore.class);
    private BackgroundPoller poller = mock(BackgroundPoller.class);

    @BeforeEach
    void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
        vertx.deployVerticle(new MainVerticle(statusStore, poller), testContext.succeeding(id -> testContext.completeNow()));
    }

    @Test
    @DisplayName("Should init the status store when starting the server")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void init_status_store(Vertx vertx, VertxTestContext testContext) {
        verify(statusStore).init();
        testContext.completeNow();
    }

    @Test
    @DisplayName("Should get all service statuses")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void get_all(Vertx vertx, VertxTestContext testContext) {
        when(statusStore.getAll()).thenReturn(list(
                new ServiceStatus(new Service(URI.create("https://www.kry-1.se"), "some name", Instant.parse("2020-04-04T19:40:01.084884Z")), OK),
                new ServiceStatus(new Service(URI.create("https://www.kry-2.se"), null, Instant.parse("2020-04-04T19:38:57.980500Z")), FAIL),
                new ServiceStatus(new Service(URI.create("https://www.kry-3.se"), "kry", Instant.parse("2020-04-04T19:29:08.327Z")), UNKNOWN)
        ));

        WebClient.create(vertx)
                .get(8080, "::1", "/service")
                .send(response -> testContext.verify(() -> {
                    assertThat(response.result().statusCode()).isEqualTo(200);
                    JsonArray body = response.result().bodyAsJsonArray();
                    assertThat(body).hasSize(3);
                    assertThat(body).containsExactlyInAnyOrder(
                            new JsonObject()
                                    .put("url", "https://www.kry-1.se")
                                    .put("name", "some name")
                                    .put("created_at", "2020-04-04T19:40:01.084884Z")
                                    .put("status", "OK"),
                            new JsonObject()
                                    .put("url", "https://www.kry-2.se")
                                    .put("created_at", "2020-04-04T19:38:57.980500Z")
                                    .put("status", "FAIL"),
                            new JsonObject()
                                    .put("url", "https://www.kry-3.se")
                                    .put("name", "kry")
                                    .put("created_at", "2020-04-04T19:29:08.327Z")
                                    .put("status", "UNKNOWN")
                    );
                    testContext.completeNow();
                }));
    }

    @Test
    @DisplayName("Should add service")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void add_service(Vertx vertx, VertxTestContext testContext) {
        String url = "https://www.kry-4.se";
        Service service = new Service(URI.create(url), "some service", now());
        when(statusStore.add(any())).thenReturn(succeededFuture(new ServiceStatus(service, UNKNOWN)));
        JsonObject json = new JsonObject().put("url", url);

        WebClient.create(vertx)
                .post(8080, "::1", "/service")
                .sendJson(json, response -> testContext.verify(() -> {
                    assertThat(response.result().statusCode()).isEqualTo(200);
                    assertThat(response.result().bodyAsString()).isEqualTo("OK");
                    verify(statusStore).add(any());
                    testContext.completeNow();
                }));
    }

    @Test
    @DisplayName("Should delete service")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void delete_service(Vertx vertx, VertxTestContext testContext) {
        String url = "https://www.kry-2.se";
        when(statusStore.delete(any())).thenReturn(succeededFuture());
        JsonObject json = new JsonObject().put("url", url);

        WebClient.create(vertx)
                .delete(8080, "::1", "/service")
                .sendJson(json, response -> testContext.verify(() -> {
                    assertThat(response.result().statusCode()).isEqualTo(200);
                    assertThat(response.result().bodyAsString()).isEqualTo("OK");
                    verify(statusStore).delete(URI.create(url));
                    testContext.completeNow();
                }));
    }

}
