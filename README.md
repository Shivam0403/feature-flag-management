# feature-management

Java Spring Boot project for feature flag management and dynamic feature evaluation.

## Setup Instructions

1. Make sure Java 17+ and Maven are installed.
2. Clone the repository and change into the project folder.
3. Build the project:

   ```bash
   mvn clean package
   ```

4. Run the application:

   ```bash
   mvn spring-boot:run
   ```

5. The service will start on `http://localhost:8080`.

## API

- `POST /api/flags`
  - create a feature flag with `name`, `defaultState`, and `rules`
- `POST /api/flags/{name}/evaluate`
  - evaluate a feature flag based on runtime user context (`userId`, `subscriptionTier`, `region`)
- `GET /api/flags`
  - list all feature flags with their configurations

## Example Requests

### Create a feature flag with context-based rules

```http
POST /api/flags
Content-Type: application/json

{
  "name": "checkout-redesign",
  "defaultState": false,
  "rules": [
    {
      "attribute": "region",
      "operator": "IN",
      "values": ["us", "ca"],
      "state": true,
      "priority": 1
    },
    {
      "attribute": "subscriptionTier",
      "operator": "IN",
      "values": ["pro"],
      "state": true,
      "priority": 2
    }
  ]
}
```

### Create a feature flag with percentage rollout

```http
POST /api/flags
Content-Type: application/json

{
  "name": "new-ui-feature",
  "defaultState": false,
  "rules": [
    {
      "attribute": "userId",
      "operator": "PERCENTAGE_ROLLOUT",
      "values": [],
      "state": true,
      "priority": 0,
      "percentageRollout": 30
    }
  ]
}
```

This rolls out the feature to approximately 30% of users deterministically based on userId hash. The same userId will always receive the same rollout decision.

### Evaluate a feature flag

```http
POST /api/flags/checkout-redesign/evaluate
Content-Type: application/json

{
  "userId": "user-123",
  "subscriptionTier": "pro",
  "region": "us"
}
```

### Example response

```json
{
  "featureFlagName": "checkout-redesign",
  "enabled": true,
  "result": "ON"
}
```

## Test

    mvn test

## Architecture Overview

```mermaid
flowchart LR
    U[Client / User] --> C[API Controller]
    C --> S[Application Service]
    S --> CACHE[(In-Memory Cache)]
    S --> R[Repository]
    R --> DB[(Persistent Database)]

    subgraph Evaluation
        E[Evaluation Request<br/>userId, subscriptionTier, region]
        RULE[Rule Engine]
        E --> RULE
        RULE --> RESULT[Enabled / Disabled Decision]
    end

    S --> E
    RESULT --> S

    C -->|Create / Update Flag| S
    S -->|Persist Rules & Flag State| R
    S -->|Read cached flag first| CACHE
    CACHE -->|Cache miss| R
    R -->|Return flag definition| S
```

## High-Level Flow

1. A client sends a request to create or update a feature flag.
2. The controller passes the request to the service layer.
3. The service stores the flag definition and rules in the database.
4. The service also updates the in-memory cache for fast future reads.
5. When a feature is evaluated, the client sends user context such as userId, subscriptionTier, and region.
6. The service checks the cache first; if the flag is missing, it loads it from the database.
7. The rule engine evaluates the flag against the provided context.
8. The service returns the final enabled or disabled result.

---

## Detailed Flow Diagrams

### 1. Create Flag Flow

```mermaid
flowchart TD
    A[Client: POST /api/flags] --> B[Controller: Validate Request]
    B --> C{Valid Request?}
    C -->|No| D[Return 400 Error]
    C -->|Yes| E[Service: Create Flag]
    E --> F[Service: Validate Flag Name Unique]
    F --> G{Exists?}
    G -->|Yes| H[Return 409 Conflict]
    G -->|No| I[Service: Parse Rules]
    I --> J[Repository: Save Flag & Rules]
    J --> K[Database: Insert Flag]
    K --> L[Database: Insert Rules]
    L --> M[Service: Update Cache]
    M --> N[Cache: Store Flag]
    N --> O[Controller: Return 201 Created]
    O --> P[Client: Receive Confirmation]
```

### 2. Evaluate Flag Flow

```mermaid
flowchart TD
    A[Client: POST /api/flags/name/evaluate] --> B[Controller: Parse User Context]
    B --> C[Service: Evaluate Flag]
    C --> D[Service: Check Cache]
    D --> E{Flag in Cache?}
    E -->|Yes| F[Cache: Return Flag Definition]
    E -->|No| G[Repository: Query Flag by Name]
    G --> H[Database: Fetch Flag & Rules]
    H --> I[Service: Update Cache]
    I --> J[Cache: Store Flag]
    J --> K[Rule Engine: Sort Rules by Priority]
    K --> L[Rule Engine: Iterate Rules]
    L --> M{Rule Type?}
    M -->|Standard Rule| N{Rule Matches<br/>User Context?}
    M -->|Percentage Rollout| O[Bucketing: Hash userId]
    O --> P{User in<br/>Percentage?}
    N -->|Yes| Q[Rule Engine: Return Rule State]
    N -->|No| R{More Rules?}
    P -->|Yes| Q
    P -->|No| R
    R -->|Yes| L
    R -->|No| S[Rule Engine: Return Default State]
    Q --> T[Service: Build Response]
    S --> T
    T --> U[Controller: Return Evaluation Result]
    U --> V[Client: Receive enabled/disabled]
```

### 3. List Flags Flow

```mermaid
flowchart TD
    A[Client: GET /api/flags] --> B[Controller: List Flags]
    B --> C[Service: List All Flags]
    C --> D[Repository: Query All Flags]
    D --> E[Database: Fetch All Flag Records]
    E --> F[Service: Transform to Summary]
    F --> G[Controller: Return Flag Summaries]
    G --> H[Client: Receive Flag List]
```

### 4. Cache Interaction Flow

```mermaid
flowchart LR
    A[Client Request] --> B{Cache Hit?}
    B -->|Hit| C[Cache: Return Flag]
    B -->|Miss| D[Database: Fetch Flag]
    C --> E[Rule Engine]
    D --> F[Cache: Store Flag]
    F --> E
    E --> G[Return Result]
    G --> H[Client Response]
```
