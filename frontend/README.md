# LicenseHub Frontend

The LicenseHub frontend is a Next.js App Router application for managing products, licenses, and machine activations through the Spring Boot API.

See the [repository README](../README.md) for the architecture, backend setup, domain model, and complete local-development instructions.

## Local development

Start PostgreSQL and the Spring Boot backend first. Then run:

```powershell
Copy-Item .env.example .env.local
npm.cmd ci
npm.cmd run dev
```

Open <http://localhost:3000>.

## Commands

```powershell
npm.cmd run lint
npm.cmd run test:run
npm.cmd run build
npm.cmd run test:e2e
npm.cmd run test:e2e:ui
```

The Playwright commands require PostgreSQL and the Spring Boot application to be running. Playwright starts or reuses the local Next.js development server.

## Environment

| Variable | Default | Purpose |
|---|---|---|
| `BACKEND_URL` | `http://localhost:8080` | Backend origin used by server-side requests and Next.js rewrites |
