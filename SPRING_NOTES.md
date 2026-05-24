# Spring Boot — Industrial API Learning Notes

This document covers every concept introduced in this project, in the order it was built,
with the reason for that order and the reason each concept exists.

---

## How to Read This Document

Each section follows this structure:

- **What it is** — the concept in plain language
- **Why now** — why this step comes before the next one
- **Why it exists** — the problem it solves
- **Where it lives in the project** — the file(s) involved

---

## 1. Spring Boot Project Setup

**What it is:**
A Spring Boot project is a Java application pre-configured to run a web server
with minimal setup. The entry point is a class annotated with `@SpringBootApplication`.

**Why now:**
Everything else depends on this. Without a running application, nothing can be tested.

**Why it exists:**
Plain Java has no built-in HTTP server. Spring Boot embeds Tomcat so you can receive
HTTP requests without external configuration.

**Where it lives:**
```
IndustrialApiApplication.java
pom.xml
```

---

## 2. Controllers — HTTP Interface Layer

**File:** `controller/HistoryController.java`

### 2.1 First Endpoint

**What it is:**
A controller is a class that receives HTTP requests and returns responses.
`@RestController` marks the class. `@GetMapping` maps a URL path to a method.

```java
@RestController
public class HistoryController {
    @GetMapping("/api/tags/history")
    public String history() {
        return "Hello";
    }
}
```

**Why now:**
The controller is the entry point to your application. You need to verify that HTTP
requests reach your code before adding any logic behind it.

**Why it exists:**
Without a controller, the application has no way to receive requests. Controllers
translate HTTP into Java method calls.

---

### 2.2 Query Parameters

**What it is:**
`@RequestParam` binds a URL parameter to a method argument.

```java
@GetMapping("/api/tags/history")
public String history(@RequestParam String tag) {
    return "History for: " + tag;
}
```

Called with: `GET /api/tags/history?tag=temperature`

**Why now:**
After a basic endpoint works, you need to accept input. Query parameters are simpler
than request bodies because there is no JSON parsing involved yet.

**Why it exists:**
APIs are useless without input. `@RequestParam` is Spring's way of extracting values
from the URL query string.

---

### 2.3 Returning Structured Data

**What it is:**
Instead of returning a String, return a Java object. Spring serializes it to JSON
automatically using Jackson.

```java
public record HistoricalPoint(Instant timestamp, float value, Quality quality) {}

@GetMapping("/api/tags/history")
public List<HistoricalPoint> history(@RequestParam String tag) {
    return List.of(new HistoricalPoint(Instant.now(), 25.5f, Quality.GOOD));
}
```

**Why now:**
Real APIs return JSON, not plain strings. This step introduces Spring's automatic
serialization before business logic adds complexity.

**Why it exists:**
Clients expect structured data. Spring (via Jackson) converts Java objects to JSON
without any manual work.

---

### 2.4 Optional and Default Parameters

**What it is:**
`@RequestParam` can be made optional or given a default value.

```java
@GetMapping("/api/tags/history")
public List<HistoricalPoint> history(
        @RequestParam String tag,
        @RequestParam(defaultValue = "10") int points,
        @RequestParam(required = false) String start,
        @RequestParam(required = false) String end,
        @RequestParam(required = false) Quality quality) {
    // ...
}
```

**Why now:**
After basic parameters work, you add flexibility. Clients should not be forced to
provide every parameter every time.

**Why it exists:**
Real APIs have optional filters. `defaultValue` prevents null checks for common
parameters. `required = false` allows parameters to be omitted entirely.

---

### 2.5 Request Body (`@RequestBody`)

**What it is:**
For POST requests, data arrives in the request body as JSON.
`@RequestBody` tells Spring to deserialize the JSON into a Java object.

```java
@PostMapping("/api/tags/data")
public HistoricalPoint writeData(@RequestBody DataPointRequest request) {
    // ...
}
```

**Why now:**
After GET endpoints work, you add write operations. POST with a body is the standard
way to send data to create resources.

**Why it exists:**
Query parameters are unsuitable for complex or sensitive data. Request bodies carry
structured payloads and are the REST standard for write operations.

---

## 3. Models — Data Structure Layer

