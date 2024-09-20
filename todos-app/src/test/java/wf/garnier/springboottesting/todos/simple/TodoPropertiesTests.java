package wf.garnier.springboottesting.todos.simple;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.TestPropertySource;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static wf.garnier.springboottesting.todos.simple.validation.ValidationResultAssert.assertThat;

@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TodoPropertiesTests {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Nested
	@Order(1)
	class ManualBeanBuilding {

		@Test
		public void empty() {
			var empty = new TodoProperties(List.of(), null);
			assertThat(validator.validate(empty)).isEmpty();
		}

		@Test
		public void simple() {
			//@formatter:off
			var complex = new TodoProperties(
					List.of(
							new TodoProperties.UserProfile(
									"internal",
									new TodoProperties.UserProfile.InternalUser(
											"      ",
											null,
											"Daniel",
											"Garnier-Moiroux",
											null
									),
									null
							)
					),
					null);
			//@formatter:on

			// Need the full Spring mechanisms
			assertThat(validator.validate(complex)).hasSize(2)
				.hasViolationForProperty("profiles[0].internalUser.email")
				.hasViolationForProperties("profiles[0].internalUser.password");
		}

		@Test
		public void complex() {
			//@formatter:off
			var complex = new TodoProperties(
					List.of(
							new TodoProperties.UserProfile(
									"internal",
									new TodoProperties.UserProfile.InternalUser(
											"git@garnier.wf",
											"some-password",
											"Daniel",
											"Garnier-Moiroux",
											null
									),
									null
							),
							new TodoProperties.UserProfile(
									"internal",
									null,
									new TodoProperties.UserProfile.Github("kehrlann")
							)

					),
					null);
			//@formatter:on
			assertThat(validator.validate(complex)).hasViolationForProperty("profiles",
					"List items should have distinct \"name\" properties. Found duplicates: [internal].");
		}

	}

	@Nested
	@Order(2)
	class Jackson {

		ObjectMapper mapper = YAMLMapper.builder()
			.propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
			.configure(JsonParser.Feature.IGNORE_UNDEFINED, true)
			.addModule(new ParameterNamesModule())
			.build();

		@Test
		public void empty() throws JsonProcessingException {
			var empty = mapper.readValue("""
					profiles: []
					""", TodoProperties.class);
			assertThat(validator.validate(empty)).isEmpty();
		}

		@Test
		public void simple() throws JsonProcessingException {
			var props = mapper.readValue("""
					profiles:
					- name: internal
					  internal-user:
					    email: git@garnier.wf
					""", TodoProperties.class);
			//@formatter:on
			assertThat(validator.validate(props)).hasViolationForProperties("profiles[0].internalUser.firstName",
					"profiles[0].internalUser.lastName");
		}

		@Test
		public void complex() throws JsonProcessingException {
			var props = mapper.readValue("""
					profiles:
					- name: internal
					  internal-user:
					    email: git@garnier.wf
					    password: some-password
					    first-name: Daniel
					    last-name: Garnier-Moiroux
					- name: internal
					  github:
					    id: kerhlann
					""", TodoProperties.class);
			//@formatter:on
			assertThat(validator.validate(props)).hasViolationForProperty("profiles",
					"List items should have distinct \"name\" properties. Found duplicates: [internal].");
		}

	}

	@Nested
	@Order(3)
	class IntegrationTest {

		@Test
		void validProperties() throws IOException {
			var config = new ByteArrayResource("""
					todo:
					  profiles:
					  - name: xxx
					    internal-user:
					      email: git@garnier.wf
					      password: some-password
					      first-name: Daniel
					      last-name: Garnier-Moiroux
					""".getBytes(StandardCharsets.UTF_8));

			List<PropertySource<?>> propertySources = new YamlPropertySourceLoader().load("env-from-inline-test",
					config);
			var env = new StandardEnvironment();
			env.getPropertySources().addFirst(propertySources.get(0));

			var app = new SpringApplicationBuilder(PropertiesLoader.class).web(WebApplicationType.NONE)
				.environment(env);
			assertThatNoException().isThrownBy(app::run);
			// assertThatExceptionOfType(ConfigurationPropertiesBindException.class).isThrownBy(app::run);
		}

		@Test
		void invalidProperties() throws IOException {
			var config = new ByteArrayResource("""
					todo:
					  profiles:
					  - name: "  "
					    internal-user:
					      email: git@garnier.wf
					""".getBytes(StandardCharsets.UTF_8));

			List<PropertySource<?>> propertySources = new YamlPropertySourceLoader().load("env-from-inline-test",
					config);
			var env = new StandardEnvironment();
			env.getPropertySources().addFirst(propertySources.get(0));

			var app = new SpringApplicationBuilder(PropertiesLoader.class).web(WebApplicationType.NONE)
				.environment(env);
			assertThatExceptionOfType(ConfigurationPropertiesBindException.class).isThrownBy(app::run);
		}

		@Configuration
		@EnableConfigurationProperties(TodoProperties.class)
		static class PropertiesLoader {

		}

	}

	@Nested
	@SpringBootTest(classes = IntegrationTest.PropertiesLoader.class)
	@TestPropertySource(properties = """
			todo.profiles[0].name = internal
			todo.profiles[0].internal-user:
			todo.profiles[0].internal-user.email: git@garnier.wf
			todo.profiles[0].internal-user.password: some-password
			todo.profiles[0].internal-user.first-name: Daniel
			todo.profiles[0].internal-user.last-name: Garnier-Moiroux
			""")
	@Order(4)
	class SpringBoot {

		@Test
		void loads() {
			// This is slower, ~600ms
		}

	}

}
