package com.sericulture.marketandauction.helper;

import lombok.experimental.UtilityClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Component
public final class Util {

    @Autowired
    private ResourceBundleMessageSource resourceBundleMessageSource;

   static DecimalFormat decimalFormat = new DecimalFormat("#.##");


    public String getMessageByCode(String code) {
        return resourceBundleMessageSource.getMessage(code, null, Locale.ENGLISH);
    }

    public static boolean isNullOrEmptyOrBlank(String s) {
        return (s == null || s.isBlank());
    }

    public static String objectToString(Object object) {
        return object == null ? "" : String.valueOf(object);
    }

    public static float objectToFloat(Object object) {
        return object == null ? 0 : Float.valueOf(decimalFormat.format(Float.parseFloat(String.valueOf(object)))) ;
    }

    public static int objectToInteger(Object object) {
        return object == null ? 0 :  Integer.parseInt(String.valueOf(object));
    }

    public static long objectToLong(Object object){
        return object == null ? 0 :  Long.parseLong(String.valueOf(object));
    }

    public static String getCRN(LocalDate date, int marketId, int allottedLotId) {
        String dateInString = date.toString();
        return (dateInString.replace("-", "") + String.format("%03d", marketId) + String.format("%04d", allottedLotId));
    }

    public static boolean isNullOrEmptyList(List list) {
        return list == null ? true : list.isEmpty() ? true : false;
    }

    public static boolean isNullOrEmptySet(Set set) {
        return set == null ? true : set.isEmpty() ? true : false;
    }

}
