Home Assignment: Event Reader System with Spring Boot
Objective
Create an event reader system using Spring Boot that processes insurance product data from
files and provides a REST API for querying the processed information. The completed project
should be pushed to a GitHub repository.
Requirements
1. File Processing:
- Implement a scheduled process to read request files from a specified file system folder every
fixed time (configurable interval).
- After processing each file, move it to a backup folder.
- Use an in-memory database (H2 Database) to store the processed data.
- Request example attached

2. REST API:
- Develop a REST API endpoint to retrieve products by InsuredId.
- The API should return results in JSON format.
- Group the products by SourceCompany for each InsuredId.

3. Scheduled Data Refresh:
- Implement a scheduled process to read data from the input folder every 10 minutes.