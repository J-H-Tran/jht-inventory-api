package com.jht.nvntry.repository;

import com.jht.nvntry.domain.location.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {
}