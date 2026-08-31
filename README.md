# Boda

Plataforma web para una boda con frontend público en Astro y servicios backend en Spring Boot para invitados, imágenes y música.

## Overview

`Boda` es un monorepo preparado para mostrar en entrevista una solución full stack orientada a experiencia de invitado.

El repositorio público incluye:

- `frontBoda`: web pública de invitados.
- `BO-USERS`: autenticación, registro y datos de invitados.
- `BO-IMAGE`: subida y resolución de imágenes.
- `BO-SPOTIFY`: integración con Spotify para sugerencias musicales.

`adminBoda/` se mantiene fuera del repositorio público.

## Features

- Registro e inicio de sesión de invitados.
- Panel personal con datos familiares y confirmación de asistencia.
- Galería y subida de imágenes.
- Sugerencia de canciones para la playlist de la boda.
- Invitación personalizada por familia.

## Tech Stack

- Astro
- React
- TypeScript
- Spring Boot
- Java 17
- AWS S3
- DynamoDB
- Spotify API

## Repository Layout

```text
Boda/
├── frontBoda/         # Frontend público
├── BO-USERS/          # Auth, invitaciones y familia
├── BO-IMAGE/          # Upload y resolución de imágenes
├── BO-SPOTIFY/        # Integración con Spotify
├── .env.example       # Variables requeridas sin secretos
└── README.md
```

## Local Setup

1. Copia `.env.example` a `.env` y completa los valores.
2. Instala dependencias en `frontBoda`.
3. Arranca cada backend desde su carpeta.

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

### Backends

Desde cada carpeta del servicio:

```sh
./mvnw spring-boot:run
```

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

## Environment Variables

Usa `.env.example` como base. No publiques el `.env` real.

## Interview Notes

- Este repo está pensado para revisión de código y arquitectura.
- Se excluyen archivos generados, logs, credenciales locales y `adminBoda/`.
- Si quieres evaluar rápido el proyecto, empieza por `frontBoda/src/pages/` y luego revisa `BO-USERS`, `BO-IMAGE` y `BO-SPOTIFY`.
