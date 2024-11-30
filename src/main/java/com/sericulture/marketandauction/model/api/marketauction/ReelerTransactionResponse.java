package com.sericulture.marketandauction.model.api.marketauction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sericulture.marketandauction.model.api.RequestBody;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class ReelerTransactionResponse extends RequestBody {
    private String slNo;

    private int reelerId;

    private LocalDate postingDate;

    private String reelerLicenseNumber;

    private String name;

    private String remitterName;

    private String remitterAccount;

    private String remitterBank;

    private String sqNo;

    private String refNo;

    private String reelerVirtualAccount;

    private float amount;

    private String updatedDateTime;

    private String mobileNumber;

    private String total;
}
