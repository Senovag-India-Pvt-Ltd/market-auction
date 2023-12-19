package com.sericulture.marketandauction.model.api.marketauction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sericulture.marketandauction.model.api.ResponseBody;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigInteger;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class LotBidDetailResponse extends ResponseBody {

    private int allottedlotid;

    private int amount;

    private String farmerFirstName;

    private String farmerMiddleName;

    private String farmerLastName;

    private String farmerNumber;

    private String reelerName;

    private String reelerFruitsId;

    private String reelingLicenseNumber;

    @Schema(name = "reelerAuctionId", example = "1", required = true)
    private BigInteger reelerAuctionId;

    private int lotApproxWeightBeforeWeighment;

    private String farmervillageName;

    private String status;

    private String bidAcceptedBy;

}
