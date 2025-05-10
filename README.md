# TeamPulse: Daily Team Status Bot

TeamPulse is a Java 21 / Spring Boot Slack bot that sends a daily check-in modal, saves answers in Cloudflare Workers KV, and shows a live roster via /status. It also detects huddles & presence, runs entirely on Cloudflare's free tier—no servers, no bills.

## Features

- **Daily Reminders**: Sends scheduled daily reminders at 09:00 CET to 8 developers.
- **Interactive Status Updates**: Provides an interactive modal for developers to set their daily status (availability, tasks, notes).
- **Status Summary**: Implements a `/status` Slack slash command to display a summary of the team's daily status.
- **Secure Storage**: Persists daily status data securely in Cloudflare Workers KV.
- **Team Management**: Creates and manages teams with multiple managers for proper access controls.
- **Access Control**: Enforces team-based permissions for viewing status updates and accessing team resources.

## Team Features

### Multiple Team Managers

TeamPulse now supports multiple managers per team, distributing leadership responsibilities and improving team management:

- Each team can have multiple managers with equal permissions
- Managers can promote team members to manager role
- Managers can demote other managers (with safeguards to prevent teams from having no managers)
- Team status information is accessible based on team membership and role

### Team-Based Commands

The bot supports the following team management commands:

- `/team create [team_name] [description]` - Create a new team
- `/team list` - List all available teams
- `/team join [team_id]` - Join an existing team
- `/team leave [team_id]` - Leave a team
- `/team add [team_id] [user_id]` - Add a user to a team (manager only)
- `/team remove [team_id] [user_id]` - Remove a user from a team (manager only)
- `/team promote [team_id] [user_id]` - Promote a member to manager (manager only)
- `/team demote [team_id] [user_id]` - Demote a manager to regular member (manager only)
- `/team info [team_id]` - Display team information

## Technology Stack

- Java 21
- Spring Boot 3.3.x
- Slack Bolt SDK
- Spring WebFlux (for HTTP client)
- Gradle Build Tool
- Cloudflare Workers KV (for persistence)

## Project Structure

The project follows the Hexagonal Architecture pattern:

```
src/main/java/com/example/slackbot/
├── domain/              # Domain models
├── application/         # Application services and use cases
│   └── impl/            # Implementation of application services
├── adapters/
│   ├── primary/         # Primary adapters (Slack Event Adapter)
│   └── secondary/       # Secondary adapters (Cloudflare KV Adapter)
└── configuration/       # Configuration classes
```

## Setup and Installation

### Prerequisites

- Java 21 JDK
- Gradle
- Slack App with appropriate permissions
- Cloudflare Workers KV namespace

### Slack App Configuration

1. Create a new Slack App at [api.slack.com](https://api.slack.com/apps)
2. Under "OAuth & Permissions", add the following scopes:
   - `chat:write`
   - `commands`
   - `users:read`
   - `im:write`
   - `im:history`
3. Enable Interactivity and create slash commands:
   - `/status` - For checking team status
   - `/team` - For team management functions
4. Install the app to your workspace

### Environment Variables

Set the following environment variables:

```
SLACK_SIGNING_SECRET=<your-slack-signing-secret>
SLACK_BOT_TOKEN=<your-slack-bot-token>
CLOUDFLARE_ACCOUNT_ID=<your-cloudflare-account-id>
CLOUDFLARE_NAMESPACE_ID=<your-cloudflare-namespace-id>
CLOUDFLARE_API_TOKEN=<your-cloudflare-api-token>
```

### Build and Run

#### Windows

```
gradlew clean build
java -jar build/libs/teampulse-0.0.1-SNAPSHOT.jar
```

#### Unix-based Systems

```
./gradlew clean build
java -jar build/libs/teampulse-0.0.1-SNAPSHOT.jar
```

## Testing

The application has comprehensive unit and integration tests:

```
# Run unit tests
./gradlew test

# Run integration tests 
./gradlew integrationTest
```

### Test Structure

The test suite is organized into two main categories:

#### Unit Tests
- Use the `test` profile
- Focus on testing individual components in isolation
- Mock external dependencies
- Examples:
  - `TeamServiceTest` - Tests the multiple team managers functionality
  - `SlackTeamAdapterTest` - Tests team command handling with mocks

#### Integration Tests
- Use the `integrationTest` profile
- Test the interaction between multiple components
- Examples:
  - `TeamServiceIntegrationTest` - Tests the service layer with multiple managers
  - `SlackEventTeamAccessIntegrationTest` - Tests team-based access control
  - `SlackBotIntegrationTest` - Tests the overall application behavior

### Test Configuration

The application uses separate configuration profiles for testing:

- `application-test.properties` - Configuration for unit tests
- `application-integrationTest.properties` - Configuration for integration tests

These profiles provide mock values for external services like Slack and Cloudflare KV.

## Deployment to Cloudflare Workers

### Prerequisites

- Install Wrangler CLI: `npm install -g @cloudflare/wrangler`

### Deployment Steps

1. Login to Cloudflare:
   ```
   wrangler login
   ```

2. Create a `wrangler.toml` file:
   ```toml
   name = "teampulse"
   type = "javascript"
   account_id = "<your-cloudflare-account-id>"
   workers_dev = true
   kv_namespaces = [
     { binding = "DAILY_STATUS", id = "<your-namespace-id>" }
   ]
   ```

3. Deploy the application:
   ```
   wrangler deploy
   ```

## Security Considerations

- Slack request verification is implemented to validate that requests are coming from Slack.
- All secrets are stored as environment variables and not hardcoded in the application.
- Team-based access control ensures users only see status information for their own teams.
- Use secure HTTPS connections for all API calls.
- Implement proper error handling and logging to avoid exposing sensitive information.

## Extending the Application

The Hexagonal Architecture makes it easy to extend the application:

- **Add New Features**: Create new application services in the `application` layer.
- **Change Persistence**: Replace the Cloudflare KV adapter with another persistence adapter.
- **Add New Integrations**: Create new primary or secondary adapters as needed.
- **Enhance Team Features**: Add more team-based functionality in the team service layer.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
