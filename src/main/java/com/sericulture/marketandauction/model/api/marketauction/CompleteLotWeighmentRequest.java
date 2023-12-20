package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.ResponseBody;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CompleteLotWeighmentRequest extends LotStatusRequest {

    List<Weighment> weighmentList;

    private String userName;


}

