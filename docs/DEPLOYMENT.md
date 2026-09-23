# SwiftRouteOS: Comprehensive Cloud Deployment Guide

This guide details multiple pathways to deploy SwiftRouteOS to production cloud environments, ranging from zero-ops 1-click cloud platforms to enterprise cloud providers (AWS, GCP) and self-hosted virtual servers.

---

## 🚀 Deployment Option Comparison

| Platform | Best For | Complexity | Database & Cache | Est. Cost |
|---|---|---|---|---|
| **Render** | **Recommended (1-Click Blueprint)** | Low (Zero DevOps) | Managed PostgreSQL included | Free / Low tier |
| **Railway** | Developer-friendly 1-Click | Low | Managed PostgreSQL + Redis plugins | $5/month |
| **AWS App Runner / ECS** | Enterprise Scale & Security | Medium–High | AWS RDS PostgreSQL + ElastiCache | Pay-as-you-go |
| **GCP Cloud Run** | Serverless Auto-scaling | Medium | Cloud SQL for PostgreSQL | Pay-as-you-go |
| **Linux VPS (EC2 / Hetzner)**| Full control & minimum cost | Medium | Docker Compose + Caddy SSL | $5–$10/month |

---

## ⚡ Option 1: 1-Click Deploy on Render (Recommended)

SwiftRouteOS includes a native Infrastructure-as-Code blueprint file ([`render.yaml`](../render.yaml)) at the repository root. Render reads this file and automatically provisions:
1. **Managed PostgreSQL 16 Database** (`swiftroute-postgres`)
2. **Spring Boot 3.4 API Service** (`swiftroute-backend`)
3. **React 19 + Nginx Gateway** (`swiftroute-frontend`)