**Files:** `model/HistoricalPoint.java`, `model/Quality.java`

### 3.1 Records

**What it is:**
A Java record is an immutable data class. It auto-generates constructor, getters,
`equals`, `hashCode`, and `toString`.

```java
public record HistoricalPoint(Instant timestamp, float value, Quality quality) {}
```

**Why now:**
Introduced alongside the first structured response. Models define the shape of data
before logic operates on it.

**Why it exists:**
Controllers and services need a shared language for data. Records are concise and
immutable, making them suitable for API responses that should not be modified after creation.

---

### 3.2 Enums

**What it is:**
An enum defines a fixed set of allowed values.

```java
public enum Quality {
    GOOD, BAD, UNCERTAIN
}
```

**Why now:**
Introduced when data quality tracking was added. Before enums, quality was a free
String — any value was accepted.

**Why it exists:**
Enums prevent invalid values at the Java level. Spring also validates enum query
parameters automatically, returning 400 for unknown values without any manual check.

---

## 4. Service Layer — Business Logic

**File:** `service/TagHistoryService.java`

### 4.1 Creating a Service

**What it is:**
A service is a class annotated with `@Service` that contains business logic.
It sits between the controller and the data source.

```java
@Service
public class TagHistoryService {
    public List<HistoricalPoint> getHistoricalData(String tagName) {
        // logic here
    }
}
```

**Why now:**
After the controller works, it contains logic that does not belong there. The service
extracts that logic into a dedicated layer.

**Why it exists:**
Controllers should only handle HTTP — receive input, return output. Business rules
belong in a service so they can be:
- Reused from multiple controllers
- Tested independently of HTTP
- Changed without touching the controller

---

### 4.2 Dependency Injection

**What it is:**
Instead of creating a service with `new`, you declare it as a dependency and Spring
provides it. This is called Dependency Injection (DI).

```java
@RestController
public class HistoryController {
    private final TagHistoryService tagHistoryService;

    // Spring calls this constructor and passes the service
    public HistoryController(TagHistoryService tagHistoryService) {
        this.tagHistoryService = tagHistoryService;
    }
}
```

**Why now:**
After the service exists, you need to connect it to the controller. DI is how Spring
wires components together.

**Why it exists:**
If you write `new TagHistoryService()` inside the controller:
- The controller controls the service's lifecycle
- You cannot replace the service with a mock for testing
- If the service needs dependencies, you manage them manually

With DI, Spring manages the lifecycle. You declare what you need, Spring provides it.

**Note on `@Autowired`:**
`@Autowired` on a field also works, but constructor injection (shown above) is preferred
because it makes dependencies explicit and the class easier to test.

---

### 4.3 Stateless Services

**What it is:**
A stateless service stores no data in its fields. Every method call receives what it
needs via parameters and returns a result without modifying shared state.

```java
@Service
public class TagHistoryService {
    // No fields storing request data

    public List<HistoricalPoint> getHistoricalData(String tagName) {
        List<HistoricalPoint> points = new ArrayList<>();
        // generate and return
        return points;
    }
}
```

**Why now:**
After DI is understood, you learn about service lifecycle. Spring services are
singletons — one instance shared across all requests.

**Why it exists:**
If a singleton stores request-specific data in a field:
- Request A writes to the field
- Request B overwrites it before A finishes reading
- Both requests get corrupted data

Stateless services avoid this entirely. Fields are only used for injected dependencies
(like repositories), never for request data.

---

### 4.4 Tag Configuration (Code Reuse)

**What it is:**
A private record inside the service that holds per-tag configuration, extracted
into a single method to avoid duplicating the switch statement.

```java
public record TagConfig(float baseValue, float range, int intervalSeconds) {}

public TagConfig getTagConfig(String tagName) {
    return switch (tagName) {
        case "temperature" -> new TagConfig(0.0f, 10.0f, 10);
        case "pressure"    -> new TagConfig(20.0f, 20.0f, 20);
        // ...
        default -> throw new TagNotFoundException(tagName);
    };
}
```

**Why now:**
Introduced when multiple methods needed to know tag parameters. Without extraction,
the switch was duplicated in each method.

