package com.sericulture.marketandauction.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ResponseWrapper<T> {

    T content;

    List<? extends Object> errorMessages = new ArrayList<>();

    int     errorCode = 0;

    public static <T> ResponseWrapper  createWrapper(T t) {
        return new ResponseWrapper<T>();
    }

}
