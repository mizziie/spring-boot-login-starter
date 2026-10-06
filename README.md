# spring-boot-login-starter

ตัวอย่างโครงสร้าง Spring Boot 3 สำหรับระบบ Login พร้อม:

- **Spring Security** สำหรับ authentication/authorization
- **Spring Session + Redis** สำหรับจัดเก็บ session แบบ distributed
- **Spring Data JPA + PostgreSQL** สำหรับเก็บข้อมูล user/profile
- **Flyway** สำหรับควบคุม database schema
- **Docker Compose** สำหรับรัน app + database + redis
- **Default admin user** ที่กำหนดผ่าน environment variables
- **Rate limiting** ป้องกัน brute force ที่ login
- **Audit logging** บันทึกการ login/logout
- **Email verification** สำหรับการ register
- **JWT + Refresh Token** สำหรับ mobile/SPA

---

## โครงสร้างเทคโนโลยี

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 3.3.x, Java 21 |
| Build Tool | Maven |
| Auth | Spring Security |
| Session | Spring Session Data Redis |
| Stateless Auth | JWT + Refresh Token |
| Database | PostgreSQL 16 |
| Migration | Flyway |
| Cache/Session Store | Redis 7 |
| Docs | OpenAPI / Swagger |

---

## วิธีใช้งาน

### 1. Clone และเข้าไปในโฟลเดอร์โปรเจกต์

```bash
cd java-login-starter
```

### 2. ตั้งค่า environment variables

คัดลอกจาก `.env.example` แล้วแก้ไขตามต้องการ:

```bash
cp .env.example .env
```

ตัวอย่าง `.env`:

```env
POSTGRES_DB=javalogin
POSTGRES_USER=javalogin
POSTGRES_PASSWORD=changeme_db

ADMIN_USERNAME=admin
ADMIN_PASSWORD=changeme_admin
ADMIN_EMAIL=admin@example.com

SPRING_PROFILES_ACTIVE=docker

JWT_SECRET=change-me-to-a-very-long-random-secret-key-32chars
JWT_ACCESS_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=604800000

EMAIL_VERIFICATION_REQUIRED=false
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_FROM=noreply@example.com

RATE_LIMIT_MAX_ATTEMPTS=5
RATE_LIMIT_ATTEMPT_WINDOW_SECONDS=300
RATE_LIMIT_LOCKOUT_DURATION_SECONDS=900
```

> **คำเตือน**: เปลี่ยน `ADMIN_PASSWORD` ทันทีใน production อย่าใช้ default password

### 3. รันด้วย Docker Compose

```bash
docker compose up --build
```

รอจน service ทั้งหมดพร้อม จากนั้น API จะพร้อมใช้งานที่ `http://localhost:8080`

---

## API Endpoints

### Auth

| Method | Endpoint | คำอธิบาย |
|--------|----------|---------|
| POST | `/api/auth/register` | สมัครสมาชิก (ส่ง verification email) |
| GET | `/api/auth/verify-email?token=...` | ยืนยันอีเมล |
| POST | `/api/auth/login` | เข้าสู่ระบบ สร้าง session |
| POST | `/api/auth/token` | ขอ JWT access + refresh token |
| POST | `/api/auth/refresh` | refresh access token |
| POST | `/api/auth/logout` | ออกจากระบบ |

### User

| Method | Endpoint | คำอธิบาย |
|--------|----------|---------|
| GET | `/api/users/me` | ดู profile ตัวเอง (ต้อง login) |
| GET | `/api/users` | ดูรายชื่อ user ทั้งหมด (ต้อง login) |

### Admin

| Method | Endpoint | คำอธิบาย |
|--------|----------|---------|
| GET | `/api/admin/dashboard` | ตัวอย่าง endpoint เฉพาะ admin |

### Docs & Health

| URL | คำอธิบาย |
|-----|---------|
| `/swagger-ui.html` | Swagger UI |
| `/v3/api-docs` | OpenAPI JSON |
| `/actuator/health` | Health check |

---

## ตัวอย่างการเรียก API

### Login (Session/Cookie)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  -c cookies.txt
```

### ดู Profile ตัวเอง

```bash
curl -X GET http://localhost:8080/api/users/me \
  -b cookies.txt
```

### Register

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"password123","email":"john@example.com","fullName":"John Doe"}'
```

### ขอ JWT Token (สำหรับ mobile/SPA)

```bash
curl -X POST http://localhost:8080/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### เรียก API ด้วย JWT

```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <access_token>"
```

### Refresh Token

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refresh_token>"}'
```

### Logout

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -b cookies.txt
```

---

## การรันแบบ Local Development (ไม่ใช้ Docker)

ต้องมี PostgreSQL และ Redis รันอยู่ในเครื่อง จากนั้น:

```bash
./mvnw spring-boot:run
```

ค่า default ใน `application.yml` จะเชื่อมต่อ `localhost:5432` และ `localhost:6379`

---

## Build

```bash
./mvnw clean package
```

หรือบน Windows:

```bash
mvnw.cmd clean package
```

---

## การทดสอบ

```bash
./mvnw test
```

หรือรันทั้ง build + test:

```bash
./mvnw clean package
```

### Test Coverage

โปรเจกต์มี unit tests ครอบคลุบหลักๆ ดังนี้:

| ไฟล์ | สิ่งที่ทดสอบ |
|------|------------|
| `AuthControllerTest` | register, login success/failure, token generation, refresh token, verify email, logout, email not verified denial, rate limiting block |
| `JwtServiceTest` | generate token, validate token, malformed token, extract username, expiration config |
| `UserServiceTest` | register user, duplicate username/email, get current user, get all users, find by username, create/skip admin seed |
| `RefreshTokenServiceTest` | create token, find valid/expired token, delete by user, expiration check |
| `LoginAttemptServiceTest` | block detection, failure recording, success reset, window expiry |
| `UserDetailsServiceImplTest` | load user by username, user not found, role mapping |

---

## คำแนะนำเพิ่มเติมสำหรับ Production

- เปลี่ยน **JWT_SECRET** ให้เป็น strong random key ขั้นต่ำ 32 ตัวอักษร
- เปลี่ยน default admin password ทันที
- เปิดใช้งาน HTTPS และตั้งค่า `server.servlet.session.cookie.secure=true`
- ตั้งค่า **CORS** ให้เหมาะสมกับ frontend
- ใช้ **SMTP จริง** สำหรับ email verification (ปรับ `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`)
- เก็บ secrets และ password ใน secret manager ไม่ใช่ใน repository
- ปรับ **rate limiting** ให้ persistent ข้าม restart (เช่น ใช้ Redis หรือ Bucket4j)
- พิจารณาเพิ่ม **IP-based rate limiting** และ captcha สำหรับ login
- ตั้งค่า `EMAIL_VERIFICATION_REQUIRED=true` หากต้องการบังคับยืนยันอีเมลก่อน login

---

## License

MIT
