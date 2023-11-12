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
public class GodownMaster extends BaseEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GODOWN_MASTER_SEQ")
    @SequenceGenerator(name = "GODOWN_MASTER_SEQ", sequenceName = "GODOWN_MASTER_SEQ", allocationSize = 1)
    @Column(name = "GODOWN_MASTER_ID")
    private int id;

    @Size(message = "name of the market")
    @Column(name = "GODOWN_NAME",unique = true)
    private String name;

    @Column(name = "MARKET_ID")
    private int marketId;

}
