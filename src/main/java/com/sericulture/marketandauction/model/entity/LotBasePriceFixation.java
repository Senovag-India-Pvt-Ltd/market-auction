package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.time.LocalDate;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LotBasePriceFixation extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "LOT_BASE_PRICE_FIXATION_SEQ")
    @SequenceGenerator(name = "LOT_BASE_PRICE_FIXATION_SEQ", sequenceName = "LOT_BASE_PRICE_FIXATION_SEQ", allocationSize = 1)
    @Column(name = "LOT_BASE_PRICE_FIXATION_ID")
    private BigInteger id;

    private int marketId;

    private LocalDate fixationDate;

    private int pricePerKg;
}
