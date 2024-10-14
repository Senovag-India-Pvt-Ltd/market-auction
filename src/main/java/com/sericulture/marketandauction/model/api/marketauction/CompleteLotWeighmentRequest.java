package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.ResponseBody;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class CompleteLotWeighmentRequest extends LotStatusRequest {

    List<Weighment> weighmentList;

    private String userName;

    private LocalDate auctionDate;


}

