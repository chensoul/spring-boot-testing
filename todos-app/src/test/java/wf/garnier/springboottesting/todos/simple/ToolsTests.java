package wf.garnier.springboottesting.todos.simple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static wf.garnier.springboottesting.todos.simple.Assertions.assertThat;
import static wf.garnier.springboottesting.todos.simple.Assertions.assertThatLog;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.testcontainers.shaded.org.awaitility.Awaitility;

class ToolsTests {

	@Nested
	class AssertJ {

		record Book(String title, String author, String country, int publicationYear) {
		}

		List<Book> books = List.of(new Book("The three-body problem", "Liu Cixin", "PRC", 2015),
				new Book("The Fifth Season", "N.K. Jemisin", "USA", 2015),
				new Book("The Obelisk Gate", "N.K. Jemisin", "USA", 2016),
				new Book("The Stone Skies", "N.K. Jemisin", "USA", 2017),
				new Book("The calculating stars", "Mary Robinette Kowal", "USA", 20218));

		@Test
		void example() {
			assertThat(books).hasSize(5)
				.filteredOn(book -> book.publicationYear > 2016)
				.extracting(Book::title)
				.hasSize(2)
				.containsExactlyInAnyOrder("The calculating stars", "The Stone Skies");
		}

		@Test
		void awaitility() {
			var dice = new Dice(20);
			var count = new AtomicInteger(1);

			// @formatter:off
            Awaitility.await()
                    .timeout(Duration.ofSeconds(2))
                    .pollInterval(Duration.ofMillis(10))
                    .untilAsserted(() -> {
                        var result = dice.next();
                        System.out.printf("Throw #%s - got %s%n", count.getAndIncrement(), result);
                        assertThat(result).isEqualTo(dice.maxValue());
                    });
            // @formatter:on;
		}

		@Test
		void assertions() {
			assertThatLog("🕵️ user with IP [127.0.0.1] requested [/todo.js]. We responded with [200].")
				.hasIp("127.0.0.1");
		}

		static class Dice {

			private final int sides;

			private final Random random = new Random();

			public Dice(int sides) {
				this.sides = sides;
			}

			public int next() {
				return this.random.nextInt(1, this.sides + 1);
			}

			public int maxValue() {
				return this.sides;
			}

		}

	}

	@Nested
	class Json {

		@Test
		void shouldCompareJson() throws JSONException {
			var data = getRestData();
			var expected = """
					{
					    "todos" : [
					        {
					            "name": "TEST 1",
					            "completed": true
					        },
					        {
					            "name": "TEST 2",
					            "completed": true
					        }
					    ]
					}
					""";

			// When strict is set to false (recommended), it forgives reordering data and
			// extending results
			// (as long as all the expected elements are there), making tests less
			// brittle.
			JSONAssert.assertEquals(expected, data, false);
		}

		@Test
		void shouldCompareJsonPath() {
			var json = """
					{
					    "todos" : [
					        {
					            "name": "TEST 1",
					            "completed": true
					        },
					        {
					            "name": "TEST 2",
					            "completed": true
					        }
					    ]
					}
					""";

			Integer length = JsonPath.read(json, "$.todos.length()");
			String name = JsonPath.read(json, "$.todos[1].name");
			assertEquals(2, length);
			assertEquals("TEST 2", name);
		}

		private String getRestData() {
			return """
					{
					    "todos" : [
					        {
					            "completed": true,
					            "name": "TEST 1"
					        },
					        {
					            "completed": true,
					            "name": "TEST 2"
					        }
					    ]
					}
					""";
		}

	}

}
