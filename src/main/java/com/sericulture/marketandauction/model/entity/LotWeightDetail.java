package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LotWeightDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "LOT_WEIGHT_DETAIL_SEQ")
    @SequenceGenerator(name = "LOT_WEIGHT_DETAIL_SEQ", sequenceName = "LOT_WEIGHT_DETAIL_SEQ", allocationSize = 1)
    @Column(name = "LOT_WEIGHT_DETAIL_ID")
    private BigInteger id;

    @Column(name = "LOT_ID")
    private BigInteger lotId;

    @Column(name = "CRATE_NUMBER")
    private int crateNumber;

    @Column(name = "GROSS_WEIGHT")
    private float grossWeight;

    @Column(name = "NET_WEIGHT")
    private float netWeight;

    public LotWeightDetail(BigInteger lotId, int crateNumber, float grossWeight, float netWeight) {
        this.lotId = lotId;
        this.crateNumber = crateNumber;
        this.grossWeight = grossWeight;
        this.netWeight = netWeight;
    }
}
