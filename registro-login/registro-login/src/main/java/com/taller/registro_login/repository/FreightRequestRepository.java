package com.taller.registro_login.repository;

import com.taller.registro_login.model.FreightRequest;
import com.taller.registro_login.model.FreightRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface FreightRequestRepository extends JpaRepository<FreightRequest, Long> {
    @EntityGraph(attributePaths = {"customer", "lane"})
    List<FreightRequest> findAllByOrderByCreatedAtDesc();
    @EntityGraph(attributePaths = {"customer", "lane"})
    List<FreightRequest> findTop5ByOrderByCreatedAtDesc();
    @EntityGraph(attributePaths = "lane")
    List<FreightRequest> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    long countByStatus(FreightRequestStatus status);
    @Query("select coalesce(sum(request.estimatedPrice), 0) from FreightRequest request")
    BigDecimal sumEstimatedShipmentValue();
    void deleteByCustomerId(Long customerId);
}
