# ShopSphere — Deployment & Production Guide

## 1. Hosting Environment & Options Evaluation

When deploying a full-stack Spring Boot + MySQL application online, several modern options exist. Below is a comparative assessment:

| Provider | Java / Spring Boot Support | MySQL Support | Free Tier / Low Cost | Recommended For |
|---|---|---|---|---|
| **Render** | Native Docker / Java Web Service | Managed PostgreSQL (Free tier), MySQL via Docker or external service (Aiven / PlanetScale) | Generous trial / $7/mo | Fast demo deployment |
| **Railway** | Native Docker / Nixpacks | Native 1-click Managed MySQL container | $5/mo usage credit | **Top Pick for Full Monolith + MySQL** |
| **Fly.io** | Native Docker containers | MySQL via Fly Postgres/LiteFS or external MySQL | Free allowance with CC | High-performance edge deployment |
| **AWS App Runner + RDS** | Containerized Web App | Aurora / RDS MySQL | Free tier for 12 mos | Enterprise-grade production |
| **Self-Hosted VPS (Hetzner / DigitalOcean)** | Docker Compose (App + MySQL + Nginx) | Docker MySQL container with volume | $4 - $6/month flat | Complete operational control |

---

## 2. Docker & Containerization Plan

### Multi-Stage `Dockerfile`
```dockerfile
# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=build /app/target/shopsphere-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
```

### `docker-compose.yml` (Local & VPS Staging)
```yaml
version: '3.8'

services:
  mysql-db:
    image: mysql:8.0
    container_name: shopsphere-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: root_password_change_me
      MYSQL_DATABASE: shopsphere_db
      MYSQL_USER: shopsphere_user
      MYSQL_PASSWORD: shopsphere_secret_change_me
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  shopsphere-app:
    build: .
    container_name: shopsphere-app
    restart: always
    depends_on:
      - mysql-db
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:mysql://mysql-db:3306/shopsphere_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
      DB_USERNAME: shopsphere_user
      DB_PASSWORD: shopsphere_secret_change_me

volumes:
  mysql_data:
```

---

## 3. Environment Variables Reference

| Variable Name | Description | Default / Example |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev`, `prod` |
| `DB_URL` | JDBC Connection URL | `jdbc:mysql://localhost:3306/shopsphere_db` |
| `DB_USERNAME` | MySQL database user | `root` or `shopsphere_user` |
| `DB_PASSWORD` | MySQL database password | Supplied securely via env |
| `SERVER_PORT` | Application HTTP port | `8080` |
| `APP_ADMIN_EMAIL` | Default administrator account email | `admin@shopsphere.com` |
| `APP_ADMIN_PASSWORD` | Default administrator password | Secure secret |

---

## 4. Pre-Deployment & Rollback Checklist

### Pre-Deployment Verification
- [ ] Run full automated test suite: `mvn clean verify`.
- [ ] Ensure all Flyway migration scripts in `src/main/resources/db/migration/` are strictly additive and version-stamped (`V1__...`, `V2__...`).
- [ ] Ensure `ddl-auto` is set to `validate` or `none` in production profile (Flyway owns schema).
- [ ] Verify CSRF protection is active.
- [ ] Check logs for absence of plain-text passwords or secret keys.

### Rollback Strategy
1. **Application Rollback**: Revert deployment container to previous stable Docker image tag.
2. **Database Rollback**: Flyway migrations must be designed to avoid destructive operations (e.g., deprecate columns before dropping). Maintain pre-deployment automated database snapshots.
