# Advanced Programming Backend

Backend base project for the Advanced Programming course. The current codebase is
a minimal Kotlin application built with Spring Boot and Maven.

## Tech Stack

- Kotlin 2.3.21
- Java 25
- Spring Boot 4.1.0
- Maven Wrapper 3.9.16
- JUnit 5 with Spring Boot Test

## Requirements

- JDK 25
- Bash, macOS/Linux shell, or Windows PowerShell
- Internet access on the first Maven run so dependencies can be downloaded

This repository includes `.tool-versions` for users of `asdf`:

```bash
asdf install
```

## Getting Started

Clone the repository and enter the project directory:

```bash
git clone <repository-url>
cd advanced_programming_backend
```

Start the application:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

## Code Style

This project uses [ktlint](https://pinterest.github.io/ktlint/) to enforce Kotlin code style and formatting.

### Format code

Automatically format all Kotlin source files:

```bash
./mvnw ktlint:format
```

### Check code style

Check the project without modifying files:

```bash
./mvnw ktlint:check
```

### Verify

```bash
./mvnw verify
```

## Build

Create a packaged application:

```bash
./mvnw clean package
```

Run the packaged artifact:

```bash
java -jar target/project-0.0.1-SNAPSHOT.jar
```

## Project Structure

```text
.
|-- .github/workflows/        # GitHub Actions workflows
|-- .mvn/wrapper/             # Maven Wrapper configuration
|-- src/main/kotlin/          # Application source code
|-- src/main/resources/       # Application configuration
|-- src/test/kotlin/          # Automated tests
|-- mvnw                      # Maven Wrapper for Unix-like systems
|-- mvnw.cmd                  # Maven Wrapper for Windows
`-- pom.xml                   # Maven build configuration
```

Main application entry point:

```text
src/main/kotlin/com/project/advanced/project/ProjectApplication.kt
```

Default configuration:

```properties
spring.application.name=project
```

## Testing

The project currently includes a Spring Boot context-load test:

```bash
./mvnw test
```

Expected result:

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Useful References

- [Spring Boot Maven Plugin](https://docs.spring.io/spring-boot/4.1.0/maven-plugin)
- [Apache Maven Guides](https://maven.apache.org/guides/)
- [Kotlin Maven Plugin](https://kotlinlang.org/docs/maven.html)