**Why it exists:**
Single source of truth. If temperature's range changes, you change it in one place.
Multiple callers (`getHistoricalData`, `getLatestData`, `DataSeedService`) all use
the same configuration.

---

## 5. Validation and Error Handling

**Files:** `exception/TagNotFoundException.java`, `exception/GlobalExceptionHandler.java`,
`exception/ErrorResponse.java`

### 5.1 Input Validation

**What it is:**
Checks on method parameters that reject invalid input before processing begins.

```java
if (tagName == null || tagName.isEmpty()) {
    throw new IllegalArgumentException("Tag name cannot be null or empty");
}
if (points < 1 || points > 1000) {
    throw new IllegalArgumentException("Points must be between 1 and 1000");
}
```

**Why now:**
After services work, you add defensive checks. Validation belongs in the service,
not the controller, because the service is the entry point for all callers.

**Why it exists:**
Without validation:
- Bad input reaches the database
- Errors appear deep in the stack with unhelpful messages
- Clients cannot tell what they did wrong

---

### 5.2 Custom Exceptions

**What it is:**
A domain-specific exception that extends `RuntimeException`.

```java
public class TagNotFoundException extends RuntimeException {
    public TagNotFoundException(String tagName) {
        super("Tag not found: " + tagName);
    }
}
```

**Why now:**
After validation exists, you replace generic exceptions with domain-specific ones.
`IllegalArgumentException` does not convey intent. `TagNotFoundException` does.

**Why it exists:**
- Clarity: the exception name describes exactly what went wrong
- HTTP mapping: different exceptions can map to different status codes (404 vs 400)
- Searchability: you can find all places that throw `TagNotFoundException`

---

### 5.3 Global Exception Handler

