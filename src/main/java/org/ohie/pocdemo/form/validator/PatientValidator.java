package org.ohie.pocdemo.form.validator;

import org.ohie.pocdemo.form.model.Patient;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class PatientValidator implements Validator {

	public boolean supports(Class<?> paramClass) {
		return Patient.class.equals(paramClass);
	}

	public void validate(Object obj, Errors errors) {
		Patient form = (Patient) obj;

	}
}
