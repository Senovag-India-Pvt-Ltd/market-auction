package com.sericulture.marketandauction.model.exceptions;

import lombok.*;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
public class ValidationMessage extends Message{
    @NonNull
    private String label;
    private String locale;

    public ValidationMessage(String label, String message){
        super(message);
        this.label = label;
    }
}
