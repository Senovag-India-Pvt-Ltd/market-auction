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
public class FlexTime  extends BaseEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "FLEX_TIME_SEQ")
    @SequenceGenerator(name = "FLEX_TIME_SEQ", sequenceName = "FLEX_TIME_SEQ", allocationSize = 1)
    @Column(name = "FLEX_TIME_ID")
    private int id;

    @Column(name = "ACTIVITY_TYPE")
    private String activityType;

    @Column(name = "FLEX_TIME_START")
    private boolean start;

    @Column(name = "MARKET_ID")
    private int marketId;

    @Column(name = "GODOWN_ID")
    private int godownId;

}
