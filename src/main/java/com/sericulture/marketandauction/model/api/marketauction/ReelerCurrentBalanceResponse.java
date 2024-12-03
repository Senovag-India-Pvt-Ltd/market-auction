package com.sericulture.marketandauction.model.api.marketauction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sericulture.marketandauction.model.api.RequestBody;
import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class ReelerCurrentBalanceResponse extends RequestBody {
    private String slNo;

    private int reelerId;

    private String reelerLicenseNumber;

    private String name;

    private String reelerVirtualAccount;

    private String mobileNumber;

    private float balance;

    private LocalDate updatedDate;

    private String total;
}
