package com.taller.registro_login.service;

import com.taller.registro_login.dto.FreightLaneForm;
import com.taller.registro_login.dto.FreightRequestForm;
import com.taller.registro_login.dto.TransportNoticeForm;
import com.taller.registro_login.model.FreightLane;
import com.taller.registro_login.model.FreightRequest;
import com.taller.registro_login.model.FreightRequestStatus;
import com.taller.registro_login.model.TransportNotice;
import com.taller.registro_login.model.User;
import com.taller.registro_login.repository.FreightLaneRepository;
import com.taller.registro_login.repository.FreightRequestRepository;
import com.taller.registro_login.repository.TransportNoticeRepository;
import com.taller.registro_login.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class FreightService {

    private final FreightLaneRepository laneRepository;
    private final FreightRequestRepository requestRepository;
    private final TransportNoticeRepository noticeRepository;
    private final UserRepository userRepository;

    public FreightService(FreightLaneRepository laneRepository,
                          FreightRequestRepository requestRepository,
                          TransportNoticeRepository noticeRepository,
                          UserRepository userRepository) {
        this.laneRepository = laneRepository;
        this.requestRepository = requestRepository;
        this.noticeRepository = noticeRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<FreightLane> findAvailableLanes() {
        return laneRepository.findByActiveTrueOrderByOriginAscDestinationAsc();
    }

    @Transactional(readOnly = true)
    public List<FreightLane> findAllLanes() {
        return laneRepository.findAllByOrderByOriginAscDestinationAsc();
    }

    @Transactional(readOnly = true)
    public List<TransportNotice> findActiveNotices() {
        return noticeRepository.findByActiveTrueOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<TransportNotice> findAllNotices() {
        return noticeRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<FreightRequest> findCustomerRequests(Long customerId) {
        return requestRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public List<FreightRequest> findAllRequests() {
        return requestRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<FreightRequest> findRecentRequests() {
        return requestRepository.findTop5ByOrderByCreatedAtDesc();
    }

    @Transactional
    public FreightRequest createRequest(Long customerId, FreightRequestForm form) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("La cuenta del usuario ya no está disponible."));
        FreightLane lane = laneRepository.findById(form.getLaneId())
                .filter(FreightLane::isActive)
                .orElseThrow(() -> new IllegalArgumentException("La ruta seleccionada no está disponible."));

        BigDecimal calculatedPrice = lane.getPricePerKg()
                .multiply(form.getWeightKg())
                .max(lane.getMinimumPrice())
                .setScale(2, RoundingMode.HALF_UP);

        FreightRequest request = new FreightRequest();
        request.setTrackingCode("CO-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                .toUpperCase());
        request.setCustomer(customer);
        request.setLane(lane);
        request.setCargoDescription(form.getCargoDescription().trim());
        request.setWeightKg(form.getWeightKg());
        request.setRecipientName(form.getRecipientName().trim());
        request.setRecipientPhone(form.getRecipientPhone().trim());
        request.setOriginAddress(form.getOriginAddress().trim());
        request.setDestinationAddress(form.getDestinationAddress().trim());
        request.setEstimatedPrice(calculatedPrice);
        request.setStatus(FreightRequestStatus.RECEIVED);
        return requestRepository.save(request);
    }

    @Transactional
    public FreightLane createLane(FreightLaneForm form) {
        return laneRepository.save(toLane(form, new FreightLane()));
    }

    @Transactional
    public FreightLane updateLane(Long id, FreightLaneForm form) {
        FreightLane lane = laneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La ruta solicitada no existe."));
        return laneRepository.save(toLane(form, lane));
    }

    @Transactional
    public void toggleLane(Long id) {
        FreightLane lane = laneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La ruta solicitada no existe."));
        lane.setActive(!lane.isActive());
        laneRepository.save(lane);
    }

    @Transactional
    public TransportNotice createNotice(TransportNoticeForm form) {
        return noticeRepository.save(new TransportNotice(
                form.getTitle().trim(), form.getBody().trim(), form.getCategory().trim()));
    }

    @Transactional
    public void deleteNotice(Long id) {
        TransportNotice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El aviso solicitado no existe."));
        noticeRepository.delete(notice);
    }

    @Transactional
    public void updateRequestStatus(Long id, FreightRequestStatus status) {
        FreightRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La solicitud de carga no existe."));
        if (status == null) {
            throw new IllegalArgumentException("Selecciona un estado válido.");
        }
        FreightRequestStatus current = request.getStatus();
        if (current == status) {
            return;
        }
        boolean validTransition = switch (current) {
            case RECEIVED -> status == FreightRequestStatus.QUOTED || status == FreightRequestStatus.CANCELLED;
            case QUOTED -> status == FreightRequestStatus.IN_TRANSIT || status == FreightRequestStatus.CANCELLED;
            case IN_TRANSIT -> status == FreightRequestStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
        if (!validTransition) {
            throw new IllegalArgumentException("No se puede cambiar el estado de "
                    + current.getLabel() + " a " + status.getLabel() + ".");
        }
        request.setStatus(status);
        requestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public long countLanes() {
        return laneRepository.count();
    }

    @Transactional(readOnly = true)
    public long countNotices() {
        return noticeRepository.count();
    }

    @Transactional(readOnly = true)
    public long countRequests() {
        return requestRepository.count();
    }

    @Transactional(readOnly = true)
    public long countOpenRequests() {
        return requestRepository.countByStatus(FreightRequestStatus.RECEIVED)
                + requestRepository.countByStatus(FreightRequestStatus.QUOTED);
    }

    @Transactional(readOnly = true)
    public long countRequestsByStatus(FreightRequestStatus status) {
        return requestRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public BigDecimal sumEstimatedShipmentValue() {
        return requestRepository.sumEstimatedShipmentValue();
    }

    private FreightLane toLane(FreightLaneForm form, FreightLane lane) {
        lane.setOrigin(form.getOrigin().trim());
        lane.setDestination(form.getDestination().trim());
        lane.setSchedule(form.getSchedule().trim());
        lane.setTransitDays(form.getTransitDays());
        lane.setPricePerKg(form.getPricePerKg());
        lane.setMinimumPrice(form.getMinimumPrice());
        return lane;
    }
}
