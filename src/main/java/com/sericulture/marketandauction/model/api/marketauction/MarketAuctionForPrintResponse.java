package com.sericulture.marketandauction.model.api.marketauction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sericulture.marketandauction.model.api.ResponseBody;
import lombok.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class MarketAuctionForPrintResponse extends ResponseBody {
    private String farmerNumber;
    private String farmerFirstName;
    private String farmerMiddleName;
    private String farmerLastName;
    private String farmerAddress;
    private String farmerTaluk;
    private String farmerVillage;
    private String ifscCode;
    private String accountNumber;
    private String reelerLicense;
    private String reelerName;
    private String reelerAddress;
    private int allottedLotId;
    private String auctionDate;
    private float lotWeight;
    private double farmerMarketFee;
    private double reelerMarketFee;
    private double traderMarketFee;
    private float lotSoldOutAmount;
    private float bidAmount;
    private double reelerCurrentBalance;
    private int farmerEstimatedWeight;
    private List<Float> lotWeightDetail;
    private String marketName;
    private String race;
    private String source;
    private float tareWeight;
    private String serialNumber;
    private BigDecimal marketAuctionId;
    private String marketNameKannada;
    private String farmerNameKannada;
    private String externalUnitName;
    private String externalUnitAddress;
    private String externalUnitLicenseNumber;
    private String externalUnitNumber;
    private String externalUnitOrganisationName;
    private Date auctionDateWithTime;
    private String farmerMobileNumber;
    private String reelerMobileNumber;
    private String reelerNumber;
    private String reelerNameKannada;
    private double farmerAmount;
    private double reelerAmount;
    private double traderAmount;
    private List<Integer> smallBinList;
    private List<Integer> bigBinList;
    private String loginName;
    private String fruitsId;
    private String fatherNameKan;
    private String traderFirstName;
    private String traderMiddleName;
    private String traderLastName;
    private String traderFatherName;
    private String traderAddress;
    private String traderSilkType;
    private float traderLicenseFee;
    private String traderMobileNumber;
    private String traderArnNumber;
    private String traderLicenseNumber;
    private String traderStateNameInKannada;
    private String traderDistrictNameInKannada;
    private String traderApplicationNumber;
    private String traderLicenseChallanNumber;
    private String reelerMobile;
    private String reelerBankName;
    private String reelerBranchName;
    private String reelerAccountNumber;
    private String reelerIfscCode;
    private String reelerFatherName;
    private String sadodLotNumber;
    private String godownName;




}
