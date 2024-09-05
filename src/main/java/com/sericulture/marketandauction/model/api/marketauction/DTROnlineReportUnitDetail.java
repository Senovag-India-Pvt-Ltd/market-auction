package com.sericulture.marketandauction.model.api.marketauction;

import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class DTROnlineReportUnitDetail {
    private int serialNumber;
    private int allottedLotId;
    private String farmerFirstName;
    private String farmerMiddleName;
    private String farmerLastName;
    private String farmerNumber;
    private String farmerMobileNumber;
    private String farmerAddress;
    private float weight;
    private int bidAmount;
//    private double farmerAmount;
//    private double reelerAmount;
    private float farmerAmount;
    private float reelerAmount;
    private float lotSoldOutAmount;
//    private double farmerMarketFee;
//    private double reelerMarketFee;
    private float farmerMarketFee;
    private float reelerMarketFee;
    private String reelerLicense;
    private String reelerName;
    private String reelerMobile;
    private String bankName;
    private String branchName;
    private String ifscCode;
    private String accountNumber;
    private LocalDate auctionDate;
    private String farmerTaluk;
    private String farmerVillage;
    private String raceName;
    private Long cocoonAge;
    private Long maxAmount;
    private Long minAmount;
    private float avgAmount;

}
