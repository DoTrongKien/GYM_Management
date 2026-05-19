package com.example.gymmanagement.repository;
import com.example.gymmanagement.entity.ServiceRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
public interface ServiceRatingRepository extends JpaRepository<ServiceRating, Long> {
    List<ServiceRating> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<ServiceRating> findByIsPublicTrueOrderByCreatedAtDesc();
    @Query("SELECT AVG(r.rating) FROM ServiceRating r WHERE r.serviceType = :serviceType")
    Double getAverageRatingByType(String serviceType);
}