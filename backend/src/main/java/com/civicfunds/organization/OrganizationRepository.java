package com.civicfunds.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByUei(String uei);

    Optional<Organization> findByNormalizedName(String normalizedName);

    List<Organization> findByReviewStatus(Organization.ReviewStatus status);

    /**
     * Trigram-blocked candidate lookup. Cheap because of the GIN index, and it
     * keeps the expensive scoring pass to a handful of rows rather than the
     * whole table.
     */
    @Query(value = "SELECT * FROM organization o "
                 + "WHERE similarity(o.normalized_name, :name) > :threshold "
                 + "ORDER BY similarity(o.normalized_name, :name) DESC "
                 + "LIMIT :limit", nativeQuery = true)
    List<Organization> findFuzzyCandidates(@Param("name") String normalizedName,
                                           @Param("threshold") double threshold,
                                           @Param("limit") int limit);
}
