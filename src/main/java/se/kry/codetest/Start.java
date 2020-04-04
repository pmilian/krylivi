package se.kry.codetest;

import com.squareup.okhttp.OkHttpClient;
import io.vertx.core.Vertx;
import se.kry.codetest.persistence.DBConnector;
import se.kry.codetest.persistence.SqliteServiceRepository;
import se.kry.codetest.services.ServiceRepository;
import se.kry.codetest.services.ServiceStatusStore;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;

public class Start {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    DBConnector dbConnector = new DBConnector(vertx);
    ServiceRepository serviceRepository = new SqliteServiceRepository(dbConnector);
    ServiceStatusStore serviceStatusStore = new ServiceStatusStore(
            serviceRepository,
            new ConcurrentHashMap<>()
    );
    OkHttpClient httpClient = new OkHttpClient();
    // to avoid getting slowed down by an unresponsive service
    httpClient.setConnectTimeout(1, SECONDS);
    httpClient.setReadTimeout(2, SECONDS);

    // to avoid hogging all the threads of our server
    ExecutorService executorService = newFixedThreadPool(4);

    BackgroundPoller poller = new BackgroundPoller(serviceStatusStore, httpClient, executorService);

    vertx.deployVerticle(new MainVerticle(serviceStatusStore, poller));
  }
}
