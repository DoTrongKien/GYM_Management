package com.example.gymmanagement.repository;

import com.example.gymmanagement.entity.MembershipCancellationRequest;
import com.example.gymmanagement.enums.CancellationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipCancellationRequestRepository extends JpaRepository<MembershipCancellationRequest, Long> {
    List<MembershipCancellationRequest> findByUserIdOrderByRequestedAtDesc(Long userId);
    List<MembershipCancellationRequest> findAllByOrderByRequestedAtDesc();
    List<MembershipCancellationRequest> findByStatusOrderByRequestedAtDesc(CancellationStatus status);
    Optional<MembershipCancellationRequest> findByMembershipIdAndStatus(Long membershipId, CancellationStatus status);
}