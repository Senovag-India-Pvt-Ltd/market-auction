package com.sericulture.marketandauction.model.api.marketauction.reporting;

import com.sericulture.marketandauction.model.api.ResponseBody;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
public class ReelerPendingInfo extends ResponseBody {
    private String reelerName;
    private String reelerNumber;
    private String reelingLicenseNumber;
    private String mobileNumber;
    private String lastTxnTime;
    private String currentBalance;
    private String serialNumber;
    private String counter;
    private String onlineTxn;
    private String suspend;
}