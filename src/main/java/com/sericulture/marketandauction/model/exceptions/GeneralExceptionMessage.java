package com.sericulture.marketandauction.model.exceptions;


import lombok.*;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
public class GeneralExceptionMessage extends Message{
    @NonNull
    private String label;
    private String locale;

    private String reason;

    private String errorCode;

    public GeneralExceptionMessage(String label, String message, String errorCode, String locale, String reason){
        super(message);
        this.label = label;
        this.reason = reason;
        this.locale = locale;
        this.errorCode = errorCode;
    }
}
