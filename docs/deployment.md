# ShopSphere — Production Deployment & Cloud Hosting Guide

## 1. Hosting Architecture & Provider Comparison

To deploy ShopSphere as a live portfolio demonstration, the target platform must support:
1. An executable **Spring Boot (Java 21/24)** service (JAR or Docker container).
2. A persistent **MySQL 8.0** relational database instance.
3. Automatic **HTTPS/TLS** with proxy header forwarding for secure session cookies.
4. Health-check integration via `/actuator/health`.
5. Secure environment variable injection for credentials.

### Provider Comparison Matrix

| Provider | Java / Spring Boot Support | MySQL Persistence Support | Pricing / Free Allowance | Limits & Quirks | Overall Rating for Freshers |
|---|---|---|---|---|---|
| **Railway** *(Recommended)* | Native Dockerfile or Maven Nixpack | 1-Click Managed MySQL 8.0 with persistent volume | $5 monthly credit on trial; ~$5/mo pay-as-you-go | No forced sleep; persistent MySQL connection; shared private network | **Top Recommendation** (Easiest full-stack setup) |
| **Render** | Native Docker or Native Java 21 Web Service | No native free MySQL (Only Postgres). Requires external MySQL (e.g. Aiven or Railway) | Free tier Web Service ($0); external MySQL required | Free web services spin down after 15 min inactivity (50s cold start) | **Good Alternative** (Free, but requires 2 separate providers) |
| **Aiven + Render** | Render Web Service | Free Tier Managed MySQL (1 node, 5GB storage) on Aiven | 100% Free combined tier | Aiven free MySQL is always-on; Render spins down on idle | **Best $0 Cost Alternative** |
| **Fly.io** | Native Docker containers | No native MySQL (Requires Dockerized MySQL with attached NVMe volume or external DB) | Pay-as-you-go / credit card required | Complex CLI setup with `flyctl`; requires manual volume management | Intermediate |
| **AWS (App Runner + RDS)** | Enterprise Container Runner | AWS RDS MySQL 8.0 | Free tier for 12 months (750 hrs db.t3.micro + App Runner) | Credit card required; potential surprise bills if Free Tier limits exceeded | Advanced / Enterprise |

---

## 2. Production Configuration & Architecture

### 2.1 Profile & Security Configuration
When running in production (`SPRING_PROFILES_ACTIVE=prod`):
- **Database Engine**: Connects to the cloud MySQL instance via `DB_URL`.
- **Flyway Migrations**: Runs automatically on application startup (`V1` through `V5`), executing schema validation, table creation, and baseline catalog seeding if empty.
- **Hibernate DDL Auto**: Set to `validate` (never `update` or `create-drop`). Schema integrity is strictly managed by Flyway.
- **Forwarded Headers**: Configured with `server.forward-headers-strategy: framework`. Cloud reverse proxies (which terminate SSL and forward HTTP to the container) provide `X-Forwarded-Proto: https`. Spring Security recognizes this, preserving secure session cookies and redirect schemes.
- **Session Cookie**: `server.servlet.session.cookie.secure: true`, `http-only: true`, `same-site: lax`.
- **Actuator Health Probe**: Exposes `GET /actuator/health` publicly (returning `{"status":"UP"}` without leaking internal memory/disk details).

### 2.2 Environment Variables

| Variable | Required | Description | Example Production Value |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Yes | Activates production configuration | `prod` |
| `DB_URL` | Yes | Cloud MySQL JDBC URL | `jdbc:mysql://mysql.railway.internal:3306/railway?useSSL=true&serverTimezone=UTC` |
| `DB_USERNAME` | Yes | Cloud database username | `root` or generated user |
| `DB_PASSWORD` | Yes | Cloud database password | Strong generated secret |
| `PORT` | Auto | Port injected by cloud platform | `8080` (Render/Railway inject this dynamically) |

---

## 3. Recommended Deployment Path: Railway (Step-by-Step)

Railway provides the simplest, most robust environment for a Spring Boot + MySQL monolith because both components reside in the same project and communicate over a private, low-latency internal network.

