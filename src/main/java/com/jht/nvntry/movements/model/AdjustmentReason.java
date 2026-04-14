package com.jht.nvntry.movements.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "adjustment_reasons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdjustmentReason {
        @Id
        @Column(nullable = false)
        private String code;

        @Column(nullable = false)
        private String description;

        @Column(nullable = false)
        private boolean active;
}