# Keel Bank: Online Banking System

Java 25 · Spring Boot 3 · Spring Data JPA (SQL) · REST API · vanilla JS front end

## Run
    mvn spring-boot:run
Open http://localhost:8080. The default database is a file-based H2 SQL database (`./data`), so no setup is needed.
To use MySQL, edit `src/main/resources/application.properties` (instructions inside).

## Features
- Registration and sign in (BCrypt-hashed passwords, HTTP-only SameSite=Strict session cookie)
- Auto-generated 10-digit account number per user
- Balance inquiry, deposit, withdrawal
- Fund transfers between accounts: one atomic DB transaction, row locks taken in a fixed order (no lost updates, no deadlocks)
- Transaction history (latest 50) with balance after every entry
- Bean Validation on every request; a global exception handler returns JSON `{ "message": "..." }`

## REST API
| Method | Path | Purpose |
|---|---|---|
| POST | /api/auth/register | Create user + account, start session |
| POST | /api/auth/login | Sign in |
| POST | /api/auth/logout | Sign out |
| GET | /api/auth/me, /api/account | Profile and balance |
| POST | /api/deposit, /api/withdraw | `{ "amount": 100.50 }` |
| POST | /api/transfer | `{ "toAccount": "1234567890", "amount": 50, "note": "Rent" }` |
| GET | /api/transactions | Ledger |

## Layout
    model/    JPA entities (User, Account, BankTransaction)
    repo/     Spring Data repositories (incl. pessimistic-lock queries)
    service/  BankService: all business rules
    web/      Controllers, auth interceptor, exception handler
    static/   index.html, app.css, app.js

## Before going to production
Add HTTPS (set `server.servlet.session.cookie.secure=true`), rate-limit login, and use a managed DB with migrations (Flyway) instead of `ddl-auto=update`.
