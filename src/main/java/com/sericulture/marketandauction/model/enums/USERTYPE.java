package com.sericulture.marketandauction.model.enums;

import lombok.Getter;

@Getter
public enum USERTYPE {

    REELER(2);

    private int type ;

     USERTYPE(int type) {
        this.type = type;
    }
}
