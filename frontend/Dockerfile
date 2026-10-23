# Dockerfile for the Truvira frontend: BUILD the React app with Node.js, then SERVE the
# finished files with Caddy. React doesn't "run" on a server: it becomes plain HTML/JS/CSS files
# that the browser downloads and runs itself.

# =====================================================================================
# STAGE 1: "build" - Node.js turns our React code into static files (the dist/ folder).
# =====================================================================================
# node:22 = a Long-Term-Support Node.js version; "alpine" = a very small Linux base.
FROM node:22-alpine AS build
WORKDIR /app

# Layer caching: install libraries first, so editing a component doesn't reinstall everything.
# "npm ci" (clean install) = install EXACTLY the versions in package-lock.json: same result every build.
COPY package.json package-lock.json ./
RUN npm ci

# ARG = a setting given while BUILDING the image (not while running it).
# Vite writes VITE_API_URL into the JavaScript files at build time (see src/api.js).
# "/api" = "the API lives at the SAME address as the website" (Caddy forwards it to the backend).
ARG VITE_API_URL=/api
ENV VITE_API_URL=${VITE_API_URL}

# Copy the source code and build. Output goes to /app/dist.
COPY . .
RUN npm run build

# =====================================================================================
# STAGE 2: "serve" - Caddy web server with only the finished files.
# No Node.js, no node_modules (45 MB), no source code in the final image.
# =====================================================================================
FROM caddy:2.8-alpine

# Our Caddy settings (reverse proxy for /api + serving the React files).
COPY Caddyfile /etc/caddy/Caddyfile
# The built React app from stage 1.
COPY --from=build /app/dist /srv

# 80 = HTTP, 443 = HTTPS (used on the real server with a domain name).
EXPOSE 80 443

# No CMD needed: the official Caddy image already starts "caddy run --config /etc/caddy/Caddyfile".
# Note: Caddy runs as root here, as in the official image, because web servers need to open
# ports 80/443. It only serves files and forwards requests; the backend and AI service are non-root.
