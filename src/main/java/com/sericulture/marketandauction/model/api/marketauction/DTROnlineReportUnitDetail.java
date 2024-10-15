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
    private String reelerAddress;
    private String reelerBankName;
    private String reelerBranchName;
    private String reelerIfscCode;
    private String reelerNumber;
    private String traderFirstName;
    private String traderMiddleName;
    private String traderLastName;
    private String traderFatherName;
    private String traderAddress;
    private String traderSilkType;
    private Long traderMarketMasterId;
    private String traderMobileNumber;
    private String traderArnNumber;
    private String traderLicenseNumber;
    private String bankName;
    private String branchName;
    private String ifscCode;
    private String accountNumber;
    private LocalDate auctionDate;
    private String farmerTaluk;
    private String farmerVillage;
    private String raceName;
    private Long cocoonAge;
    private String farmerNameKannada;
    private String fatherNameKannada;
    private String talukNameInKannada;
    private String villageNameInKannada;
    private Long maxAmount;
    private Long minAmount;
    private float avgAmount;

}
