package com.sericulture.marketandauction.helper;

import com.sericulture.authentication.model.JwtPayloadData;
import com.sericulture.authentication.service.UserInfoDetails;
import com.sericulture.authentication.utils.TokenDecrypterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.text.DecimalFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public final class Util {

    @Autowired
    private ResourceBundleMessageSource resourceBundleMessageSource;

    static DecimalFormat decimalFormat = new DecimalFormat("#.##");

    @Autowired
    ApplicationContext applicationContext;


    public String getMessageByCode(String code) {
        return resourceBundleMessageSource.getMessage(code, null, Locale.ENGLISH);
    }

    public static boolean isNullOrEmptyOrBlank(String s) {
        return (s == null || s.isBlank());
    }

    public static String objectToString(Object object) {
        return object == null ? "" : String.valueOf(object);
    }


    public static Float formatToTwoDecimalPlaces(Object value) {
        if (value == null) return null;
        try {
            return new BigDecimal(value.toString())
                    .setScale(2, RoundingMode.HALF_UP)
                    .floatValue();
        } catch (NumberFormatException e) {
            return null;
        }
    }
    public static LocalDate objectToDate(Object date) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        } else if (date instanceof java.util.Date) {
            return ((java.util.Date) date).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } else if (date instanceof LocalDate) {
            return (LocalDate) date;
        } else if (date instanceof String) {
            return LocalDate.parse(((String) date).split(" ")[0], DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } else {
            throw new IllegalArgumentException("Unsupported date type");
        }
    }

    public static String objectToDateTime(Object date) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } else if (date instanceof java.util.Date) {
            return (((java.util.Date) date).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } else if (date instanceof LocalDate) {
            return ((LocalDate) date).atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } else if (date instanceof String) {
            return LocalDateTime.parse((String) date, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } else {
            throw new IllegalArgumentException("Unsupported date type");
        }
    }

    public static float objectToFloat(Object object) {
        if (object == null) return 0;
        try {
            return new BigDecimal(String.valueOf(object))
                    .setScale(2, RoundingMode.HALF_UP)
                    .floatValue();
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static int objectToInteger(Object object) {
        return object == null ? 0 : Integer.parseInt(String.valueOf(object));
    }

    public static long objectToLong(Object obj) {
        if (obj == null) return 0L;

        // ✅ BEST way (handles 90, 90.00, BigDecimal, etc.)
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }

        try {
            return (long) Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return 0L;
        }

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

    public static LocalDate getISTLocalDate() {
        LocalDateTime l = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        return l.toLocalDate();
    }

    public static Date getISTDate() {
        ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        return Date.from(zonedDateTime.toInstant());
    }

    public static LocalTime getISTLocalTime() {
        LocalDateTime l = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        return l.toLocalTime();
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static JwtPayloadData getTokenValues() {
        Principal principal = SecurityContextHolder.getContext().getAuthentication();
        String token = ((UserInfoDetails)((UsernamePasswordAuthenticationToken) principal).getPrincipal()).getJwtToken();
        return TokenDecrypterUtil.extractJwtPayload(token);
    }
    public static Integer getMarketId(JwtPayloadData jwtPayloadData) {
        return jwtPayloadData.getMarketId();
    }
    public static Integer getGodownId(JwtPayloadData jwtPayloadData) {
        return jwtPayloadData.getGodownId();
    }
    public static Integer getUserType(JwtPayloadData jwtPayloadData) {
        return jwtPayloadData.getUserType();
    }
    public static String getUserId(JwtPayloadData jwtPayloadData) {
        return jwtPayloadData.getUsername();
    }

    public static Long getUserMasterId(JwtPayloadData jwtPayloadData) {
        return jwtPayloadData.getUserMasterId();
    }

    public static <T> T castObjectToTypeValue(Object value, Class<T> clazz) {
        return value == null ? null : clazz.cast(value);
    }

    public static String returnPassedValueIfNullOrEmpty(String value,String defaultValue) {
        return (Objects.isNull(value) || "".equals(value)) ? defaultValue : (String)value;
    }
}
