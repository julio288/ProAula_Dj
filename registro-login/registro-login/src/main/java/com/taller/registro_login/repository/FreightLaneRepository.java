package com.taller.registro_login.repository;

import com.taller.registro_login.model.FreightLane;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FreightLaneRepository extends JpaRepository<FreightLane, Long> {
    List<FreightLane> findByActiveTrueOrderByOriginAscDestinationAsc();
    List<FreightLane> findAllByOrderByOriginAscDestinationAsc();
}
