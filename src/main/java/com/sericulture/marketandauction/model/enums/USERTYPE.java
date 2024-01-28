package com.sericulture.marketandauction.model.enums;

import lombok.Getter;

@Getter
public enum USERTYPE {

    REELER(2),

    MO(0);

    private int type ;

     USERTYPE(int type) {
        this.type = type;
    }
}
