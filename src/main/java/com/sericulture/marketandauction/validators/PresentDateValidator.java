package com.sericulture.marketandauction.validators;

import com.sericulture.marketandauction.helper.Util;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class PresentDateValidator implements ConstraintValidator<PresentDate, LocalDate> {
  @Override
  public boolean isValid(final LocalDate valueToValidate, final ConstraintValidatorContext context) {
    return  Util.getISTLocalDate().equals(valueToValidate);
  }
}
