package com.healthMini.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Repository;

import com.healthMini.entityDto.HealthCareUser;

@EnableJpaRepositories
@Repository
public interface HealthCareUserRepository extends JpaRepository<HealthCareUser, Integer>{

	

	List<HealthCareUser> findByEmailOrPhoneNumber(String email, String string);

	HealthCareUser findByEmail(String email);

	List<HealthCareUser> findByUserIdBetween(int startId, int endId, Sort by);

	List<HealthCareUser> findByUserIdBetweenAndRole(int startId, int endId, String role, Sort by);

}
