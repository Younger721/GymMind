package com.gymmind.repository;

import com.gymmind.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    List<Challenge> findByStatusOrderByCreatedAtDesc(String status);

    List<Challenge> findByCreatorIdOrderByCreatedAtDesc(Long creatorId);
}
