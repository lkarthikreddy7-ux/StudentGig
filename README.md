# StudentGig

StudentGig is a career-first student platform built with Java 17+, Spring Boot 3.5, MySQL, REST APIs, HTML/CSS/JavaScript, and session authentication.

## Roles

### Student
- Job marketplace with full-time, part-time, internship, trainee, apprenticeship, graduate program, volunteer and other work types.
- Remote, hybrid and on-site work modes.
- Applications and saved career workflow.
- Profile and career tools.
- AI Resume Builder with ATS-friendly preview and browser PDF printing.
- Online Code Lab: Java 17 and Python 3 only.
- Coding assessments with MCQ + coding questions, sample tests and hidden tests.
- Configurable browser proctoring events: camera permission/disconnect, tab visibility, focus loss, fullscreen and mobile blocking.
- Logout.

### Recruiter
- Job/company management endpoints and career marketplace access.
- Candidate application status updates.

### Admin
The admin UI is intentionally isolated: when an ADMIN session is active, the sidebar contains only Admin Control and Logout. Student/recruiter pages are not shown in the admin UI.

Admin capabilities:
- Platform statistics.
- User role management.
- Assessment builder and question bank workflow.
- MCQ and coding questions.
- Sample input/output and hidden test cases.
- Assessment duration, publish state and violation limit.
- Proctoring settings per assessment.
- Admin compiler using the same Java/Python execution engine as student coding tests.
- Job/company/user management endpoints.

## Default admin

Created automatically on first startup if the configured email does not already exist.

```text
Email: admin@studentgig.local
Password: Admin@12345
```

Change it before production use:

PowerShell:

```powershell
$env:STUDENTGIG_ADMIN_EMAIL="admin@yourdomain.com"
$env:STUDENTGIG_ADMIN_PASSWORD="Use-A-Strong-Password"
```

## MySQL

Set the password for your local MySQL installation. Do not commit credentials.

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/studentgig?createDatabaseIfNotExist=true&serverTimezone=Asia/Kolkata"
$env:DB_USER="root"
$env:DB_PASSWORD="YOUR_MYSQL_PASSWORD"
```

Hibernate automatically creates/updates the application tables with `ddl-auto=update`.

## Run on Windows

Open PowerShell in the directory that contains `pom.xml`:

```powershell
& "C:\Users\HP\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd" clean spring-boot:run
```

Do not type `mvn` after `mvn.cmd`.

Open:

```text
http://localhost:8080
```

## Compiler requirements

The Code Lab executes code on the server machine. Install:

- JDK 17+ with `javac` and `java`.
- Python 3 with `python` available on Windows PATH.

The Java runner uses the current JDK's `java.home` when possible, so the same JDK used to run Spring Boot is preferred.

Execution limits:
- Maximum source: 30,000 characters.
- Maximum input: 10,000 characters.
- Maximum output: 20,000 characters.
- Java compilation: 8 seconds.
- Program execution: 3 seconds per test.
- Maximum 20 student Code Lab test cases per run.

For public internet deployment, move compilation/execution into a real sandbox/container service. Do not expose raw host process execution to untrusted internet users.

## AI Resume Builder

Set an OpenAI-compatible API key to enable AI generation. Without a key, StudentGig uses a deterministic profile-based professional resume fallback so the feature remains usable.

```powershell
$env:AI_API_KEY="YOUR_API_KEY"
$env:AI_MODEL="gpt-4o-mini"
```

The AI prompt explicitly instructs the model not to invent employers, dates, degrees or metrics.

## Error-check notes

- Frontend JavaScript passes Node `--check` syntax validation.
- The shared Java code execution service passes standalone `javac` syntax validation with its Spring annotation stub.
- All Java source files pass brace-balance validation.
- Full Maven dependency compilation must be run on a machine with Maven and network/local Maven dependencies available.
### Exam proctoring behavior
During an active assessment, enabled browser proctoring events create a single warning per user action. Tab-switch and window-focus events that fire together are de-duplicated, so one tab/window change does not incorrectly count as multiple violations. Warnings are shown inside the exam page rather than with browser alerts, preventing the warning itself from triggering another focus-loss event.
