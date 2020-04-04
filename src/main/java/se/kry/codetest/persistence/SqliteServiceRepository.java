package se.kry.codetest.persistence;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import se.kry.codetest.services.Service;
import se.kry.codetest.services.ServiceRepository;

import java.net.URI;
import java.time.Instant;
import java.util.Collection;

import static java.util.stream.Collectors.toList;

public class SqliteServiceRepository implements ServiceRepository {

    private static final String COLUMN_URL = "url";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_CREATED_AT = "created_at";

    private final DBConnector dbConnector;

    public SqliteServiceRepository(DBConnector dbConnector) {
        this.dbConnector = dbConnector;
    }

    @Override
    public Future<Service> add(Service service) {
        JsonArray values = new JsonArray();
        values.add(service.uri().toString());
        if (service.name().isPresent()) {
            values.add(service.name().get());
        } else {
            values.addNull();
        }
        values.add(service.createdAt().toEpochMilli());
        return dbConnector
                .query("insert into service (url, name, created_at) values (?, ?, ?)", values)
                .map(service);
    }

    @Override
    public Future<Void> delete(Service service) {
        return dbConnector.query(
                "delete from service where url = ?",
                new JsonArray().add(service.uri().toString())
        ).mapEmpty();
    }

    @Override
    public Future<Collection<Service>> findAll() {
        return dbConnector.query("select * from service").map(this::toServices);
    }

    private Collection<Service> toServices(ResultSet resultSet) {
        return resultSet.getRows()
                .stream()
                .map(this::toService)
                .collect(toList());
    }

    private Service toService(JsonObject entries) {
        return new Service(
                URI.create(entries.getString(COLUMN_URL)),
                entries.getString(COLUMN_NAME),
                Instant.ofEpochMilli(entries.getLong(COLUMN_CREATED_AT))
        );
    }
}
