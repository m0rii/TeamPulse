# TeamPulse: Daily Team Status Bot

TeamPulse is a Spring Boot application that integrates with Slack to help teams manage daily status updates. The bot allows team members to submit their daily status, including availability, tasks, and notes, and provides a summary of all team members' status.

## Features

- **Daily Reminders**: Sends scheduled daily reminders at 09:00 CET to 8 developers.
- **Interactive Status Updates**: Provides an interactive modal for developers to set their daily status (availability, tasks, notes).
- **Status Summary**: Implements a `/status` Slack slash command to display a summary of the team's daily status.
- **Secure Storage**: Persists daily status data securely in Cloudflare Workers KV.

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
3. Enable Interactivity and create a slash command `/status`
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
java -jar build/libs/slackbot-0.0.1-SNAPSHOT.jar
```

#### Unix-based Systems

```
./gradlew clean build
java -jar build/libs/slackbot-0.0.1-SNAPSHOT.jar
```

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
- Use secure HTTPS connections for all API calls.
- Implement proper error handling and logging to avoid exposing sensitive information.

## Extending the Application

The Hexagonal Architecture makes it easy to extend the application:

- **Add New Features**: Create new application services in the `application` layer.
- **Change Persistence**: Replace the Cloudflare KV adapter with another persistence adapter.
- **Add New Integrations**: Create new primary or secondary adapters as needed.

## License

This project is licensed under the MIT License - see the LICENSE file for details. 