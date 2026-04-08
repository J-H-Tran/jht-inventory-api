package com.jht.nvntry.domain.reservation.mapper;

import com.jht.nvntry.domain.reservation.Reservation;
import com.jht.nvntry.domain.reservation.dto.ReservationDTO;
import org.springframework.stereotype.Component;
import java.time.Clock;

@Component
public class ReservationMapper {
    private final Clock clock;

    public ReservationMapper(Clock clock) {
        this.clock = clock;
    }

    public ReservationDTO toDTO(Reservation r) {
        if (r == null) return null;
        return new ReservationDTO(
            r.getProductId(),
            r.getLocationId(),
            r.getQuantity()
        );
    }

    public Reservation toEntity(ReservationDTO dto) {
        if (dto == null) return null;
        return Reservation.createActive(
                dto.productId(),
                dto.locationId(),
                dto.quantity(),
                clock
        );
    }
}