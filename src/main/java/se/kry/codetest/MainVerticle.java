package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import se.kry.codetest.services.Service;
import se.kry.codetest.services.ServiceStatus;
import se.kry.codetest.services.ServiceStatusStore;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import static java.time.format.DateTimeFormatter.ISO_DATE;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.stream.Collectors.toList;

public class MainVerticle extends AbstractVerticle {

    private final ServiceStatusStore statusStore;
    private final BackgroundPoller poller;

    public MainVerticle(ServiceStatusStore statusStore, BackgroundPoller poller) {
        this.statusStore = statusStore;
        this.poller = poller;
    }

    @Override
    public void start(Future<Void> startFuture) {
        statusStore.init();
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        vertx.setPeriodic(1000 * 60, timerId -> poller.poll());
        setRoutes(router);
        vertx
                .createHttpServer()
                .requestHandler(router)
                .listen(8080, result -> {
                    if (result.succeeded()) {
                        System.out.println("KRY code test service started");
                        startFuture.complete();
                    } else {
                        startFuture.fail(result.cause());
                    }
                });
    }

    private void setRoutes(Router router) {
        router.route("/*").handler(StaticHandler.create());
        router.get("/service").handler(this::getServiceStatuses);
        router.post("/service").handler(this::addService);
        router.delete("/service").handler(this::deleteService);
    }

    private void deleteService(RoutingContext req) {
        JsonObject json = req.getBodyAsJson();
        URI uri = URI.create(json.getString("url"));
        statusStore.delete(uri).setHandler(event -> {
            if (event.succeeded()) {
                req.response()
                        .putHeader("content-type", "text/plain")
                        .end("OK");
            } else {
                req.response()
                        .putHeader("content-type", "text/plain")
                        .setStatusCode(500)
                        .end("FAIL");
            }
        });
    }

    private void addService(RoutingContext req) {
        JsonObject json = req.getBodyAsJson();
        URI uri = URI.create(json.getString("url"));
        String name = json.getString("name");
        Instant now = Instant.now();
        statusStore.add(new Service(uri, name, now)).setHandler(event -> {
            if (event.succeeded()) {
                req.response()
                        .putHeader("content-type", "text/plain")
                        .end("OK");
            } else {
                System.err.println("error");
                System.err.println(event.cause());
                // TODO: should handle a duplicate error as a success
                req.response()
                        .putHeader("content-type", "text/plain")
                        .setStatusCode(500)
                        .end("FAIL");
            }
        });
    }

    private void getServiceStatuses(RoutingContext req) {
        List<JsonObject> jsonServices = statusStore.getAll()
                .stream()
                .map(this::serialize)
                .collect(toList());
        req.response()
                .putHeader("content-type", "application/json")
                .end(new JsonArray(jsonServices).encode());
    }

    private JsonObject serialize(ServiceStatus serviceStatus) {
        Service service = serviceStatus.service();
        Status status = serviceStatus.status();
        JsonObject json = new JsonObject()
                .put("url", service.uri().toString())
                .put("created_at", ISO_INSTANT.format(service.createdAt()))
                .put("status", status);

        service.name().ifPresent(name -> json.put("name", name));

        return json;
    }

}



