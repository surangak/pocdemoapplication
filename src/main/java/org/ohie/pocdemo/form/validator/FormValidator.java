package org.ohie.pocdemo.form.validator;

import java.util.List;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import org.ohie.pocdemo.form.model.AphpDocument;

public class FormValidator implements Validator {

	public boolean supports(Class<?> paramClass) {
		return AphpDocument.class.equals(paramClass);
	}

	public void validate(Object obj, Errors errors) {
		AphpDocument form = (AphpDocument) obj;

	}
}
