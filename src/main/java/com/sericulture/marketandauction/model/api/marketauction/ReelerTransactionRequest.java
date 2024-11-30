package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.RequestBody;
import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode

public class ReelerTransactionRequest extends RequestBody {
    private String reelerLicenceNumber;
    private String mobileNumber;
    private LocalDate transactionDate;
}
