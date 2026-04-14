package com.jht.nvntry.movements;

import com.jht.nvntry.movements.model.AdjustmentReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AdjustmentReasonRepository extends JpaRepository<AdjustmentReason, String> {

    @Query("""
        SELECT a
        FROM AdjustmentReason a
        WHERE a.code = :code
    """
    )
    Optional<AdjustmentReason> findByCode(String code);
}