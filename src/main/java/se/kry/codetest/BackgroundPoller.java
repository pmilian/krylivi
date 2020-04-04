package se.kry.codetest;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import io.vertx.core.Future;
import se.kry.codetest.services.Service;
import se.kry.codetest.services.ServiceStatus;
import se.kry.codetest.services.ServiceStatusStore;

import java.util.List;
import java.util.concurrent.ExecutorService;

import static java.util.stream.Collectors.toList;
import static se.kry.codetest.Status.FAIL;
import static se.kry.codetest.Status.OK;

public class BackgroundPoller {

    private final ServiceStatusStore store;
    // didn't figure out how to use the vertx one in time, sorry
    private final OkHttpClient httpClient;
    private final ExecutorService executor;

    public BackgroundPoller(ServiceStatusStore store, OkHttpClient httpClient, ExecutorService executor) {
        this.store = store;
        this.httpClient = httpClient;
        this.executor = executor;
    }

    public Future<Void> poll() {
        return Future.future(future -> {
            try {
                services()
                        .parallelStream()
                        .forEach(this::updateStatus);
                future.complete();
            } catch (Exception e) {
                future.fail(e);
            }
        });
    }

    private void updateStatus(Service service) {
        store.update(service.uri(), new ServiceStatus(service, getStatus(service)));
    }

    private Status getStatus(Service service) {
        try {
            java.util.concurrent.Future<Status> future = executor.submit(() -> {
                Request request = new Request.Builder()
                        .url(service.uri().toString())
                        .build();
                Response response = httpClient.newCall(request).execute();
                return response.isSuccessful() ? OK : FAIL;
            });
            return future.get();
        } catch (Exception e) {
            e.printStackTrace();
            return FAIL;
        }
    }

    private List<Service> services() {
        return store.getAll()
                .stream()
                .map(ServiceStatus::service)
                .collect(toList());
    }
}
