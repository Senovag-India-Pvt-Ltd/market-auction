package com.sericulture.marketandauction.model.api.marketauction.reporting;

import com.sericulture.marketandauction.model.api.ResponseBody;
import lombok.*;

import java.time.LocalTime;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class LotReportResponse extends ResponseBody {
    private int lotId;
    private String reelerLicenseNumber;
    private int bidAmount;
    private LocalTime bidTime;
    private String accepted;
    private int auctionNumber;
    private LocalTime acceptedTime;
    private String acceptedBy;
    private String marketName;
    private String auctionSession;
    private int serialNumber;

    private long serailNumberForPagination;

}
