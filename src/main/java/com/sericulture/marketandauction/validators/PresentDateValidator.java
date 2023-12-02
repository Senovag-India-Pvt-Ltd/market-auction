package com.sericulture.marketandauction.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class PresentDateValidator implements ConstraintValidator<PresentDate, LocalDate> {
  @Override
  public boolean isValid(final LocalDate valueToValidate, final ConstraintValidatorContext context) {
    return  LocalDate.now().equals(valueToValidate);
  }
}
