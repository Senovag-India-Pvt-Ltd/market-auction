package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.ResponseBody;
import lombok.*;

import java.math.BigInteger;
import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class FarmerPaymentInfoResponse extends ResponseBody {

    private int serialNumber;

    private int allottedLotId;

    private String lotTransactionDate;

    private String farmerFirstName;

    private String farmerMiddleName;

    private String farmerLastName;

    private String farmerNumber;

    private String farmerMobileNumber;

    private String reelerLicense;

    private String bankName;

    private String branchName;

    private String ifscCode;

    private String accountNumber;

    private float lotSoldOutAmount;

    private double farmerMarketFee;

    private double reelerCurrentBalance;

    private long lotTableId;

}
