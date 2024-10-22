package wf.garnier.springboottesting.todos.simple.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegExConstraintValidator implements ConstraintValidator<RegEx, String> {

	@Override
	public boolean isValid(String regEx, ConstraintValidatorContext context) {
		if (regEx == null) {
			return true;
		}

		if (regEx.isEmpty()) {
			return false;
		}

		try {
			Pattern.compile(regEx);
		}
		catch (PatternSyntaxException e) {
			return false;
		}
		return true;
	}

}
