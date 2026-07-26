# Roomify

Roomify is a room-swapping web application built for the ITBA PAW course. The platform lets users publish rooms, search available stays, request swaps, manage contacts, plan trips, and review completed exchanges.

The project was migrated from a traditional server-rendered MVC application with JSP views to a RESTful API plus a React single-page application (SPA). The Java backend now exposes versioned HTTP resources, while the frontend owns navigation, state, forms, and the user experience in the browser.

## Main Features

- User registration, login, email verification, password reset, and JWT-based sessions.
- Public and private user profiles with preferences, languages, statistics, and reviews.
- Room publishing, editing, deletion, image upload, availability ranges, amenities, and search filters.
- Swap/contact requests between room owners and travelers.
- Trip and group-trip planning with destination matching.
- Review creation and deletion for completed exchanges.
- Internationalization support in English and Spanish.
- REST API documentation in `webapp/src/main/java/ar/edu/itba/paw/webapp/controller/resource/roomify-openapi.yaml`.

## Technology Stack

### Backend

- Java 21
- Maven multi-module project
- Spring Framework 5.3
- Spring Security
- Jersey / JAX-RS for REST resources
- Hibernate / JPA
- PostgreSQL
- JJWT for access and refresh tokens
- Bean Validation / Hibernate Validator
- Thymeleaf for email templates
- JavaMail for transactional emails
- Bucket4j for rate limiting
- JUnit 4, Mockito, Spring Test, and HSQLDB for tests

### Frontend

- React 19
- React Router 7
- TypeScript
- Vite
- Tailwind CSS 4
- Radix UI components
- TanStack React Query
- Axios
- React Hook Form and Zod
- i18next / react-i18next
- Vitest and Testing Library

## Project Structure

```text
.
|-- model/        Domain entities and value objects.
|-- interfaces/   Service and persistence contracts.
|-- persistence/  DAO implementations, Hibernate/JPA persistence, and SQL schema files.
|-- service/      Business logic and application services.
|-- webapp/       WAR module, Spring/Jersey configuration, REST controllers, security, DTOs, and email templates.
`-- frontend/     React SPA, API clients, hooks, routes, components, styles, and frontend tests.
```

The Maven parent project builds all modules in order:

```text
model -> interfaces -> persistence -> service -> frontend -> webapp
```

The final `webapp` WAR packages the compiled React client from `frontend/build/client`, so the backend can serve both the API and the SPA from the same deployed application.

## RESTful SPA Migration

The original application followed a server-side MVC model: controllers rendered JSP pages, navigation caused full-page reloads, and backend code was responsible for both business workflows and HTML presentation.

The migrated architecture separates those responsibilities:

- The backend exposes REST resources under `/api/*` using Jersey controllers such as `RoomController`, `UserController`, `ContactController`, `ReviewController`, `GroupTripsController`, `ImageController`, and `CountriesController`.
- Responses use JSON DTOs and custom versioned media types such as `application/vnd.roomify.room.v1+json`.
- Resource representations include links where useful, and list endpoints expose pagination through HTTP `Link` headers.
- Authentication moved to stateless token-based security with access and refresh JWTs.
- The React SPA handles routes, forms, validation, UI state, data fetching, and protected-route behavior.
- A `SpaFallbackFilter` forwards browser navigation requests to `index.html`, while `/api/*` remains reserved for REST calls.
- Static frontend assets are cached separately from API responses.
- The API contract is documented with OpenAPI in the webapp module.

This migration makes the UI independent from backend rendering, improves client-side navigation, and turns the backend into a reusable HTTP API that can be consumed by the SPA or future clients.

## REST API Areas

- `/api/` discovery endpoint.
- `/api/users` user accounts, profiles, verification, and password reset flows.
- `/api/rooms` room search, room details, creation, update, deletion, and availability.
- `/api/contacts` swap/contact requests.
- `/api/reviews` room and user reviews.
- `/api/group-trips` trip planning and destination management.
- `/api/images` multipart image upload and retrieval.
- `/api/countries` country metadata used by search and trip planning.

## Deployment

The backend is packaged as a WAR named `webapp.war`. During the Maven build, the frontend module installs Node.js/npm, runs `npm install`, builds the React application, and the WAR module includes the generated client assets.

Deploy the generated WAR to a servlet container that supports the configured Java/Spring/Jersey stack, with PostgreSQL and the required application properties available at runtime.

## How to Run

Before running the project, create the local configuration files from the examples and fill in the values for your environment.

```bash
cp webapp/src/main/resources/application.properties.example webapp/src/main/resources/application.properties
cp frontend/.env.example frontend/.env
```

In `webapp/src/main/resources/application.properties.example`, configure:

- `datasource.url`, `datasource.username`, `datasource.password`
- `spring.mail.*`
- `app.url`
- `app.name`
- `cors.url`

In `frontend/.env.example`, configure:

- `VITE_PUBLIC_URL`
- `VITE_API_URL`

The backend example points to a local PostgreSQL database named `paw`. Make sure PostgreSQL is running and that the configured database exists before starting the application.

JWT keys are loaded from the webapp resources. Keep real keys out of version control when preparing a production deployment.

Then build the complete application with Maven:

```bash
mvn clean package
```

Run backend and frontend tests through Maven:

```bash
mvn test
```

Work on the frontend locally:

```bash
cd frontend
npm install
npm run dev
```

Build only the frontend:

```bash
cd frontend
npm run build
```

Run frontend tests:

```bash
cd frontend
npm test
```
