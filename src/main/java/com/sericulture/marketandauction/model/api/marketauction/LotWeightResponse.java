package com.sericulture.marketandauction.model.api.marketauction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sericulture.marketandauction.model.api.ResponseBody;
import lombok.*;

import java.math.BigInteger;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class LotWeightResponse extends ResponseBody {

    private String farmerNumber;

    private String farmerFruitsId;

    private String reelerLicense;

    private String farmerFirstName;

    private String farmerMiddleName;

    private String farmerLastName;

    private String reelerName;

    private float bidAmount;

    private int raceMasterId;

    private String farmerVillage;

    private double reelerCurrentBalance;

    private double blockedAmount;

    private double reelerCurrentAvailableBalance;

    @JsonIgnore
    private String reelerVirtualAccountNumber;

    @JsonIgnore
    private int reelerId;




}
