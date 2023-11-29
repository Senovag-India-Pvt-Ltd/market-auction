package com.sericulture.marketandauction.helper;

import lombok.experimental.UtilityClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public final class Util {

    @Autowired
    private ResourceBundleMessageSource resourceBundleMessageSource;


    public  String getMessageByCode(String code){
        return resourceBundleMessageSource.getMessage(code, null, Locale.ENGLISH);
    }
}
