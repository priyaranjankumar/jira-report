# Jira Report

This project is a Java-based application designed to generate reports from Jira. It provides an efficient way to extract and analyze data from Jira for better project management and tracking.

## Features

- Fetch data from Jira using API.
- Generate detailed reports.
- Customizable configurations.

## Prerequisites

- Java 11 or higher.
- Maven installed.
- Jira account with API access.

## Setup Instructions

1. Clone the repository:

   ```bash
   git clone https://github.com/your-repo/jira-report.git
   cd jira-report
   ```

2. Build the project:

   ```bash
   mvn clean install
   ```

3. Set up the required environment variables:

   - `JIRA_BASE_URL`: The base URL of your Jira instance.
   - `JIRA_USERNAME`: Your Jira username.
   - `JIRA_API_TOKEN`: Your Jira API token.

   Example:

   ```bash
   export JIRA_BASE_URL=https://your-jira-instance.atlassian.net
   export JIRA_USERNAME=your-email@example.com
   export JIRA_API_TOKEN=your-api-token
   export MAIL_HOST=smtp.gmail.com
   export MAIL_PORT=465
   export MAIL_USERNAME=username@gmail.com
   export MAIL_PASSWORD=your-email-password(app password in case of gmail)
   ```

4. Run the application:
   ```bash
   mvn spring-boot:run
   ```

## Usage

- Configure the application by editing the `application.properties` file if needed.
- Run the application and follow the prompts to generate reports.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please open an issue or submit a pull request.
