package se.kry.codetest.services;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static se.kry.codetest.Status.*;

class ServiceStatusTest {

    @Test
    void should_create_service_status() {
        // given
        Service service = mock(Service.class);

        // when
        ServiceStatus status = new ServiceStatus(service, OK);

        // then
        assertThat(status).isNotNull();
        assertThat(status.service()).isEqualTo(service);
        assertThat(status.status()).isEqualTo(OK);
    }

    @Test
    void should_throw_exception_on_invalid_parameters() {
        assertThatThrownBy(() -> new ServiceStatus(null, FAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Service must be non-null");
        assertThatThrownBy(() -> new ServiceStatus(mock(Service.class), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Status must be non-null");
    }

    @Test
    void should_be_equal() {
        Service service = mock(Service.class);

        ServiceStatus status = new ServiceStatus(service, UNKNOWN);
        ServiceStatus other = new ServiceStatus(service, UNKNOWN);

        assertThat(status).isEqualTo(other);
    }

    @Test
    void should_not_be_equal() {
        ServiceStatus status = new ServiceStatus(mock(Service.class), UNKNOWN);
        ServiceStatus other = new ServiceStatus(mock(Service.class), FAIL);

        assertThat(status).isNotEqualTo(other);
    }

    @Test
    void should_have_same_hash_code() {
        Service service = mock(Service.class);

        ServiceStatus status = new ServiceStatus(service, UNKNOWN);
        ServiceStatus other = new ServiceStatus(service, UNKNOWN);

        assertThat(status.hashCode()).isEqualTo(other.hashCode());
    }

    @Test
    void should_not_have_same_hash_code() {
        ServiceStatus status = new ServiceStatus(mock(Service.class), UNKNOWN);
        ServiceStatus other = new ServiceStatus(mock(Service.class), FAIL);

        assertThat(status.hashCode()).isNotEqualTo(other.hashCode());
    }
}