package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigInteger;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MarketMaster extends BaseEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "MARKET_MASTER_SEQ")
    @SequenceGenerator(name = "MARKET_MASTER_SEQ", sequenceName = "MARKET_MASTER_SEQ", allocationSize = 1)
    @Column(name = "MARKET_MASTER_ID")
    private int id;

    @Size(message = "name of the market")
    @Column(name = "MARKET_NAME",unique = true)
    private String name;

    @Column(name = "MARKET_ADDRESS")
    @Size(message = "market address")
    private String address;

    @Column(name = "BOX_WEIGHT")
    private float boxWeight;

    @Column(name = "LOT_WEIGHT")
    private int lotWeight;

    @Column(name = "STATE_ID")
    private int stateId;

    @Column(name = "DISTRICT_ID")
    private int districtId;

    @Column(name = "TALUK_ID")
    private int talukId;

}
