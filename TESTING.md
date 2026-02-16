# Testing Guide

This project includes comprehensive test coverage with unit tests, integration tests, and end-to-end (E2E) tests.

## Test Structure

```
src/test/kotlin/com/vidopi/processor/
├── service/                    # Unit tests for services
│   ├── R2ServiceTest.kt       # Tests for R2Service
│   ├── ThumbnailServiceTest.kt # Tests for ThumbnailService
│   └── MetadataServiceTest.kt  # Tests for MetadataService
├── controller/                 # Unit tests for controllers
│   └── UploadControllerTest.kt # Tests for UploadController
├── integration/               # Integration tests
│   └── UploadIntegrationTest.kt # Tests service interactions
├── e2e/                       # End-to-end tests
│   └── UploadE2ETest.kt       # Full HTTP request/response tests
├── config/                     # Test configuration
│   └── TestR2Config.kt        # Mock S3 client configuration
└── ProcessorApplicationTests.kt # Spring Boot context test
```

## Test Types

### Unit Tests

Unit tests test individual components in isolation with mocked dependencies.

**Location**: `src/test/kotlin/com/vidopi/processor/service/` and `controller/`

**Examples**:
- `R2ServiceTest`: Tests R2Service methods with mocked S3Client and S3Presigner
- `ThumbnailServiceTest`: Tests ThumbnailService with various file scenarios
- `MetadataServiceTest`: Tests metadata JSON generation
- `UploadControllerTest`: Tests controller logic with mocked services

**Key Features**:
- Fast execution (no external dependencies)
- Isolated testing of business logic
- Uses MockK for Kotlin-friendly mocking

### Integration Tests

Integration tests verify interactions between multiple components using real service instances but mocked external dependencies.

**Location**: `src/test/kotlin/com/vidopi/processor/integration/`

**Examples**:
- `UploadIntegrationTest`: Tests UploadController with real ThumbnailService and MetadataService, but mocked R2Service

**Key Features**:
- Tests component interactions
- Uses real service instances where possible
- Mocks external dependencies (R2, FFmpeg)

### End-to-End (E2E) Tests

E2E tests verify the complete HTTP request/response cycle using MockMvc.

**Location**: `src/test/kotlin/com/vidopi/processor/e2e/`

**Examples**:
- `UploadE2ETest`: Tests full HTTP POST requests to `/api/upload/video`

**Key Features**:
- Tests complete request/response cycle
- Uses Spring Boot Test with MockMvc
- Verifies JSON responses and HTTP status codes
- Uses TestR2Config to mock S3 dependencies

## Running Tests

### Run All Tests
```bash
./gradlew test
```

### Run Specific Test Types
```bash
# Unit tests only
./gradlew test --tests "*Test"

# Integration tests only
./gradlew test --tests "*IntegrationTest"

# E2E tests only
./gradlew test --tests "*E2ETest"
```

### Run Individual Test Classes
```bash
./gradlew test --tests "com.vidopi.processor.service.R2ServiceTest"
```

## Test Dependencies

The project uses the following testing libraries:

- **JUnit 5**: Test framework
- **MockK**: Kotlin mocking library
- **Spring Boot Test**: Spring Boot testing support
- **MockMvc**: Web layer testing
- **Kotlinx Coroutines Test**: Coroutine testing utilities

## Test Configuration

### TestR2Config

The `TestR2Config` class provides mocked S3Client and S3Presigner beans for integration and E2E tests. This allows testing without requiring actual Cloudflare R2 credentials.

### Test Properties

E2E tests use `@TestPropertySource` to override application properties:
- Cloudflare R2 credentials (mocked)
- Thumbnail generation settings
- Other configuration values

## Writing New Tests

### Unit Test Example
```kotlin
class MyServiceTest {
    private lateinit var dependency: Dependency
    private lateinit var myService: MyService

    @BeforeEach
    fun setUp() {
        dependency = mockk()
        myService = MyService(dependency)
    }

    @Test
    fun `should do something`() {
        // Given
        every { dependency.method() } returns "result"

        // When
        val result = myService.doSomething()

        // Then
        assertEquals("expected", result)
        verify { dependency.method() }
    }
}
```

### Integration Test Example
```kotlin
@SpringBootTest
class MyIntegrationTest {
    @Autowired
    private lateinit var myService: MyService

    @MockBean
    private lateinit var externalDependency: ExternalService

    @Test
    fun `should integrate correctly`() {
        // Test service interactions
    }
}
```

### E2E Test Example
```kotlin
@SpringBootTest
@AutoConfigureWebMvc
class MyE2ETest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `should handle HTTP request`() {
        mockMvc.perform(get("/api/endpoint"))
            .andExpect(status().isOk)
    }
}
```

## Best Practices

1. **Isolation**: Unit tests should be completely isolated with all dependencies mocked
2. **Naming**: Use descriptive test names that explain what is being tested
3. **Arrange-Act-Assert**: Structure tests with clear Given-When-Then sections
4. **Coverage**: Aim for high code coverage, especially for business logic
5. **Speed**: Keep unit tests fast; use integration/E2E tests sparingly
6. **Cleanup**: Always clean up resources (temp files, mocks) in `@AfterEach` or `finally` blocks

## Troubleshooting

### Tests Fail with S3 Connection Errors
- Ensure `TestR2Config` is imported in your test class
- Check that S3Client and S3Presigner are properly mocked

### FFmpeg Tests Fail
- ThumbnailService tests may fail if FFmpeg is not installed
- These tests are designed to handle FFmpeg failures gracefully
- Consider mocking FFmpeg execution for unit tests

### Coroutine Tests Fail
- Use `runTest` from `kotlinx.coroutines.test` for coroutine testing
- Ensure proper coroutine scope handling in tests
