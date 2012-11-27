package org.hibernate.validator.test.internal.engine.groups.conversion;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConvertGroup;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.groups.Default;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.hibernate.validator.testutil.ConstraintViolationAssert.assertCorrectPropertyPaths;
import static org.hibernate.validator.testutil.ValidatorUtil.getValidator;

/**
 * Integrative test for group conversion.
 *
 * @author Gunnar Morling
 */
public class GroupConversionTest {

	private Validator validator;

	@BeforeMethod
	public void setupValidator() {
		validator = getValidator();
	}

	@Test
	public void groupConversionOnField() {
		Set<ConstraintViolation<User1>> violations = validator.validate( new User1() );
		assertCorrectPropertyPaths( violations, "addresses[].street1", "addresses[].zipCode" );
	}

	@Test
	public void groupConversionOnGetter() {
		Set<ConstraintViolation<User2>> violations = validator.validate( new User2() );
		assertCorrectPropertyPaths( violations, "addresses[].street1", "addresses[].zipCode" );
	}

	@Test
	public void groupConversionOnParameter() throws Exception {

		Set<ConstraintViolation<User6>> violations = getValidator().forMethods().validateParameters(
				new User6(),
				User6.class.getMethod( "setAddresses", List.class ),
				new List<?>[] { Arrays.asList( new Address() ) }
		);

		assertCorrectPropertyPaths( violations, "setAddresses.arg0[0].street1", "setAddresses.arg0[0].zipCode" );
	}

	@Test
	public void groupConversionOnReturnValue() throws Exception {

		Set<ConstraintViolation<User7>> violations = getValidator().forMethods().validateReturnValue(
				new User7(),
				User7.class.getMethod( "findAddresses" ),
				Arrays.asList( new Address() )
		);

		assertCorrectPropertyPaths(
				violations,
				"findAddresses.$retval[0].street1",
				"findAddresses.$retval[0].zipCode"
		);
	}

	@Test
	public void multipleGroupConversionsOnField() {
		Set<ConstraintViolation<User3>> violations = validator.validate( new User3() );
		assertCorrectPropertyPaths( violations, "addresses[].street1", "addresses[].zipCode" );

		violations = validator.validate( new User3(), Complete.class );
		for ( ConstraintViolation<User3> constraintViolation : violations ) {
			System.out.println( constraintViolation.getPropertyPath() );
		}
		assertCorrectPropertyPaths( violations, "addresses[].doorCode", "addresses[].street1", "addresses[].zipCode" );
	}

	@Test
	public void multipleGroupConversionsOnGetter() {
		Set<ConstraintViolation<User4>> violations = validator.validate( new User4() );
		assertCorrectPropertyPaths( violations, "addresses[].street1", "addresses[].zipCode" );

		violations = validator.validate( new User4(), Complete.class );
		assertCorrectPropertyPaths( violations, "addresses[].doorCode", "addresses[].street1", "addresses[].zipCode" );
	}

	@Test
	public void conversionIsEvaluatedPerProperty() {
		Set<ConstraintViolation<User5>> violations = validator.validate( new User5() );
		assertCorrectPropertyPaths( violations, "addresses[].street1", "addresses[].zipCode", "phoneNumber.number" );
	}


	public interface Complete extends Default {
	}

	public interface BasicPostal {
	}

	public interface FullPostal extends BasicPostal {
	}

	private interface BasicNumber {
	}

	private static class Address {
		@NotNull(groups = BasicPostal.class)
		String street1;

		@NotNull
		String street2;

		@Size(groups = BasicPostal.class, min = 3)
		String zipCode = "12";

		@Size(groups = FullPostal.class, max = 2)
		String doorCode = "ABC";
	}

	private static class PhoneNumber {

		@NotNull(groups = BasicNumber.class)
		String number;
	}

	private static class User1 {

		@Valid
		@ConvertGroup(from = Default.class, to = BasicPostal.class)
		private final Set<Address> addresses = asSet( new Address() );

		public Set<Address> getAddresses() {
			return addresses;
		}
	}

	private static class User2 {

		private final Set<Address> addresses = asSet( new Address() );

		@Valid
		@ConvertGroup(from = Default.class, to = BasicPostal.class)
		public Set<Address> getAddresses() {
			return addresses;
		}
	}

	private static class User3 {

		private final Set<Address> addresses = asSet( new Address() );

		@Valid
		@ConvertGroup.List({
				@ConvertGroup(from = Default.class, to = BasicPostal.class),
				@ConvertGroup(from = Complete.class, to = FullPostal.class)
		})
		public Set<Address> getAddresses() {
			return addresses;
		}
	}

	private static class User4 {

		private final Set<Address> addresses = asSet( new Address() );

		@Valid
		@ConvertGroup.List({
				@ConvertGroup(from = Default.class, to = BasicPostal.class),
				@ConvertGroup(from = Complete.class, to = FullPostal.class)
		})
		public Set<Address> getAddresses() {
			return addresses;
		}
	}

	private static class User5 {

		@Valid
		@ConvertGroup(from = Default.class, to = BasicPostal.class)
		public Set<Address> getAddresses() {
			return asSet( new Address() );
		}

		@Valid
		@ConvertGroup(from = Default.class, to = BasicNumber.class)
		public PhoneNumber getPhoneNumber() {
			return new PhoneNumber();
		}
	}

	//TODO HV-638: Fails when using j.u.Set as parameter type
	private static class User6 {

		public void setAddresses(
				@Valid
				@ConvertGroup(from = Default.class, to = BasicPostal.class)
				List<Address> addresses) {
		}
	}

	private static class User7 {

		private final List<Address> addresses = Arrays.asList( new Address() );

		@Valid
		@ConvertGroup(from = Default.class, to = BasicPostal.class)
		public List<Address> findAddresses() {
			return addresses;
		}
	}
}
