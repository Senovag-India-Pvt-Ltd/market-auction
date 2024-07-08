package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.ResponseBody;
import jakarta.persistence.Column;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.*;

import java.math.BigInteger;
import java.time.LocalDate;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class FarmerReadyPaymentInfoForSeedMarketResponse extends ResponseBody {
//    private int serialNumber;
//    @Column(name = "MARKET_AUCTION_ID")
//    private Long marketAuctionId;
//    private Long allottedLotId;
//    private String lotTransactionDate;
//    private String farmerFirstName;
//    private String farmerMiddleName;
//    private String farmerLastName;
//    private String farmerNumber;
//    private String farmerMobileNumber;
//    private long lotTableId;
//    @Column(name = "LOT_GROUPAGE_ID")
//    private Long lotGroupageId;
//
//    @Column(name = "BUYER_TYPE")
//    private String buyerType;
//
//    @Column(name = "BUYER_ID")
//    private Long buyerId;
//
//    @Column(name = "LOT_WEIGHT")
//    private Long lotWeight;
//
//    @Column(name = "AMOUNT")
//    private Long amount;
//
//    @Column(name = "MARKET_FEE")
//    private Long marketFee;
//
//    @Column(name = "SOLD_AMOUNT")
//    private Long soldAmount;
//
//    @Column(name = "buyerName")
//    private String buyerName;
//
//
//    @Column(name = "LOT_ID")
//    private Long id;
//
//    @Temporal(TemporalType.DATE)
//    @Column(name = "AUCTION_DATE")
//    private LocalDate auctionDate;
private int serialNumber;
    private Long allottedLotId;
    private String auctionDate;
    private String farmerFirstName;
    private String farmerMiddleName;
    private String farmerLastName;
    private String farmerNumber;
    private String farmerMobileNumber;
    private Long lotGroupageId;
    private String buyerType;
    private Long lotWeight;
    private Long amount;
    private Long marketFee;
    private Long soldAmount;
    private String buyerName;
    private Long buyerId;
    private Long id;
    private Long farmerAmount;

}
