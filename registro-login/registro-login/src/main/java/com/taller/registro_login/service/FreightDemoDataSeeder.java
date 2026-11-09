package com.taller.registro_login.service;

import com.taller.registro_login.model.FreightLane;
import com.taller.registro_login.model.FreightRequest;
import com.taller.registro_login.model.FreightRequestStatus;
import com.taller.registro_login.model.TransportNotice;
import com.taller.registro_login.model.User;
import com.taller.registro_login.repository.FreightLaneRepository;
import com.taller.registro_login.repository.FreightRequestRepository;
import com.taller.registro_login.repository.TransportNoticeRepository;
import com.taller.registro_login.repository.UserRepository;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
@Profile({"default", "dev", "prod"})
@Order(2)
public class FreightDemoDataSeeder implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreightDemoDataSeeder.class);

    private final FreightLaneRepository laneRepository;
    private final FreightRequestRepository requestRepository;
    private final TransportNoticeRepository noticeRepository;
    private final UserRepository userRepository;

    public FreightDemoDataSeeder(FreightLaneRepository laneRepository,
                                 FreightRequestRepository requestRepository,
                                 TransportNoticeRepository noticeRepository,
                                 UserRepository userRepository) {
        this.laneRepository = laneRepository;
        this.requestRepository = requestRepository;
        this.noticeRepository = noticeRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Faker faker = new Faker(Locale.forLanguageTag("es-CO"));
        seedLanes();
        seedNotices();
        seedRequests(faker);
    }

    private void seedLanes() {
        if (laneRepository.count() > 0) {
            return;
        }
        laneRepository.saveAll(List.of(
                lane("Bogotá", "Medellín", "Lun-Sáb · 06:00 y 14:00", 2, 1_850, 18_000),
                lane("Bogotá", "Cali", "Lun-Vie · 07:00", 2, 2_100, 20_000),
                lane("Medellín", "Cartagena", "Mar-Jue-Sáb · 08:00", 3, 2_400, 24_000),
                lane("Cali", "Barranquilla", "Lun-Mié-Vie · 05:30", 3, 2_650, 28_000),
                lane("Bogotá", "Bucaramanga", "Lun-Sáb · 09:00", 2, 1_950, 19_000),
                lane("Medellín", "Pereira", "Lun-Sáb · 07:30 y 15:00", 1, 1_400, 15_000)));
        LOGGER.info("Created six Colombian freight lanes for the demo.");
    }

    private void seedNotices() {
        if (noticeRepository.count() > 0) {
            return;
        }
        noticeRepository.saveAll(List.of(
                new TransportNotice("Seguimiento en línea", "Consulta el avance de tus solicitudes desde tu panel personal.", "Servicio"),
                new TransportNotice("Cobertura nacional", "Contamos con rutas de carga entre las principales ciudades de Colombia.", "Cobertura"),
                new TransportNotice("Tarifas de demostración", "Los valores publicados son referenciales para este prototipo y pueden cambiar.", "Importante")));
        LOGGER.info("Created three Colombian freight notices for the demo.");
    }

    private void seedRequests(Faker faker) {
        if (requestRepository.count() > 0) {
            return;
        }
        List<User> customers = userRepository.findAll().stream()
                .filter(user -> User.ROLE_USER.equals(user.getRole()))
                .toList();
        List<FreightLane> lanes = laneRepository.findByActiveTrueOrderByOriginAscDestinationAsc();
        if (customers.isEmpty() || lanes.isEmpty()) {
            LOGGER.info("Skipping demo freight requests because no customer accounts are available.");
            return;
        }

        FreightRequestStatus[] statuses = FreightRequestStatus.values();
        for (int index = 0; index < 8; index++) {
            User customer = customers.get(index % customers.size());
            FreightLane lane = lanes.get(index % lanes.size());
            BigDecimal weight = BigDecimal.valueOf(faker.number().numberBetween(2, 180))
                    .setScale(2, RoundingMode.HALF_UP);
            FreightRequest request = new FreightRequest();
            request.setTrackingCode("CO-DEMO-" + String.format("%04d", index + 1));
            request.setCustomer(customer);
            request.setLane(lane);
            request.setCargoDescription(faker.commerce().productName());
            request.setRecipientName(faker.name().fullName());
            request.setRecipientPhone(faker.phoneNumber().cellPhone());
            request.setOriginAddress(faker.address().streetAddress());
            request.setDestinationAddress(faker.address().streetAddress());
            request.setWeightKg(weight);
            request.setEstimatedPrice(lane.getPricePerKg().multiply(weight)
                    .max(lane.getMinimumPrice()).setScale(2, RoundingMode.HALF_UP));
            request.setStatus(statuses[index % statuses.length]);
            requestRepository.save(request);
        }
        LOGGER.info("Created eight fictional freight requests for the admin demo.");
    }

    private FreightLane lane(String origin, String destination, String schedule, int days,
                             int pricePerKg, int minimumPrice) {
        return new FreightLane(origin, destination, schedule, days,
                BigDecimal.valueOf(pricePerKg), BigDecimal.valueOf(minimumPrice));
    }
}
