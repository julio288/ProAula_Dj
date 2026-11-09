package com.taller.registro_login.service;

import com.taller.registro_login.dto.FreightRequestForm;
import com.taller.registro_login.model.FreightLane;
import com.taller.registro_login.model.FreightRequest;
import com.taller.registro_login.model.FreightRequestStatus;
import com.taller.registro_login.model.User;
import com.taller.registro_login.repository.FreightLaneRepository;
import com.taller.registro_login.repository.FreightRequestRepository;
import com.taller.registro_login.repository.TransportNoticeRepository;
import com.taller.registro_login.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FreightServiceTests {

    private final FreightLaneRepository laneRepository = mock(FreightLaneRepository.class);
    private final FreightRequestRepository requestRepository = mock(FreightRequestRepository.class);
    private final TransportNoticeRepository noticeRepository = mock(TransportNoticeRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final FreightService freightService = new FreightService(
            laneRepository, requestRepository, noticeRepository, userRepository);

    @Test
    void createsRequestWithCalculatedMinimumPriceAndTrackingCode() {
        User customer = new User("client@example.test", "encoded", "Cliente", "Prueba");
        customer.setId(7L);
        FreightLane lane = new FreightLane("Bogotá", "Medellín", "Lun-Vie", 2,
                new BigDecimal("1850.00"), new BigDecimal("20000.00"));
        lane.setId(4L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(customer));
        when(laneRepository.findById(4L)).thenReturn(Optional.of(lane));
        when(requestRepository.save(any(FreightRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FreightRequestForm form = new FreightRequestForm();
        form.setLaneId(4L);
        form.setCargoDescription("Repuestos");
        form.setWeightKg(new BigDecimal("10.00"));
        form.setRecipientName("Persona destino");
        form.setRecipientPhone("3001234567");
        form.setOriginAddress("Calle 1");
        form.setDestinationAddress("Carrera 2");

        FreightRequest request = freightService.createRequest(7L, form);

        assertThat(request.getCustomer()).isSameAs(customer);
        assertThat(request.getEstimatedPrice()).isEqualByComparingTo("20000.00");
        assertThat(request.getStatus()).isEqualTo(FreightRequestStatus.RECEIVED);
        assertThat(request.getTrackingCode()).startsWith("CO-").hasSize(15);
        verify(requestRepository).save(request);
    }

    @Test
    void refusesRequestsForPausedLanes() {
        FreightLane pausedLane = new FreightLane("Bogotá", "Cali", "Lun-Vie", 2,
                BigDecimal.TEN, BigDecimal.TEN);
        pausedLane.setActive(false);
        when(userRepository.findById(7L)).thenReturn(Optional.of(new User(
                "client@example.test", "encoded", "Cliente", "Prueba")));
        when(laneRepository.findById(4L)).thenReturn(Optional.of(pausedLane));
        FreightRequestForm form = new FreightRequestForm();
        form.setLaneId(4L);

        assertThatThrownBy(() -> freightService.createRequest(7L, form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La ruta seleccionada no está disponible.");
        verify(requestRepository, never()).save(any(FreightRequest.class));
    }

    @Test
    void advancesStatusesButRejectsMovingARequestBackwards() {
        FreightRequest request = new FreightRequest();
        request.setStatus(FreightRequestStatus.RECEIVED);
        when(requestRepository.findById(3L)).thenReturn(Optional.of(request));

        freightService.updateRequestStatus(3L, FreightRequestStatus.QUOTED);

        assertThat(request.getStatus()).isEqualTo(FreightRequestStatus.QUOTED);
        verify(requestRepository).save(request);

        assertThatThrownBy(() -> freightService.updateRequestStatus(3L, FreightRequestStatus.RECEIVED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No se puede cambiar el estado");
    }
}
