# Church API

A comprehensive backend API for church management built with Clojure, GraphQL (Lacinia), and SQLite. This API provides CRUD operations for managing church members, events, groups, attendance, and donations.

## Features

- **GraphQL API**: Modern API using Lacinia for GraphQL implementation
- **SQLite Database**: Lightweight, serverless database for easy deployment
- **Comprehensive Testing**: Over 55% test coverage with unit, integration, and e2e tests
- **CI/CD Pipeline**: Automated testing and deployment with GitHub Actions

## Data Models

The API manages the following entities:

- **Members**: Church members with personal information
- **Events**: Church events and services
- **Groups**: Ministry groups and teams
- **Attendance**: Records of members attending events
- **Donations**: Financial contributions

## Getting Started

### Prerequisites

- [Clojure](https://clojure.org/guides/getting_started) (1.11.1 or higher)
- Java JDK 11+

### Installation

1. Clone the repository:
   ```
   git clone https://github.com/your-username/church-api.git
   cd church-api
   ```

2. Install dependencies:
   ```
   clojure -P
   ```

3. Run the application:
   ```
   clojure -M -m church-api.core
   ```

The GraphQL API will be available at http://localhost:8888/graphql
GraphiQL interface will be available at http://localhost:8888/graphiql

## GraphQL API

### Queries

- `members`: Get all church members
- `member(id: Int!)`: Get a specific member by ID
- `events`: Get all church events
- `event(id: Int!)`: Get a specific event by ID
- `groups`: Get all church groups
- `group(id: Int!)`: Get a specific group by ID

### Mutations

#### Members
- `createMember(input: MemberInput!)`: Create a new member
- `updateMember(id: Int!, input: MemberInput!)`: Update an existing member
- `deleteMember(id: Int!)`: Delete a member

#### Events
- `createEvent(input: EventInput!)`: Create a new event
- `updateEvent(id: Int!, input: EventInput!)`: Update an existing event
- `deleteEvent(id: Int!)`: Delete an event

#### Groups
- `createGroup(input: GroupInput!)`: Create a new group
- `updateGroup(id: Int!, input: GroupInput!)`: Update an existing group
- `deleteGroup(id: Int!)`: Delete a group
- `addMemberToGroup(memberId: Int!, groupId: Int!)`: Add a member to a group
- `removeMemberFromGroup(memberId: Int!, groupId: Int!)`: Remove a member from a group

#### Attendance & Donations
- `createAttendance(input: AttendanceInput!)`: Record a member's attendance at an event
- `createDonation(input: DonationInput!)`: Record a donation

## Development

### Running Tests

Run all tests:
```
clojure -M:test
```

Run specific test namespace:
```
clojure -M:test -n church-api.integration.member-test
```

### Building

Build an uberjar:
```
clojure -T:build uber
```

The jar will be created at `target/church-api.jar`

## Deployment

The project includes a CI/CD pipeline using GitHub Actions that:

1. Runs all tests
2. Performs linting
3. Builds an uberjar
4. Deploys to production (when merged to main/master)

## License

This project is licensed under the MIT License - see the LICENSE file for details.
