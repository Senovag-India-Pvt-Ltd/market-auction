package com.sericulture.marketandauction.model.api;

import com.sericulture.marketandauction.model.exceptions.ValidationMessage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RequestBody implements Serializable {

    public List<ValidationMessage> validate(){
        return new ArrayList<ValidationMessage>();
    }
}
