package com.healthMini.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.healthMini.entityDto.HealthData;

@Repository
public interface HealthDataRepository extends JpaRepository<HealthData, Integer> {
	
    List<HealthData> findByUserUserId(int userId);
}
