package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CrateMaster extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CRATE_MASTER_SEQ")
    @SequenceGenerator(name = "CRATE_MASTER_SEQ", sequenceName = "CRATE_MASTER_SEQ", allocationSize = 1)
    @Column(name = "CRATE_MASTER_ID")
    private int id;

    @Column(name="RACE")
    private String race;

    @Column(name = "MARKET_ID")
    private int marketId;

    @Column(name = "GODOWN_ID")
    private int godownId;

    @Column(name = "APPROX_WEIGHT_PER_CRATE")
    private int approxWeightPerCrate;

}
