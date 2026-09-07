# ---------- Stage 1: build ----------
FROM node:22-alpine AS builder
WORKDIR /build

# Instalamos dependencias primero (cachea esta capa si package*.json no cambia)
COPY package.json package-lock.json ./
RUN npm ci

# Copiamos el resto del código
COPY . .

# Astro incrusta las variables PUBLIC_* en el bundle del cliente durante el build,
# así que tienen que estar disponibles ANTES de "npm run build", no en runtime.
ARG PUBLIC_USERS_API_URL
ARG PUBLIC_IMAGES_API_URL
ARG PUBLIC_SPOTIFY_API_URL
ENV PUBLIC_USERS_API_URL=$PUBLIC_USERS_API_URL
ENV PUBLIC_IMAGES_API_URL=$PUBLIC_IMAGES_API_URL
ENV PUBLIC_SPOTIFY_API_URL=$PUBLIC_SPOTIFY_API_URL

RUN npm run build

# ---------- Stage 2: runtime ----------
FROM node:22-alpine
WORKDIR /app

# "sharp" (usado por Astro para procesar imágenes) tiene binarios nativos: no se
# puede copiar el node_modules del builder a ciegas, hay que reinstalar las
# dependencias de producción en esta misma imagen para que el binario coincida
# con la plataforma final.
COPY package.json package-lock.json ./
RUN npm ci --omit=dev

# dist/server = servidor Node (adapter "standalone"); dist/client = estáticos
# (CSS, JS del navegador, favicon...). El propio servidor de Astro sirve ambos.
COPY --from=builder /build/dist ./dist

ENV HOST=0.0.0.0
ENV PORT=4321
EXPOSE 4321
CMD ["node", "./dist/server/entry.mjs"]
