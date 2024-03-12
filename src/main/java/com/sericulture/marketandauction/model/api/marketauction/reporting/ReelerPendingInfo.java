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
    private String marketName;
    private String currentBalance;
    private String totalAmount;
}