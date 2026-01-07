### Setup Instructions
1. Clone the repository
2. Ensure JDK 11 is installed
3. Open the project in Android Studio
4. Let Gradle sync and download dependencies

### Run Instructions
- From the project root directory, run unit tests using the Gradle wrapper:
  ```
  ./gradlew test
  ```
- The primary test suite is `SyncManagerTest`, which exercises the core sync logic

### Key Files
- `SyncManager`
- `ActuationEventRepoImpl`
- `SyncManagerTest`

### Future Work
- Implement full `HailieSensor` integration
  - Handle bonded state transitions internally
  - Manage `GattConnection` lifecycle and state changes
  - Implement real BLE connection logic
  - Handle hardware-specific edge cases

- Improve sync robustness and observability
  - Add structured telemetry and metrics
  - Improve logging and error reporting
  - Fine-tune timeout values, page size, and retry parameters
  - Enhance retry backoff strategy based on error type

- Data handling and persistence
  - Persist synced actuation data beyond in-memory storage
  - Implement reliable delivery of events to backend APIs
  - Handle offline scenarios and deferred uploads

- Scheduling and execution
  - Add background scheduling (e.g., WorkManager) to trigger syncs
  - Support foreground-triggered and periodic sync strategies

- Architecture and tooling
  - Introduce a dependency injection framework
  - Improve documentation, including architectural diagrams
  - Expand test coverage to include integration and stress tests
