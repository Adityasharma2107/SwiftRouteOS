# ==============================================================================
# SwiftRouteOS Frontend - Production Multi-Stage Dockerfile
# Stage 1: Build static distribution with Node 22 Alpine
# Stage 2: Serve via High-Performance Nginx Alpine Reverse Proxy
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Builder
# ------------------------------------------------------------------------------
FROM node:22-alpine AS builder

WORKDIR /app

# Install dependencies
COPY package.json package-lock.json* ./
RUN npm ci || npm install

# Copy source code and build production assets
COPY . .
RUN npm run build

# ------------------------------------------------------------------------------
# Stage 2: Runtime Nginx
# ------------------------------------------------------------------------------
FROM nginx:1.27-alpine AS runtime

LABEL maintainer="SwiftRouteOS Engineering <team@swiftroute.io>" \
      description="SwiftRouteOS Vite React Frontend & Nginx Gateway" \
      version="1.0.0"

# Remove default nginx website configuration
RUN rm -rf /etc/nginx/conf.d/default.conf

# Copy custom Nginx configuration and template for dynamic cloud environments
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

ENV BACKEND_HOST=backend:8080
ENV PORT=80

# Copy built production assets from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

# Health check verifying Nginx responds with HTTP 200
HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -q -O /dev/null http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