**What it is:**
A class annotated with `@RestControllerAdvice` that intercepts exceptions thrown
anywhere in the application and converts them to HTTP responses.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TagNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTagNotFound(TagNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(404, ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(400, ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
```

**Why now:**
After custom exceptions exist, you need to translate them into proper HTTP responses.
Without this, all exceptions return 500.

**Why it exists:**
- Centralized: one place handles all errors instead of every controller
- Correct status codes: 404 for missing resources, 400 for bad input, 500 for bugs
- Consistent format: all errors return the same JSON structure

**Error response record:**
```java
public record ErrorResponse(int status, String message) {}
```

---

## 6. Database Integration

**Files:** `entity/HistoricalPointEntity.java`,
`repository/HistoricalPointRepository.java`,
`docker-compose.yml`,
`application.properties`

### 6.1 Docker and PostgreSQL

**What it is:**
Docker runs PostgreSQL in an isolated container. `docker-compose.yml` defines
the container configuration.

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: industrial_db
    environment:
      POSTGRES_USER: industrial
      POSTGRES_PASSWORD: industrial123
      POSTGRES_DB: industrial_api
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
```

**Commands:**
```bash
docker-compose up -d     # start in background
docker-compose down      # stop
docker ps                # list running containers
docker logs industrial_db  # view database logs
```

**Why now:**
Before any database code can work, the database must exist and be reachable.

**Why it exists:**
- No system-level PostgreSQL installation needed
- Same setup works on any OS
- Data persists in a Docker volume across container restarts
- Production environments also use containers

---

### 6.2 Dependencies and Configuration

**What it is:**
Two additions to `pom.xml` and connection settings in `application.properties`.

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

```properties
# application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/industrial_api
spring.datasource.username=industrial
spring.datasource.password=industrial123
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

**Why now:**
After Docker is running, you add the libraries and tell Spring where the database is.

**Why it exists:**
- `spring-boot-starter-data-jpa`: Spring's ORM layer (Hibernate under the hood)
- `postgresql`: JDBC driver that lets Java connect to PostgreSQL
- `ddl-auto=update`: auto-creates or updates tables from entity definitions
- `show-sql=true`: prints generated SQL in the console (useful for learning)

---

### 6.3 Entity Class

**File:** `entity/HistoricalPointEntity.java`

**What it is:**
A Java class mapped to a database table. JPA annotations define the mapping.

```java
@Entity
@Table(name = "historical_points")
public class HistoricalPointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tagName;

    @Column(nullable = false)
    private Instant timeStamp;

    @Column(nullable = false)
    private float value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Quality quality;

    public HistoricalPointEntity() {} // Required by JPA

    public HistoricalPointEntity(String tagName, Instant timeStamp,
                                  float value, Quality quality) {
        this.tagName = tagName;
        this.timeStamp = timeStamp;
        this.value = value;
        this.quality = quality;
    }

    // Getters and setters for every field
}
```

**Annotation reference:**

| Annotation | Meaning |
|---|---|
| `@Entity` | Marks this class as a database entity |
| `@Table(name = "...")` | Sets the table name in PostgreSQL |
| `@Id` | Marks the primary key field |
| `@GeneratedValue(strategy = GenerationType.IDENTITY)` | Auto-increment ID |
| `@Column(nullable = false)` | Column cannot be null |
| `@Enumerated(EnumType.STRING)` | Store enum as string, not integer |

**Why now:**
After configuration, you define what the table looks like. JPA reads this class
and creates the table automatically.

**Why it exists:**
JPA cannot persist a record — records are immutable and have no default constructor.
The entity is a mutable class that JPA can instantiate and populate from database rows.

**Why not use `HistoricalPoint` (the record) directly:**
- The entity has `id` and `tagName` which the API response does not expose
- Database structure and API structure should be independent
- If the database changes, the API stays stable

---

### 6.4 Repository Interface

**File:** `repository/HistoricalPointRepository.java`

**What it is:**
An interface that extends `JpaRepository`. Spring generates the implementation
automatically from method names.

```java
@Repository
public interface HistoricalPointRepository
        extends JpaRepository<HistoricalPointEntity, Long> {

    List<HistoricalPointEntity> findByTagName(String tagName);

    List<HistoricalPointEntity> findByTagNameAndTimeStampBetween(
            String tagName, Instant start, Instant end);

    HistoricalPointEntity findFirstByTagNameOrderByTimeStampDesc(String tagName);
}
```

**Method name conventions:**

| Part | Meaning |
|---|---|
| `findBy` | SELECT query |
| `TagName` | WHERE tag_name = ? |
| `And` | AND |
| `TimeStamp` | time_stamp field |
| `Between` | BETWEEN ? AND ? |
| `OrderBy...Desc` | ORDER BY ... DESC |
| `First` | LIMIT 1 |

**Why now:**
After the entity exists, you create the data access layer. The repository sits between
the service and the database.

**Why it exists:**
- Services should not write SQL directly — it is fragile and hard to test
- `JpaRepository` provides `save`, `saveAll`, `findAll`, `deleteAll`, `count` for free
- Spring generates query implementations from method names — no SQL needed for common queries

**Critical note — field name matching:**
Repository method names must match the Java field name exactly, not the database column name.

```java
// Java field: private Instant timeStamp
// Database column: time_stamp (Hibernate converts camelCase to snake_case)

// CORRECT — matches Java field
findByTagNameAndTimeStampBetween(...)

// WRONG — matches database column, not Java field
findByTagNameAndTimestampBetween(...)  // throws exception at startup
```

---

### 6.5 Entity-to-Record Conversion

**What it is:**
Private helper methods in the service that convert between the database type
(`HistoricalPointEntity`) and the API type (`HistoricalPoint`).

```java
private HistoricalPoint toRecord(HistoricalPointEntity entity) {
    return new HistoricalPoint(
        entity.getTimeStamp(),
        entity.getValue(),
        entity.getQuality()
    );
}

private List<HistoricalPoint> toRecordList(List<HistoricalPointEntity> entities) {
    return entities.stream()
        .map(this::toRecord)
        .toList();
}
```

**Why now:**
Introduced when the service started receiving entities from the repository and
needed to return records to the controller.

**Why it exists:**
- Hides database fields (`id`, `tagName`) from the API response
- Decouples database structure from API contract
- If the entity gains new fields, the API response stays the same unless explicitly updated

---

### 6.6 Transactions (`@Transactional`)

**What it is:**
`@Transactional` wraps a method in a database transaction. If anything fails,
all database operations in that method are rolled back.

```java
@Transactional
public List<HistoricalPoint> writeBatch(List<DataPointRequest> requests) {
    // If any save fails, ALL saves are rolled back
    List<HistoricalPointEntity> saved = repository.saveAll(entities);
    return toRecordList(saved);
}
```

**Why now:**
Introduced with batch write operations, where partial success is worse than total failure.

**Why it exists:**
Without transactions:
- A batch of 100 points could save 60 and fail on the 61st
- The database is left in a partial state
- You cannot know which points were saved

With `@Transactional`, all 100 succeed or none do.

---

## 7. DTOs — Request Shape Definition

**File:** `dto/DataPointRequest.java`

**What it is:**
A Data Transfer Object defines the shape of data coming into the API.

```java
public record DataPointRequest(
    String tag,
    float value,
    Quality quality
) {}
```

**Why now:**
Introduced with the write endpoint. The entity cannot be used directly as a request
body because clients do not provide `id` or `timestamp`.

**Why it exists:**
- Client sends only what it knows: tag name, value, quality
- Server generates: `id` (database), `timestamp` (current time)
- Separates "what the client sends" from "what the database stores"

---

## 8. Write Operations

### 8.1 Single Write

**What it is:**
A POST endpoint that accepts one data point and saves it to the database.

```java
// Controller
@PostMapping("/api/tags/data")
public HistoricalPoint writeData(@RequestBody DataPointRequest request) {
    return tagHistoryService.writeDataPoint(
        request.tag(), request.value(), request.quality());
}

// Service
@Transactional
public HistoricalPoint writeDataPoint(String tagName, float value, Quality quality) {
    getTagConfig(tagName);  // validates tag exists

    HistoricalPointEntity entity = new HistoricalPointEntity(
        tagName, Instant.now(), value, quality);

    HistoricalPointEntity saved = repository.save(entity);
    return toRecord(saved);
}
```

**Why now:**
After read operations work, you add writes. Single writes are simpler than batch.

**Why it exists:**
Real systems ingest data. Without write endpoints, the API is read-only and useless
without external data insertion tools.

---

### 8.2 Batch Write

**What it is:**
A POST endpoint that accepts a list of data points and saves them all in one transaction.

```java
// Controller
@PostMapping("/api/tags/batch")
public List<HistoricalPoint> writeBatch(@RequestBody List<DataPointRequest> requests) {
    return tagHistoryService.writeBatch(requests);
}

// Service
@Transactional
public List<HistoricalPoint> writeBatch(List<DataPointRequest> requests) {
    List<HistoricalPointEntity> entities = new ArrayList<>();
    Instant timestamp = Instant.now();  // all points share the same timestamp

    for (DataPointRequest request : requests) {
        getTagConfig(request.tag());
        entities.add(new HistoricalPointEntity(
            request.tag(), timestamp, request.value(), request.quality()));
    }

    return toRecordList(repository.saveAll(entities));
}
```

**Why now:**
After single writes work, you optimize for bulk. Industrial systems receive data from
many tags simultaneously.

**Why it exists:**
- `saveAll` is more efficient than calling `save` in a loop (one transaction vs many)
- All points in a batch share the same timestamp (they arrived together)
- `@Transactional` ensures either all points save or none do

---

## 9. Admin and Seeding

**Files:** `controller/AdminController.java`, `service/DataSeedService.java`

### 9.1 Data Seeding

**What it is:**
An admin endpoint that generates and inserts realistic historical data for testing.

```java
@PostMapping("/api/admin/seed")
public String seedData(
        @RequestParam String tag,
        @RequestParam(defaultValue = "7") int days,
        @RequestParam(defaultValue = "100") int pointsPerDay) {
    int total = dataSeedService.seedHistoricalData(tag, days, pointsPerDay);
    return String.format("Seeded %d points for tag '%s' over %d days", total, tag, days);
}
```

**Seeding generates realistic drift:**
```java
float drift = (float) (Math.random() - 0.5) * (config.range() / 10);
currentValue += drift;  // gradual change, not random jumps
```

**Why now:**
After database integration, the database is empty. You cannot test read operations
without data. Seeding fills the database without manual inserts.

**Why it exists:**
- Development: verifies read operations work with real data
- Testing: reproducible dataset for queries
- Realism: gradual value drift simulates actual sensors

---

### 9.2 Clear Endpoint

**What it is:**
A DELETE endpoint that removes data from the database, optionally filtered by tag.

```java
@DeleteMapping("/api/admin/clear")
public String clearData(@RequestParam(required = false) String tag) {
    int deleted = dataSeedService.clearData(tag);
    return (tag != null)
        ? String.format("Deleted %d points for tag '%s'", deleted, tag)
        : String.format("Deleted %d total points", deleted);
}
```

**Why now:**
Alongside seeding. If you seed wrong data, you need a way to clear and re-seed.

**Why it exists:**
Development utility. Keeps the database clean between test runs.

---

## Final Architecture Reference

```
src/main/java/com/victor/industrial_api/
│
├── controller/
│   ├── HistoryController.java    HTTP interface for read and write operations
│   └── AdminController.java     HTTP interface for admin operations
│
├── service/
│   ├── TagHistoryService.java   Business logic: queries, writes, validation
│   └── DataSeedService.java     Business logic: generates and inserts test data
│
├── repository/
│   └── HistoricalPointRepository.java   Database access layer
│
├── entity/
│   └── HistoricalPointEntity.java   Database table mapping
│
├── model/
│   ├── HistoricalPoint.java     API response record
│   └── Quality.java             Enum: GOOD, BAD, UNCERTAIN
│
├── dto/
│   └── DataPointRequest.java    API request record (write operations)
│
└── exception/
    ├── TagNotFoundException.java      404 — tag does not exist
    ├── GlobalExceptionHandler.java    Converts exceptions to HTTP responses
    └── ErrorResponse.java            Standard error response record
```

---

## Request Flow — Complete Example

```
GET /api/tags/history?tag=temperature&points=10&quality=GOOD
         |
         ▼
HistoryController.history()
  - Extracts: tag="temperature", points=10, quality=GOOD
  - Calls: tagHistoryService.getHistoricalData(...)
         |
         ▼
TagHistoryService.getHistoricalData()
  - Validates: tag not null/empty
  - Validates: points between 1 and 1000
  - Calls: getTagConfig("temperature") — confirms tag is known
  - Parses: start/end to Instant (defaults: now-2h to now)
  - Calls: repository.findByTagNameAndTimeStampBetween(...)
         |
         ▼
HistoricalPointRepository
  - Spring generates SQL:
    SELECT * FROM historical_points
    WHERE tag_name = 'temperature'
    AND time_stamp BETWEEN ? AND ?
  - Returns: List<HistoricalPointEntity>
         |
         ▼
TagHistoryService (continued)
  - Filters by quality: GOOD only
  - Limits to 10 points
  - Converts: List<HistoricalPointEntity> → List<HistoricalPoint>
         |
         ▼
HistoryController (continued)
  - Returns: List<HistoricalPoint>
         |
         ▼
Spring / Jackson
  - Serializes to JSON
         |
         ▼
Client receives:
[
  {"timestamp": "2026-05-21T10:00:00Z", "value": 23.5, "quality": "GOOD"},
  ...
]
```

---

## Error Flow Example

```
GET /api/tags/history?tag=invalid
         |
         ▼
TagHistoryService.getHistoricalData()
  - Calls getTagConfig("invalid")
  - switch hits default → throws TagNotFoundException("invalid")
         |
         ▼
GlobalExceptionHandler.handleTagNotFound()
  - Catches TagNotFoundException
  - Creates ErrorResponse(404, "Tag not found: invalid")
  - Returns ResponseEntity with HTTP 404
         |
         ▼
Client receives:
{"status": 404, "message": "Tag not found: invalid"}
```

---

## Key Rules to Remember

**Controllers:**
- Only handle HTTP (extract input, return output)
- No business logic
- Call services, return results

**Services:**
- Contain all business logic
- Stateless (no mutable fields for request data)
- Call repositories, return domain objects

**Repositories:**
- Only handle database access
- No business logic
- Method names must match Java field names, not database column names

**Entities vs Records:**
- Entities: mutable, for database (JPA requirement)
- Records: immutable, for API responses (clean, concise)
- Never expose entities directly in API responses

**Transactions:**
- Use `@Transactional` on any method that writes to the database
- Especially important for batch operations
- Guarantees all-or-nothing behavior
