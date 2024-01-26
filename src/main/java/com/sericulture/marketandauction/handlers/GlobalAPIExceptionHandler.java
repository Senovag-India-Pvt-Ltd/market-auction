package com.sericulture.marketandauction.handlers;

import com.sericulture.marketandauction.helper.MAConstants;
import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.ErrorResponse;
import com.sericulture.marketandauction.model.api.ErrorType;
import com.sericulture.marketandauction.model.exceptions.GeneralExceptionMessage;
import com.sericulture.marketandauction.model.exceptions.MessageLabelType;
import com.sericulture.marketandauction.model.exceptions.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;


@ControllerAdvice
public class GlobalAPIExceptionHandler {

    @Autowired
    Util util;
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity handleEmployeeNotFound(
            ValidationException exception
    ) {
        List<ErrorResponse> errorResponses = Arrays.asList(new ErrorResponse(exception.getErrorMessages(), ErrorType.VALIDATION));
        ResponseWrapper wr = new ResponseWrapper();
        wr.setErrorMessages(errorResponses);
        wr.setErrorCode(-1);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(wr);
    }

    @ExceptionHandler({Exception.class, Throwable.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity handleInternalServerError(
            Throwable exception
    ) {
        ResponseWrapper wr = new ResponseWrapper();
        GeneralExceptionMessage gem = new GeneralExceptionMessage(MessageLabelType.SYSTEM.name(),
                util.getMessageByCode("MA00002.GEN"),
                "MA00002.GEN",
                Locale.ENGLISH.getDisplayName(),

                exception.getMessage());
        wr.setErrorMessages(Arrays.asList(new ErrorResponse(Arrays.asList(gem), ErrorType.INTERNAL_SERVER_ERROR)));
        wr.setErrorCode(-1);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(wr);
    }
}