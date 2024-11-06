package com.sericulture.marketandauction.model.enums;

import lombok.Getter;

@Getter
public enum USERTYPE {

    REELER(2),

    TRADER(3),

    SEEDREELER(4),

    MO(0);

    private int type ;

     USERTYPE(int type) {
        this.type = type;
    }
}
