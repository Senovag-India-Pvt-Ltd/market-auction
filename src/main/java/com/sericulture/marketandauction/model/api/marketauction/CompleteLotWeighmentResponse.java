package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.ResponseBody;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CompleteLotWeighmentResponse extends ResponseBody {

    private int allottedLotId;

    private double totalAmountDebited;
}
