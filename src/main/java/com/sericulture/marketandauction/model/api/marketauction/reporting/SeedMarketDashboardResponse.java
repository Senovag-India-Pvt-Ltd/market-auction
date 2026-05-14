package com.sericulture.marketandauction.model.api.marketauction.reporting;

import com.sericulture.marketandauction.model.api.ResponseBody;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class SeedMarketDashboardResponse extends ResponseBody {

    @Schema(name = "slNo", example = "1")
    private String slNo;

    @Schema(name = "seedMarketName", example = "Bengaluru Seed Market")
    private String seedMarketName;

    @Schema(name = "seedAreaType", example = "Mysore Seed Area")
    private String seedAreaType;

    @Schema(name = "paymentMode", example = "Online")
    private String paymentMode;

    @Schema(name = "noOfLots", example = "50")
    private String noOfLots;

    @Schema(name = "totalNoOfFarmers", example = "45")
    private String totalNoOfFarmers;

    @Schema(name = "totalInwardQuantity", example = "1200.50")
    private String totalInwardQuantity;

    @Schema(name = "totalNoOfRSP", example = "10")
    private String totalNoOfRSP;

    @Schema(name = "totalRspKg", example = "500.00")
    private String totalRspKg;

    @Schema(name = "totalRspAmount", example = "50000")
    private String totalRspAmount;

    @Schema(name = "totalNoOfNSSO", example = "5")
    private String totalNoOfNSSO;

    @Schema(name = "totalNssoKg", example = "300.00")
    private String totalNssoKg;

    @Schema(name = "totalNssoAmount", example = "30000")
    private String totalNssoAmount;

    @Schema(name = "totalNoOfGovtGrainage", example = "3")
    private String totalNoOfGovtGrainage;

    @Schema(name = "totalGovtGrainageKg", example = "150.00")
    private String totalGovtGrainageKg;

    @Schema(name = "totalGovtGrainageAmount", example = "15000")
    private String totalGovtGrainageAmount;

    @Schema(name = "totalNoOfReelers", example = "20")
    private String totalNoOfReelers;

    @Schema(name = "totalReelerKg", example = "800.00")
    private String totalReelerKg;

    @Schema(name = "totalReelerAmount", example = "80000")
    private String totalReelerAmount;

    @Schema(name = "totalSeedKg", example = "950.00")
    private String totalSeedKg;

    @Schema(name = "totalSeedAmount", example = "95000")
    private String totalSeedAmount;

    @Schema(name = "totalQty", example = "1750.00")
    private String totalQty;

    @Schema(name = "totalAmount", example = "500000")
    private String totalAmount;

    @Schema(name = "paymentStatus", example = "Completed")
    private String paymentStatus;
}