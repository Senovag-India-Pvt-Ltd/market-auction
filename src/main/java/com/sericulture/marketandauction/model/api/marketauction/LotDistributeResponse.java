package com.sericulture.marketandauction.model.api.marketauction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sericulture.marketandauction.model.api.ResponseBody;
import jakarta.persistence.Column;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.*;

import java.math.BigInteger;
import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class LotDistributeResponse extends ResponseBody {
    private String farmerNumber;
    private String farmerFruitsId;
    private String farmerFirstName;
    private String farmerMiddleName;
    private String farmerLastName;
    private String fatherNameKan;

    //    private float bidAmount;
    @JsonIgnore
    private int raceMasterId;
    private String farmerVillage;

    //    @JsonIgnore
//    private int reelerId;
    private String marketName;
    private String race;
    private String source;
    private float tareWeight;
    private String lotStatus;
    //    @JsonIgnore
    @Column(name = "LOT_GROUPAGE_ID")
    private Long lotGroupageId;

    @Column(name = "BUYER_TYPE")
    private String buyerType;

    @Column(name = "BUYER_ID")
    private Long buyerId;

    @Column(name = "LOT_WEIGHT")
    private Float lotWeight;

    @Column(name = "AMOUNT")
    private Float amount;

    @Column(name = "MARKET_FEE")
    private Float marketFee;

    @Column(name = "SOLD_AMOUNT")
    private Float soldAmount;

//    @Column(name = "reelerName")
//    private String reelerName;
//
//    @Column(name = "name")
//    private String name;

    @Column(name = "buyerName")
    private String buyerName;

    @Column(name = "rspAddress")
    private String rspAddress;

    @Column(name = "ALLOTTED_LOT_ID")
    private int allottedLotId;

    @Column(name = "MARKET_AUCTION_ID")
    private BigInteger marketAuctionId;

    @Column(name = "LOT_ID")
    private BigInteger id;

    @Temporal(TemporalType.DATE)
    @Column(name = "AUCTION_DATE")
    private LocalDate auctionDate;
    private String netWeight;
    private String price;
    private String fixationDate;
    private String testDate;
    private Long noOfCocoonTakenForExamination;
    private Long noOfDFLFromFc;
    private Long noOfCocoonPerKg;
    private Long totalCocoonCount;
    private String noOfCocoonExamined;
    private String pupaCocoonStatus;
    private String marketAuctionDate;
    private String meltPercentage;
    private String dflLotNumber;
    private String lotParentLevel;
    private Float initialWeighment;
    private String averageYield;
    private String noOfDFLs;
    private String invoiceNumber;
    private String calculatedAverageYield;
    private String remainingCocoonWeight;
    private String soldCocoonInKgs;
    private String lotWeightAfterWeighment;
    private int serialNumber;
    private String farmerFullName;
    private Float totalNumber;
    private Float totalLotWeight;
    private Float totalSoldOutAmount;
    private String licenseNo;
    private String spunFromDate;
    private String spunToDate;


    private Float sumLotWeightRspNssoGovt;
    private Float sumSoldAmountRspNssoGovt;
    private Float sumLotWeightReeling;
    private Float sumSoldAmountReeling;



    private String lotNumber;

    private Long numberOfDflsDisposed;

    private String spunDate;

    private String noOfChandies;

    private String expectedCocoon;

    private String farmerNameKan;


    private String villageName;

    private Long fitnessCertificateId;

    private String tscName;
    private Long cropStatusId;
    private String cropStatusName;
    private String raceName;
    private String transactionDate;

}
