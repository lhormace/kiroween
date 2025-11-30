# Health Chat Advisor - Project Setup

## Prerequisites

This project requires:
- Java 21 or higher (Java 25 is currently installed)
- Maven 3.8+ (installed)

## Project Structure

```
health-chat-advisor/
├── pom.xml                          # Maven configuration
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/health/chat/
│   │           ├── model/           # Data models
│   │           │   ├── HealthData.java
│   │           │   ├── NutritionInfo.java
│   │           │   ├── PFCBalance.java
│   │           │   ├── DailyNutrition.java
│   │           │   ├── MentalState.java
│   │           │   ├── EmotionalTone.java
│   │           │   ├── TankaPoem.java
│   │           │   ├── AuthResult.java
│   │           │   ├── ChatResponse.java
│   │           │   ├── UserProfile.java
│   │           │   ├── AdviceResult.java
│   │           │   ├── ResearchReference.java
│   │           │   └── TimeRange.java
│   │           ├── service/         # Service interfaces
│   │           │   ├── AuthenticationService.java
│   │           │   ├── ChatService.java
│   │           │   ├── NutritionEstimator.java
│   │           │   ├── MentalStateAnalyzer.java
│   │           │   ├── TankaGenerator.java
│   │           │   ├── HealthAdvisorAI.java
│   │           │   └── GraphGenerator.java
│   │           └── repository/      # Repository interfaces
│   │               └── DataRepository.java
│   └── test/
│       └── java/
│           └── com/health/chat/
│               └── SetupVerificationTest.java
```

## Building the Project

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package
mvn package
```

## Testing Framework

This project uses:
- **JUnit 5** for unit testing
- **jqwik 1.8.2** for property-based testing

Each property-based test should run a minimum of 100 iterations and be tagged with:
```java
/**
 * Feature: health-chat-advisor, Property {number}: {property_text}
 */
```

## Next Steps

1. Proceed with implementing the next tasks in the implementation plan
2. Task 1 (Project setup) is complete
3. Ready to implement Task 2 (S3 Data Repository)
