# Boda

Plataforma web para una boda con frontend público en Astro y servicios backend en Spring Boot para invitados, imágenes y música.

## Public scope

Este repositorio público incluye:

- `frontBoda`: web pública de invitados.
- `BO-USERS`: autenticación, registro y datos de invitados.
- `BO-IMAGE`: subida y resolución de imágenes.
- `BO-SPOTIFY`: integración con Spotify para sugerencias musicales.

`adminBoda/` se mantiene fuera del repositorio público.

## Architecture

```text
Boda/
├── frontBoda/
├── BO-USERS/
├── BO-IMAGE/
├── BO-SPOTIFY/
├── .env.example
└── README.md
```

## Tech Stack

- Astro
- React
- TypeScript
- Spring Boot
- Java 17
- MongoDB
- AWS S3 / DynamoDB
- Spotify API

## Local setup

1. Copy `.env.example` to `.env` and fill in the values.
2. Install dependencies in `frontBoda`.
3. Build or run each backend service from its own folder.

### Frontend

```sh
cd frontBoda
npm install
npm run dev
```

### Frontend checks

```sh
npm run check
npm run build
```

## Environment variables

Use `.env.example` as the reference for required configuration.

## Notes

- The public repo intentionally excludes generated files, logs, local environment files, and the private `adminBoda/` app.
- This project is intended as a portfolio codebase for interview review.
