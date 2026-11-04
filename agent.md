# LeoNix Server Agent Guidelines

This Spring Boot application is structured according to **Feature-Driven Architecture** (Domain-Driven Packaging) with strict zero-boilerplate, logging, and security standards.

---

## 1. Zero Boilerplate Standard (Lombok)

**Rule: NEVER write manual getters, setters, verbose constructors, or toString methods.**
All entities, DTOs, and services must use Lombok annotations:

- **Entities**:
  - `@Getter`, `@Setter`
  - `@NoArgsConstructor`, `@AllArgsConstructor`
  - `@Builder`
  - `@ToString` (with `exclude = "password"` for sensitive data)
  - `@EqualsAndHashCode(of = "id")`
  - `@Builder.Default` for fields with default assignments (e.g. `createdAt`).

- **DTOs / Request / Response Payloads**:
  - `@Data` (or `@Getter` + `@Setter`)
  - `@NoArgsConstructor`, `@AllArgsConstructor`
  - `@Builder`

- **Spring Components / Services / Controllers**:
  - `@RequiredArgsConstructor` for constructor injection of `final` dependencies. Avoid `@Autowired`.

---

## 2. Standardized Logging System (`@Slf4j`)

**Rule: ALWAYS use `@Slf4j` from Lombok. Never use `System.out.println` or manual `LoggerFactory`.**

### Log Level Guidelines:
- `log.info(...)`: Key business events (e.g., user logged in, order placed, file uploaded).
- `log.debug(...)`: Internal execution details (e.g., token parsing, entity lookups).
- `log.warn(...)`: Handled anomalies or rejected requests (e.g., invalid credentials, failed validation, expired token).
- `log.error(...)`: Unexpected exceptions and server failures (always include the exception object for stack trace).

Example:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ExampleService {
    public void process(String id) {
        log.info("Processing entity: {}", id);
        // ...
        log.warn("Constraint flagged for entity: {}", id);
    }
}
```

---

## 3. JWT Authentication System

- **Token Format**: Standard JSON Web Token (HS256) signed with a secret key.
- **Header**: `Authorization: Bearer <token>`.
- **Components**:
  - `com.example.myapp.config.jwt.JwtUtils`: Generates, parses, and validates tokens with claims (`userId`, `role`, `fullName`).
  - `com.example.myapp.config.jwt.JwtAuthenticationFilter`: Extends `OncePerRequestFilter`, extracts Bearer token, populates Spring's `SecurityContextHolder`.
  - `SecurityConfig`: Stateless session policy (`SessionCreationPolicy.STATELESS`), permits `/auth/**`, `/uploads/**`, `/static/**`.

---

## 4. Static Resources & Image Upload Handling

- **Upload Endpoint**: `POST /api/uploads/image`
  - Multipart form data (`file`).
  - Allowed MIME types: `image/jpeg`, `image/png`, `image/webp`, `image/gif`, `image/svg+xml`.
  - Max file size: 10MB (configured in `application.yml`).
  - Saved to local `uploads/` folder using sanitized UUID filenames.
- **Resource Handler**:
  - `WebMvcConfig` maps `/uploads/**` directly to the physical storage folder `file:uploads/`.
  - Direct URL access: `http://localhost:8080/api/uploads/{filename}`.

---

## 5. Local Database Configuration (PostgreSQL)

- **Development Profile (`application-dev.yml`)**:
  - Database: PostgreSQL
  - Database Name: `leonix`
  - JDBC URL: `jdbc:postgresql://localhost:5432/leonix`
  - Default credentials: `username: postgres`, `password: postgres` (configurable via `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`).
  - DDL Auto: `update`

- **Testing Environment**:
  - Automated tests use isolated in-memory H2 (`src/test/resources/application.yml`) so builds run without requiring an active PostgreSQL daemon.

---

## 6. Build and Test Commands
```bash
# Build and run server
./gradlew bootRun

# Run test suite
./gradlew test
```
