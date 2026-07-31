package com.moveout.kb_backend.forecast.repository;

import com.moveout.kb_backend.forecast.entity.SimulationResult;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimulationResultRepository extends JpaRepository<SimulationResult, UUID> {}
