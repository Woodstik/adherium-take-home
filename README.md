### Setup Instructions
- Clone Repo

### Run Instructions
- execute `./gradlew runTests`

### Future Work
- Start implementing HailieSensor
  - Internally handle bound state changes
  - Internally handle GattConnection state changes
- Telemetry, logs 
- Background scheduling for triggering syncing
- Sending events to api
- Could improve retry backoff mechanism based on error type, also number of retries based on error type
- Actually persisting synced data
- Setting up dependency injection library
- Sending data to server
- Implement real BLE connection
- Handle any hardware specific edge cases
- Fine tuning parameters used for timeouts, page size, etc
- Better docs, would be nice to have some diagrams
