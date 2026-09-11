package com.gymmind.repository;

import com.gymmind.entity.ChallengeParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeParticipantRepository extends JpaRepository<ChallengeParticipant, Long> {

    List<ChallengeParticipant> findByUserId(Long userId);

    List<ChallengeParticipant> findByChallengeIdOrderByProgressDescRankAsc(Long challengeId);

    Optional<ChallengeParticipant> findByChallengeIdAndUserId(Long challengeId, Long userId);

    boolean existsByChallengeIdAndUserId(Long challengeId, Long userId);
}
