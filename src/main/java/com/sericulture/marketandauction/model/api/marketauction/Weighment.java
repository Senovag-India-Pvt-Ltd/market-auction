package com.sericulture.marketandauction.model.api.marketauction;

import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class Weighment {

    private int crateNumber;
    float grossWeight;
    float netWeight;
}