### Step 1: Create Railway Account & Project
1. Log in to [Railway.app](https://railway.app/) using GitHub.
2. Click **New Project** $\to$ **Provision MySQL**.
3. Railway immediately provisions a MySQL 8.0 instance with a persistent volume.
4. Under the MySQL service **Variables** tab, note the credentials:
   - `MYSQLDATABASE`
   - `MYSQLUSER`
   - `MYSQLPASSWORD`
   - `MYSQLHOST`
   - `MYSQLPORT`
   - `MYSQL_URL` (Private network connection string)

### Step 2: Add ShopSphere Web Service
1. In the same Railway project, click **+ New** $\to$ **GitHub Repo**.
2. Select your `ShopSphere` repository.
3. Railway detects the `Dockerfile` automatically and begins the multi-stage build.

### Step 3: Configure Environment Variables
In the ShopSphere service settings, add the following environment variables:
```ini
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?createDatabaseIfNotExist=true&useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=${{MySQL.MYSQLUSER}}
DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}
```
*(Railway automatically resolves `${{MySQL...}}` template variables between services).*

### Step 4: Health Check & Domain Configuration
1. Under **Settings** $\to$ **Healthcheck Path**, enter `/actuator/health`.
2. Under **Networking**, click **Generate Domain** (e.g., `shopsphere-production.up.railway.app`).
3. Railway automatically issues a Let's Encrypt SSL/TLS certificate.

---

## 4. Alternative $0 Cost Deployment Path: Render + Aiven

For a completely free continuous deployment without a trial credit limit:

1. **Aiven (Free MySQL)**:
   - Create a free account at [aiven.io](https://aiven.io/).
   - Create a free MySQL service (Free tier includes 1 node, 5 GB storage).
   - Copy the Service URI (`mysql://avnadmin:password@host:port/defaultdb?ssl-mode=REQUIRED`).
2. **Render (Free Web Service)**:
   - Create a free account at [render.com](https://render.com/).
   - Click **New Web Service** $\to$ Connect your GitHub repo.
   - Choose **Docker** as environment (Render builds the multi-stage `Dockerfile`).
   - Add environment variables:
     - `SPRING_PROFILES_ACTIVE=prod`
     - `DB_URL=jdbc:mysql://<aiven-host>:<aiven-port>/defaultdb?useSSL=true&serverTimezone=UTC`
     - `DB_USERNAME=avnadmin`
     - `DB_PASSWORD=<aiven-password>`
   - Set Health Check Path to `/actuator/health`.
   - Click **Create Web Service**.

---

## 5. Production Post-Deployment Verification Checklist

Once the cloud service reports `UP`:

- [ ] **Health Probe Check**:
  - Visit `https://<your-app-domain>/actuator/health`.
  - Expect: HTTP 200 with `{"status":"UP"}`.
- [ ] **OpenAPI / Swagger Check**:
  - Visit `https://<your-app-domain>/swagger-ui.html`.
  - Expect: Interactive API documentation rendered with all schemas.
- [ ] **Storefront Catalog Check**:
  - Visit `https://<your-app-domain>/index.html`.
  - Expect: Seeded categories (Electronics, Audio, Wearables) and products rendered with pricing and stock.
- [ ] **Security Headers Check**:
  - Run `curl -I https://<your-app-domain>/api/products`.
  - Verify headers: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Content-Security-Policy`.
- [ ] **Customer Registration & Session Login**:
  - Register a new customer via `/register.html`.
  - Log in via `/login.html` and verify navbar displays user badge.
- [ ] **End-to-End Checkout Test**:
  - Add item to cart $\to$ Proceed to `/checkout.html` $\to$ Complete simulated payment.
  - Verify confirmation receipt and verify stock decreased on catalog.
- [ ] **Admin Portal & Data Isolation**:
  - Log in as admin (`admin@shopsphere.com` / `Admin@12345`).
  - Visit `/admin.html` and verify KPI cards, daily sales charts, and inventory restock modal.
- [ ] **Change Production Admin Password**:
  - Update seeded admin password immediately after verification!

---

## 6. Backup, Rollback & Disaster Recovery

1. **Database Automated Backups**:
   - Cloud providers (Railway / Aiven / AWS) create automatic daily snapshots of the MySQL volume.
   - Manual snapshot: Run `mysqldump -h <host> -u <user> -p <database> > backup_$(date +%F).sql`.
2. **Application Rollback**:
   - If an unexpected error occurs in a new deployment, both Railway and Render provide an instant **Rollback** button to revert to the previous verified container image without rebuilding.
3. **Database Schema Safeguards**:
   - All migrations are strictly versioned (`V1` to `V5`).
   - Production profile uses `hibernate.ddl-auto=validate`, preventing Hibernate from modifying or dropping database tables.
