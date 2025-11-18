# Event Reader System

This is a Spring Boot application that processes insurance product data from XML files and provides a REST API for querying the processed information.

## Features

- Scheduled file processing: Reads XML request files from the `input` directory every 10 minutes.
- Data storage: Stores processed data in an in-memory H2 database.
- File management: Moves processed files to the `backup` directory.
- REST API: Provides endpoints to retrieve products grouped by SourceCompany for a given InsuredId.

## Prerequisites

- Java 17 or higher
- Gradle (or use the included Gradle wrapper)

## Configuration

The application can be configured via `application.properties` or environment variables:

- `app.input-dir`: Directory to read input files from (default: `input`)
- `app.backup-dir`: Directory to move processed files to (default: `backup`)
- Database: Uses H2 in-memory database by default.

## Running Locally

1. Clone the repository.
2. Navigate to the project root directory.
3. Run the application using Gradle:

   ```bash
   ./gradlew bootRun
   ```

   Or on Windows:

   ```cmd
   gradlew.bat bootRun
   ```

4. The application will start on port 8080.

## Running with Docker

1. Ensure Docker and Docker Compose are installed.
2. From the project root, run:

   ```bash
   docker-compose up --build
   ```

3. The application will be available on port 8080.

Note: The Docker setup includes a PostgreSQL database, but the application is configured to use H2. Adjust the configuration if needed.

## API Endpoints

### Get Products by InsuredId

- **URL**: `/api/products/{insuredId}`
- **Method**: GET
- **Response**: JSON object with products grouped by SourceCompany.

Example response:

```json
{
  "sourceCompany1": [
    {
      "productId": "123",
      "productName": "Product A"
    }
  ],
  "sourceCompany2": [
    {
      "productId": "456",
      "productName": "Product B"
    }
  ]
}
```

## File Processing

- Place XML request files in the `input` directory.
- The application will process them every 10 minutes automatically.
- Processed files are moved to the `backup` directory.

## Testing

Run tests with:

```bash
./gradlew test
```

## Building

Build the JAR file with:

```bash
./gradlew build
```

The JAR will be located in `build/libs/`.

## Technologies Used

- Spring Boot 3.1.5
- H2 Database
- Jackson for XML/JSON processing
- Lombok
- JUnit 5 for testing
