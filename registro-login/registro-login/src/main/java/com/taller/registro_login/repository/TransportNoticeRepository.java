package com.taller.registro_login.repository;

import com.taller.registro_login.model.TransportNotice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransportNoticeRepository extends JpaRepository<TransportNotice, Long> {
    List<TransportNotice> findByActiveTrueOrderByCreatedAtDesc();
    List<TransportNotice> findAllByOrderByCreatedAtDesc();
}
