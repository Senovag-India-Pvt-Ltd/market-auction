package com.sericulture.marketandauction.model.api.marketauction;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class FarmerPaymentInfoRequestByLotList extends FarmerPaymentInfoRequest{

    private List<Long> allottedLotList;
}
