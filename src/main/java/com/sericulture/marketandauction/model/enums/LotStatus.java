package com.sericulture.marketandauction.model.enums;

import lombok.Getter;

@Getter
public enum LotStatus {

    ACCEPTED("accepted"),
    REJECTED("rejected"),
    WEIGHMENTCOMPLETED("weighmentcompleted"),
    READYFORPAYMENT("readyforpayment"),
    PAYMENTSUCCESS("paymentsuccess"),
    PAYMENTFAILED("paymentfailed"),
    PAYMENTVALIDATIONFAILED("paymentvalidationfailed"),
    PAYMENTPROCESSING("paymentprocessing"),
    CANCELLED("cancelled"),
    INUPDATEWEIGHT("inupdateweight");

    private String label ;

    LotStatus(String label) {
        this.label = label;
    }
}
