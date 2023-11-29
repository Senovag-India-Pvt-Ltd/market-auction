package com.sericulture.marketandauction.model.exceptions;

import lombok.*;

import java.util.Locale;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
public class ValidationMessage extends Message{
    @NonNull
    private String label;
    private String locale;

    private String errorCode;

    public ValidationMessage(String label, String message,String errorCode){
        super(message);
        this.label = label;
        this.errorCode=errorCode;
        this.locale = Locale.ENGLISH.getDisplayName();
    }
}