### Step-by-Step Instructions:
1. Go to [https://dashboard.render.com](https://dashboard.render.com) and log in or create an account.
2. In the top navigation bar, click **"New +"** $\rightarrow$ **"Blueprint"**.
3. Connect your GitHub repository: `https://github.com/Adityasharma2107/SwiftRouteOS.git` (or your fork).
4. Render will detect `render.yaml` and display the deployment plan:
   - Database: `swiftroute-postgres`
   - Web Service: `swiftroute-backend`
   - Web Service: `swiftroute-frontend`
5. Click **"Apply"**.
6. Render will:
   - Create the PostgreSQL database and run Flyway migrations automatically on startup.
   - Build and start the Spring Boot container, waiting for the `/actuator/health` check to turn green.
   - Build and start the React/Nginx container, linking it to the backend host.
7. Once finished, click on the `swiftroute-frontend` URL (e.g., `https://swiftroute-frontend.onrender.com`) to open the live application!

---

## ⚡ Option 2: Render (Backend + PostgreSQL) + Vercel (Frontend Edge CDN) [Hybrid Recommended]

This hybrid pattern gives you the best of both worlds:
* **Render**: Runs the persistent PostgreSQL 16 database and Spring Boot 3.4 API service with zero container sleep on active plans.
* **Vercel**: Delivers ultra-low latency worldwide edge caching, automatic branch previews, and instant deployments for the React 19 SPA.

### Part A: Deploy Database & Backend on Render
1. In Render Dashboard, click **"New +"** $\rightarrow$ **"PostgreSQL"**:
   - Name: `swiftroute-postgres`
   - Database: `swiftroute_db`
   - User: `swiftroute`
   - Region: Choose closest (e.g. `Oregon` or `Frankfurt`)
   - Click **"Create Database"**.
2. Click **"New +"** $\rightarrow$ **"Web Service"**:
   - Connect repository: `https://github.com/Adityasharma2107/SwiftRouteOS.git`
   - Name: `swiftroute-backend`
   - Root Directory: `backend`
   - Runtime: `Docker` (Render will automatically detect `backend/Dockerfile`)
   - Health Check Path: `/actuator/health`
3. Add Environment Variables in Render:
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `DB_HOST`: In Render, select **"Add from Database"** $\rightarrow$ select `swiftroute-postgres` $\rightarrow$ choose `Host`
   - `DB_PORT`: `5432`
   - `DB_NAME`: `swiftroute_db`
   - `DB_USER`: Select from `swiftroute-postgres` $\rightarrow$ `User`
   - `DB_PASSWORD`: Select from `swiftroute-postgres` $\rightarrow$ `Password`
   - `JWT_SECRET`: Generate a 256-bit secret string
   - `REDIS_HEALTH_CHECK_ENABLED`: `false`
4. Click **"Create Web Service"**.
5. Copy your Render backend URL once deployed (e.g., `https://swiftroute-backend.onrender.com`).

### Part B: Deploy Frontend on Vercel
1. Log in to [https://vercel.com](https://vercel.com) and click **"Add New..."** $\rightarrow$ **"Project"**.
2. Import `Adityasharma2107/SwiftRouteOS`.
3. In Project Settings:
   - **Framework Preset**: `Vite`
   - **Root Directory**: Click "Edit" and select `frontend`
4. Expand **Environment Variables** and add:
   - `VITE_API_URL`: `https://your-backend.onrender.com/api`
   - `VITE_WS_URL`: `https://your-backend.onrender.com/ws`
5. Click **"Deploy"**.
6. Vercel automatically compiles the production assets via `npm run build` and deploys to `https://swiftroute.vercel.app`!
7. Client-side routing is handled seamlessly by [`frontend/vercel.json`](../frontend/vercel.json), and CORS/WebSockets are pre-configured to accept requests from your Vercel domain.

---

## 🚆 Option 3: Deploy on Railway

Railway offers native Dockerfile support and 1-click database provisioning.

### Step-by-Step Instructions:
1. Log in to [https://railway.app](https://railway.app).
2. Click **"New Project"** $\rightarrow$ **"Deploy from GitHub repo"**.
3. Select `Adityasharma2107/SwiftRouteOS`.
4. Add PostgreSQL:
   - In your Railway project canvas, click **"New"** $\rightarrow$ **"Database"** $\rightarrow$ **"Add PostgreSQL"**.
5. Configure Backend Service:
   - Set **Root Directory** to `backend`.
   - Set **DockerfilePath** to `Dockerfile`.
   - Under **Variables**, add:
     - `SPRING_PROFILES_ACTIVE`: `prod`
     - `DB_HOST`: `${{Postgres.PGHOST}}`
     - `DB_PORT`: `${{Postgres.PGPORT}}`
     - `DB_NAME`: `${{Postgres.PGDATABASE}}`
     - `DB_USER`: `${{Postgres.PGUSER}}`
     - `DB_PASSWORD`: `${{Postgres.PGPASSWORD}}`
     - `JWT_SECRET`: Generate a 256-bit secret (e.g. `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970`)
     - `REDIS_HEALTH_CHECK_ENABLED`: `false`
6. Configure Frontend Service:
   - In the canvas, click **"New"** $\rightarrow$ **"GitHub Repo"** $\rightarrow$ select the same repo.
   - Set **Root Directory** to `frontend`.
   - Set **DockerfilePath** to `Dockerfile`.
   - Under **Variables**, add:
     - `BACKEND_HOST`: `${{swiftroute-backend.RAILWAY_PRIVATE_DOMAIN}}:8080` (or the public backend domain)
7. Generate public domain for the frontend and open the app.

---

## ☁️ Option 3: Deploy on AWS (App Runner or ECS Fargate)

### Architecture on AWS:
* **Database**: Amazon RDS for PostgreSQL (db.t4g.micro or higher).
* **Container Registry**: Amazon ECR (Elastic Container Registry).
* **Compute**: AWS App Runner (serverless container) or Amazon ECS Fargate behind an Application Load Balancer (ALB).

### Push Images to Amazon ECR:
```bash
# 1. Authenticate Docker with AWS ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <YOUR_ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com

# 2. Build and tag backend
docker build -t swiftroute-backend ./backend
docker tag swiftroute-backend:latest <YOUR_ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/swiftroute-backend:latest
docker push <YOUR_ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/swiftroute-backend:latest

# 3. Build and tag frontend
docker build -t swiftroute-frontend ./frontend
docker tag swiftroute-frontend:latest <YOUR_ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/swiftroute-frontend:latest
docker push <YOUR_ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/swiftroute-frontend:latest
```

### Launch on AWS App Runner:
1. In the AWS App Runner console, create a service pointing to the backend image in ECR.
2. In the environment configuration, configure the RDS database credentials (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`).
3. Create a second App Runner service for the frontend image, setting `BACKEND_HOST` to the backend App Runner service domain.

---

## 🌐 Option 4: Deploy on Google Cloud Platform (Cloud Run)

### Architecture on GCP:
* **Database**: Google Cloud SQL (PostgreSQL 16 instance).
* **Compute**: Google Cloud Run (Fully managed serverless containers).

### Deployment Commands:
```bash
# 1. Enable GCP Services
gcloud services enable run.googleapis.com sqladmin.googleapis.com artifactregistry.googleapis.com

# 2. Build & Deploy Backend to Cloud Run
gcloud run deploy swiftroute-backend \
  --source ./backend \
  --region us-central1 \
  --platform managed \
  --allow-unauthenticated \
  --add-cloudsql-instances <PROJECT_ID>:us-central1:<SQL_INSTANCE_NAME> \
  --set-env-vars "SPRING_PROFILES_ACTIVE=prod,DB_NAME=swiftroute_db,DB_USER=swiftroute,DB_PASSWORD=YOUR_PASSWORD,REDIS_HEALTH_CHECK_ENABLED=false"

# 3. Build & Deploy Frontend to Cloud Run
gcloud run deploy swiftroute-frontend \
  --source ./frontend \
  --region us-central1 \
  --platform managed \
  --allow-unauthenticated \
  --set-env-vars "BACKEND_HOST=<BACKEND_CLOUD_RUN_URL>"
```

---

## 🖥 Option 5: Self-Hosted Linux VPS (Ubuntu / Debian + Docker Compose)

If deploying to a dedicated Linux server (DigitalOcean Droplet, Hetzner Cloud, Linode, AWS EC2):

### 1. SSH into Server & Install Docker:
```bash
sudo apt update && sudo apt upgrade -y
curl -fsSL https://get.docker.com -o get-docker.sh && sudo sh get-docker.sh
sudo usermod -aG docker $USER
```

### 2. Clone Repository & Setup Environment:
```bash
git clone https://github.com/Adityasharma2107/SwiftRouteOS.git
cd SwiftRouteOS
cp .env.example .env

# Edit .env with production passwords
nano .env
```

### 3. Launch with Docker Compose:
```bash
docker compose up --build -d
```

### 4. Optional: Automatic SSL with Caddy (Recommended Reverse Proxy)
Create `/etc/caddy/Caddyfile`:
```caddy
swiftroute.yourdomain.com {
    reverse_proxy localhost:80
}
```
Run `sudo systemctl reload caddy`. Caddy will automatically provision and renew free Let's Encrypt SSL certificates!

---

## 🔒 Post-Deployment Production Checklist

1. **Change Default Secrets**:
   - Change `JWT_SECRET` in `.env` to a cryptographically secure 256-bit string.
   - Update default database passwords (`POSTGRES_PASSWORD`).
2. **Verify Database Migrations**:
   - Flyway executes schema and seed migrations on boot. Check backend logs:
     `docker compose logs backend | grep -i flyway`
3. **Confirm WebSocket Connectivity**:
   - Open browser Developer Tools $\rightarrow$ Network $\rightarrow$ WS.
   - Confirm STOMP connection to `/ws` displays `CONNECTED`.
4. **Verify Health Endpoint**:
   - `curl https://your-backend-domain/actuator/health` returns `{"status":"UP"}`.
