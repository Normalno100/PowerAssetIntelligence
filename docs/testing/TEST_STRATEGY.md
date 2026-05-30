# AI Asset Platform Test Strategy

## Test layers
- **Unit tests (JUnit 5 + Mockito):** business logic, mapping, validation.
- **Integration tests (Spring + MockMvc):** API contract and service wiring.
- **Kafka tests (Spring Kafka):** producer/consumer paths and idempotency checks; prefer `@EmbeddedKafka` for fast developer feedback and reserve Testcontainers Kafka for Docker-enabled CI jobs.
- **Database tests (JPA/Flyway):** constraints, migrations, repository queries.

## Isolation strategy
- Use dedicated `test` profile and in-memory database for fast deterministic feedback.
- Reset state in each test case; avoid shared mutable static fixtures.
- Use idempotent external IDs in messaging tests.

## Test data strategy
- Keep canonical fixtures in `src/test/resources/fixtures`.
- Build complex payloads with test builders to avoid duplicated setup.
- Prefer deterministic timestamps and UUID seeds where possible.

## CI gates
- `mvn test` => unit tests
- `mvn verify -Pintegration-tests` => integration/kafka/db tests; Docker-dependent Testcontainers suites should be isolated behind an explicit CI profile or tag.
- Coverage threshold policy should be enforced in CI (Jacoco).
