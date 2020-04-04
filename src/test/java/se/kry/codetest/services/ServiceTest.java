package se.kry.codetest.services;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceTest {

    @Test
    void should_create_service() {
        // given
        URI uri = URI.create("http://service");
        Instant now = now();
        String name = "some name";

        // when
        Service service = new Service(uri, name, now);

        // then
        assertThat(service).isNotNull();
        assertThat(service.uri()).isEqualTo(uri);
        assertThat(service.createdAt()).isEqualTo(now);
    }

    @Test
    void should_create_service_with_optional_name() {
        // given
        URI uri = URI.create("http://service");

        // when / then
        assertThat(new Service(uri, null, now()).name()).isEmpty();
    }

    @Test
    void should_throw_exception_on_invalid_parameters() {
        assertThatThrownBy(() -> new Service(null, "name", now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("URI must be non-null");
        assertThatThrownBy(() -> new Service(URI.create("http://service"), "", now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name must be null or non-blank");
        assertThatThrownBy(() -> new Service(URI.create("http://service"), "  ", now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name must be null or non-blank");
        assertThatThrownBy(() -> new Service(URI.create("http://service"), "some name", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Created at must be non-null");
    }

    @Test
    void should_be_equal() {
        Instant now = now();
        Service service = new Service(URI.create("http://service"), "some name", now);
        Service other = new Service(URI.create("http://service"), "some name", now);

        assertThat(service).isEqualTo(other);
    }

    @Test
    void should_not_be_equal() {
        Service service = new Service(URI.create("http://service"), "some name", now());
        Service other = new Service(URI.create("http://service"), "some other name", now());

        assertThat(service).isNotEqualTo(other);
    }

    @Test
    void should_have_same_hash_code() {
        Instant now = now();
        Service service = new Service(URI.create("http://service"), "some name", now);
        Service other = new Service(URI.create("http://service"), "some name", now);

        assertThat(service.hashCode()).isEqualTo(other.hashCode());
    }

    @Test
    void should_not_have_same_hash_code() {
        Service service = new Service(URI.create("http://service"), "some name", now());
        Service other = new Service(URI.create("http://service"), "some other name", now());

        assertThat(service.hashCode()).isNotEqualTo(other.hashCode());
    }

}