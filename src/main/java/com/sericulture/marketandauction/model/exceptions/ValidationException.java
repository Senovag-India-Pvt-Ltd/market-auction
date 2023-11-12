package com.sericulture.marketandauction.model.exceptions;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
public class ValidationException extends RuntimeException{



    private List<Message> errorMessages = new ArrayList<>();

    public ValidationException(String errorMessage) {
        errorMessages.add(new ValidationMessage(MessageLabelType.NON_LABEL_MESSAGE.name(), errorMessage));
    }

    public ValidationException(List<ValidationMessage> errorMessage) {
        errorMessages.addAll(errorMessage);
    }
    public ValidationException(Set<ValidationMessage> errorMessage) {
        errorMessages.addAll(errorMessage);
    }
    @Override
    public String getMessage() {

        return errorMessages.toString();
    }
}
