package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.RequestBody;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class LotGroupageDetailsRequestEdit extends RequestBody {

    private List<LotGroupageRequestEdit> lotGroupageRequestEditList;
}
