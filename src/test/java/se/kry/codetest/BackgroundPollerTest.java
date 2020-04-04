package se.kry.codetest;

import com.squareup.okhttp.*;
import io.vertx.core.Future;
import org.junit.jupiter.api.Test;
import se.kry.codetest.services.Service;
import se.kry.codetest.services.ServiceStatus;
import se.kry.codetest.services.ServiceStatusStore;

import java.io.IOException;
import java.net.URI;

import static java.time.Instant.now;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.list;
import static org.mockito.Mockito.*;
import static se.kry.codetest.Status.*;

class BackgroundPollerTest {

    private ServiceStatusStore store = mock(ServiceStatusStore.class);
    private OkHttpClient httpClient = mock(OkHttpClient.class);
    private BackgroundPoller poller = new BackgroundPoller(store, httpClient, newFixedThreadPool(2));

    @Test
    void should_call_GET_on_all_services() {
        // given
        Service service1 = new Service(URI.create("https://www.kry-1.se/"), null, now());
        Service service2 = new Service(URI.create("https://www.kry-2.se/"), null, now());
        Service service3 = new Service(URI.create("https://www.kry-3.se/"), null, now());
        when(store.getAll()).thenReturn(list(
                new ServiceStatus(service1, OK),
                new ServiceStatus(service2, FAIL),
                new ServiceStatus(service3, UNKNOWN)
        ));

        when(httpClient.newCall(any())).thenAnswer(invocation -> {
            Request request = invocation.getArgument(0);
            if (request.urlString().equals(service1.uri().toString())) {
                return aSuccess();
            }
            if (request.urlString().equals(service2.uri().toString())) {
                return aFailure();
            }
            return aSuccess();
        });

        // when
        Future<Void> result = poller.poll();

        // then
        assertThat(result.isComplete());
        verify(store).update(URI.create("https://www.kry-1.se/"), new ServiceStatus(service1, OK));
        verify(store).update(URI.create("https://www.kry-2.se/"), new ServiceStatus(service2, FAIL));
        verify(store).update(URI.create("https://www.kry-3.se/"), new ServiceStatus(service3, OK));
    }

    private Call aSuccess() throws IOException {
        Call call = mock(Call.class);
        when(call.execute()).thenReturn(aResponse().code(200).build());
        return call;
    }

    private Call aFailure() throws IOException {
        Call call = mock(Call.class);
        when(call.execute()).thenReturn(aResponse().code(503).build());
        return call;
    }

    private Response.Builder aResponse() {
        Request request = new Request.Builder().url("http://someurl").build();
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_0)
                .code(200);
    }

}