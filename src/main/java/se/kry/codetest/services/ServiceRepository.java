package se.kry.codetest.services;

import io.vertx.core.Future;

import java.util.Collection;

public interface ServiceRepository {

    Future<Service> add(Service service);

    Future<Void> delete(Service service);

    Future<Collection<Service>> findAll();

}
