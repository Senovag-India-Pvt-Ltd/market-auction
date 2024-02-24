package com.sericulture.marketandauction.model.api.marketauction.reporting;

import com.sericulture.marketandauction.model.api.ResponseBody;
import lombok.*;
import org.hibernate.tool.schema.spi.SchemaTruncator;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class FarmerTxnReportResponse extends ResponseBody {
    private String farmerFirstName;
    private String farmerMiddleName;
    private String farmerLastName;
    private String farmerNumber;
    private String village;
    private double totalSaleAmount;
    private double totalMarketFee;
    private double totalFarmerAmount;
    List<FarmerTxnInfo> farmerTxnInfoList;
}
