<!-- Ez a dokumentum a projekt működésének vagy használatának leírását adja. -->
# RoyalCruise Postman Collection

This folder contains importable Postman assets for the RoyalCruise backend API.

## Files
- `RoyalCruise.postman_collection.json` - collection with API requests and basic response checks
- `RoyalCruise.postman_environment.json` - local environment variables

## How to use
1. Import both JSON files into Postman.
2. Select the `RoyalCruise Local` environment.
3. Start the backend on `http://localhost:8080`.
4. Run `Auth -> Login User` or `Auth -> Register User` first.
5. The `token` environment variable will be filled automatically from successful auth responses.
6. If you have an admin token, paste it into `adminToken`.

## Environment variables
- `baseUrl` - backend API base URL
- `token` - user JWT token for protected endpoints
- `adminToken` - admin JWT token for admin endpoints
- `routeId` - sample route id used by route and booking requests
- `cabinId` - sample cabin id used by cabin requests
- `bookingId` - booking id used by cancel/delete requests
- `destination` - sample destination for route filtering
- `routeName` - sample route group name for route filtering
- `email` - login email
- `password` - login password

## Notes
- Some requests are written to accept multiple expected status codes because the backend state may differ between environments.
- The collection includes smoke-level checks for status codes and basic JSON shape, not full business-rule assertions.
