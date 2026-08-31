<div align="center">
  <h1>Boda</h1>
  <p><strong>Plataforma web para gestionar la experiencia digital de una boda: invitaciones, panel personal, imágenes y música</strong></p>
  <p>
    <img src="https://img.shields.io/badge/Astro-7.0.5-0ea5e9?logo=astro&logoColor=white" alt="Astro">
    <img src="https://img.shields.io/badge/React-19.2-61dafb?logo=react&logoColor=black" alt="React">
    <img src="https://img.shields.io/badge/TypeScript-6.0-3178c6?logo=typescript&logoColor=white" alt="TypeScript">
    <img src="https://img.shields.io/badge/Spring_Boot-4.1-6db33f?logo=springboot&logoColor=white" alt="Spring Boot">
    <img src="https://img.shields.io/badge/Java-17-ed8b00?logo=openjdk&logoColor=white" alt="Java 17">
  </p>
</div>

---

## Descripción general

**Boda** es una plataforma full stack pensada para centralizar la parte digital de una boda. El objetivo es ofrecer una experiencia simple para los invitados: acceder a la web pública, registrarse o iniciar sesión, revisar su información personal, confirmar asistencia, ver la invitación de su familia, subir imágenes y sugerir canciones para la celebración.

La aplicación separa claramente la interfaz pública de la lógica de negocio. `frontBoda` actúa como capa de presentación y se comunica con tres servicios backend especializados: autenticación y datos de invitados, gestión de imágenes e integración musical con Spotify. Esa separación hace que el proyecto sea más fácil de mantener y más claro de explicar en entrevista.

El repositorio público incluye:

- `frontBoda`: web pública de invitados.
- `BO-USERS`: autenticación, registro y datos de invitados.
- `BO-IMAGE`: subida y resolución de imágenes.
- `BO-SPOTIFY`: integración con Spotify para sugerencias musicales.

`adminBoda/` queda fuera del repositorio público.

---

## Arquitectura

```text
Boda/
├── frontBoda/
├── BO-USERS/
├── BO-IMAGE/
├── BO-SPOTIFY/
├── .env.example
└── README.md
```

La arquitectura está dividida en cuatro partes principales:

1. `frontBoda` actúa como la capa de presentación.
2. `BO-USERS` gestiona identidad, registro y datos de familia.
3. `BO-IMAGE` se encarga del almacenamiento y resolución de imágenes.
4. `BO-SPOTIFY` centraliza la integración con Spotify.

Esta separación permite mantener el frontend ligero y mover la lógica de negocio a servicios independientes.

---

## Páginas principales

- `/`: página de inicio con el contenido principal de la boda.
- `/login`: acceso de invitados.
- `/registro`: alta de invitado.
- `/panel`: panel personal del invitado.
- `/menu`: información del menú.
- `/itinerario`: agenda del evento.
- `/imagenes`: galería y subida de imágenes.
- `/canciones`: sugerencia y gestión de canciones.
- `/invitacion/[idfamily]`: invitación personalizada por familia.

---

## Servicios

### `frontBoda`

Es la aplicación pública que ve el invitado. Desde aquí se muestran las páginas de inicio, login, registro, panel personal, itinerario, menú, imágenes y canciones. También coordina la comunicación con los servicios backend y guarda la sesión del usuario en el navegador.

### `BO-USERS`

Es el servicio de identidad y datos del invitado. Se encarga del login, la validación de token, el registro guiado, la gestión de RSVP, la actualización de datos personales y la información de la familia.

### `BO-IMAGE`

Es el servicio encargado de recibir archivos, almacenarlos en la infraestructura configurada y devolver claves o URLs públicas para que el frontend pueda mostrar imágenes y vídeos.

### `BO-SPOTIFY`

Es el servicio que permite buscar canciones, consultar la playlist compartida y añadir nuevas canciones sin exponer credenciales de Spotify en el frontend.

---

## Tech Stack

- Astro
- React
- TypeScript
- Spring Boot
- Java 17
- AWS S3
- DynamoDB
- Spotify API

---

## Estructura del proyecto

```text
Boda/
├── frontBoda/         # Frontend público
├── BO-USERS/          # Auth, invitaciones y familia
├── BO-IMAGE/          # Upload y resolución de imágenes
├── BO-SPOTIFY/        # Integración con Spotify
├── .env.example       # Variables requeridas sin secretos
└── README.md
```

---

## Flujo de uso

### Invitado nuevo

1. Entra en la web pública.
2. Accede al formulario de registro.
3. Verifica teléfono y contraseña general de invitación.
4. Completa sus datos personales.
5. Confirma asistencia y preferencias si aplica.
6. El frontend envía la información a `BO-USERS`.

### Invitado existente

1. El usuario inicia sesión con su email y contraseña.
2. `BO-USERS` valida las credenciales y devuelve la sesión.
3. `frontBoda` guarda el token y los datos básicos.
4. El invitado entra en su panel personal.
5. Puede editar datos, confirmar asistencia y gestionar su información.

### Galería de imágenes

1. El invitado abre la sección de imágenes.
2. Selecciona o arrastra archivos.
3. `frontBoda` previsualiza y envía el contenido a `BO-IMAGE`.
4. El servicio devuelve las claves o URLs públicas.
5. La galería se actualiza en la interfaz.

### Música

1. El invitado entra en la sección de canciones.
2. Busca una canción.
3. `frontBoda` consulta `BO-SPOTIFY`.
4. Puede sugerirla o añadirla a la playlist compartida.

### Invitación personalizada

1. El invitado abre su enlace de familia.
2. `frontBoda` consulta `BO-USERS`.
3. Si la familia existe, se muestra la invitación personalizada.
4. Si no existe, se presenta un mensaje de error amigable.

---

## Desarrollo local

### Frontend

```sh
cd frontBoda
npm install
npm run dev
```

### Verificaciones del frontend

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

---

## Variables de entorno

Usa `.env.example` como base para crear tu `.env` local. El repositorio público no incluye secretos ni credenciales reales.

---

## Estado del proyecto

- Repositorio público preparado para revisión en entrevista.
- Se excluyen `adminBoda/`, archivos generados, logs y credenciales locales.
- Si quieres revisar el código rápidamente, empieza por `frontBoda/src/pages/` y luego sigue con los servicios backend.
