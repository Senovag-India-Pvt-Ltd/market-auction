package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ExceptionalTime  extends BaseEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "EXCEPTIONAL_TIME_SEQ")
    @SequenceGenerator(name = "EXCEPTIONAL_TIME_SEQ", sequenceName = "EXCEPTIONAL_TIME_SEQ", allocationSize = 1)
    @Column(name = "EXCEPTIONAL_TIME_ID")
    private int id;
    @Column(name = "MARKET_ID")
    private int marketId;
    @Column(name = "GODOWN_ID")
    private int godownId;
    @Temporal(TemporalType.DATE)
    @Column(name = "AUCTION_DATE")
    private LocalDate auctionDate;
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
}
