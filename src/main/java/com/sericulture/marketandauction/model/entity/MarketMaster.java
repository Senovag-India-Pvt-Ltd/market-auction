package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.LocalTime;
import java.util.Date;

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
    @Column(name = "ISSUE_BID_SLIP_START_TIME")
    private LocalTime issueBidSlipStartTime;
    @Column(name = "ISSUE_BID_SLIP_END_TIME")
    private LocalTime issueBidSlipEndTime;
    @Column(name = "AUCTION_1_START_TIME")
    private LocalTime auction1StartTime;
    @Column(name = "AUCTION_2_START_TIME")
    private LocalTime auction2StartTime;
    @Column(name = "AUCTION_3_START_TIME")
    private LocalTime auction3StartTime;
    @Column(name = "AUCTION_1_END_TIME")
    private LocalTime auction1EndTime;
    @Column(name = "AUCTION_2_END_TIME")
    private LocalTime auction2EndTime;
    @Column(name = "AUCTION_3_END_TIME")
    private LocalTime auction3EndTime;
    @Column(name = "AUCTION1_ACCEPT_START_TIME")
    private LocalTime auction1AcceptStartTime;
    @Column(name = "AUCTION2_ACCEPT_START_TIME")
    private LocalTime auction2AcceptStartTime;
    @Column(name = "AUCTION3_ACCEPT_START_TIME")
    private LocalTime auction3AcceptStartTime;
    @Column(name = "AUCTION1_ACCEPT_END_TIME")
    private LocalTime auction1AcceptEndTime;
    @Column(name = "AUCTION2_ACCEPT_END_TIME")
    private LocalTime auction2AcceptEndTime;
    @Column(name = "AUCTION3_ACCEPT_END_TIME")
    private LocalTime auction3AcceptEndTime;
    @Column(name="SERIAL_NUMBER_PREFIX")
    private String serailNumberPrefix;
}