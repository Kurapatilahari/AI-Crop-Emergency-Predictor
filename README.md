# 🌾 AI Crop Emergency Predictor — Full-Stack Java Project

*Seeing the storm before the field does.*

Spring Boot 3 · Spring Security + JWT · MySQL · React (Vite)

## What it does (mapped to the abstract)

| Abstract | Implementation |
|---|---|
| Weather + soil + NDVI data streams | `SensorReading` (soil moisture, pH, salinity, N-P-K, NDVI, temp, humidity, rain, wind) + live 3-day forecast from Open-Meteo (free, no key) |
| Data cleaned, aligned by time, fused per field | All readings are stored per field with timestamps; the engine fuses the latest readings with the weather outlook |
| Derived stress indicators | Moisture deficit, heat-stress index, NDVI decline rate (time-series regression) — `PredictionEngine` |
| Predict *what*, *how severe*, *when* | Threat type (6 types), severity (Low/Moderate/Severe/Critical), days-to-impact |
| Green / Amber / Red risk | Score 0-100 → Green < 35 ≤ Amber < 65 ≤ Red |
| Recommendations | Per-threat advice (irrigation, drainage, nutrients, pest scouting …) |
| Alerts: app / SMS / voice, local language | In-app alerts stored in DB; SMS + voice dispatch hooks (`NotificationService`); messages in English, Hindi, Telugu |
| Learns from outcomes | Farmers mark "problem occurred / no problem" → per-threat sensitivity is retuned; accuracy shown on dashboard |
| Continuous monitoring | Scheduled job runs predictions for all fields daily (06:00) |
| Secure multi-user system | JWT authentication, BCrypt passwords, FARMER / ADMIN roles, per-user data isolation |

> **About the AI engine:** the engine is an explainable hybrid model (agronomic thresholds per crop +
> time-series trend regression + feedback-tuned weights). It runs with no external ML runtime, and is isolated
> in `PredictionEngine.evaluate(...)`. To use a trained **LSTM / XGBoost** model, expose it as a Python
> (FastAPI) service and call it from `PredictionService` in place of the engine — the rest of the app stays unchanged.
> Sensor / satellite NDVI feeds are entered manually or **simulated** (there is no hardware in a demo).

## Project structure

```
crop-emergency-predictor/
├── backend/                       Spring Boot API
│   ├── pom.xml
│   └── src/main/java/com/agri/cropemergency/
│       ├── config/       SecurityConfig, DataInitializer (seed data)
│       ├── security/     JwtService, JwtAuthFilter, AppUserDetailsService
│       ├── model/        User, FarmField, SensorReading, Prediction, Alert, ModelWeight, enums
│       ├── repository/   Spring Data JPA repositories
│       ├── dto/          Request / response records
│       ├── service/      PredictionEngine, PredictionService, WeatherService, SimulationService,
│       │                 AlertService, NotificationService, PredictionScheduler, CropProfiles
│       ├── controller/   Auth, Field, Reading, Prediction, Alert, Dashboard, Admin
│       └── exception/    GlobalExceptionHandler
├── frontend/                      React (Vite) UI
│   └── src/ (pages: Login, Register, Dashboard, Fields, FieldDetail, Alerts, Admin)
└── database/setup.sql             (optional)
```

## Prerequisites

* Java 17 or newer (`java -version`)
* Maven 3.8+ (`mvn -v`)
* Node.js 18+ and npm (`node -v`)
* MySQL 8 running on `localhost:3306`

## Run it

### 1. Database
The database `crop_emergency_db` and all tables are created automatically.
Just make sure MySQL is running and set your credentials (defaults: user `root`, password `root`).

```bash
# Linux / macOS
export DB_USERNAME=root
export DB_PASSWORD=your_mysql_password

# Windows (PowerShell)
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
```
(or edit `backend/src/main/resources/application.properties`)

### 2. Backend (port 8080)
```bash
cd backend
mvn clean spring-boot:run
```
First start seeds the model weights, an admin, a demo farmer and 3 demo fields with predictions.

### 3. Frontend (port 5173)
```bash
cd frontend
npm install
npm run dev
```
Open **http://localhost:5173**

### Demo logins
| Role | Email | Password |
|---|---|---|
| Farmer | farmer@agri.com | Farmer@123 |
| Admin | admin@agri.com | Admin@123 |

### Production build
```bash
cd backend && mvn clean package -DskipTests && java -jar target/crop-emergency-predictor-1.0.0.jar
cd frontend && npm run build        # static files in frontend/dist
```
Set `JWT_SECRET` (Base64, ≥ 32 bytes) and `app.cors.allowed-origins` for your real domain.

## Try the flow
1. Log in as the farmer → dashboard shows Green / Amber / Red fields.
2. Open a field → pick a scenario (e.g. *Heat wave*) → **Simulate & predict** → risk, severity, days-to-impact, recommendation and charts appear.
3. Or **+ Add reading** with your own values, then **Run AI prediction**.
4. In *Prediction history*, press **Problem occurred / No problem** → model sensitivity updates (see Admin → *Learned model sensitivity*).
5. *Alerts* page shows in-app alerts; Red alerts also log simulated SMS + voice messages in the backend console.
6. Register a new user with language Hindi/Telugu to see localized alerts.

## REST API (all except `/api/auth/*` need `Authorization: Bearer <token>`)

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/auth/register` , `/api/auth/login` | Create account / get JWT |
| GET | `/api/auth/me` | Current user |
| GET/POST | `/api/fields` | List / create fields |
| GET/PUT/DELETE | `/api/fields/{id}` | Read / update / delete a field |
| GET/POST | `/api/fields/{id}/readings` | Sensor readings (chart-ready) / add reading |
| POST | `/api/fields/{id}/simulate` | `{ "scenario": "DROUGHT", "days": 10 }` |
| GET | `/api/fields/{id}/weather` | 3-day forecast |
| POST | `/api/fields/{id}/predict` | Run the AI engine |
| GET | `/api/fields/{id}/predictions` | Prediction history |
| POST | `/api/predictions/{id}/feedback` | `{ "outcome": "PROBLEM_OCCURRED" }` |
| GET | `/api/alerts`, `/api/alerts/unread-count` | Alerts |
| POST | `/api/alerts/{id}/read`, `/api/alerts/read-all` | Mark read |
| GET | `/api/dashboard` | Summary for the logged-in farmer |
| GET | `/api/admin/users`, `/model`, `/stats` · DELETE `/users/{id}` | Admin only |

Quick test with curl:
```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"farmer@agri.com","password":"Farmer@123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/dashboard
```

## Troubleshooting
* **`Access denied for user 'root'`** → set `DB_PASSWORD` correctly.
* **`Communications link failure`** → MySQL isn't running / wrong port.
* **Frontend shows "Cannot reach the server"** → backend isn't running, or change `VITE_API_URL` in `frontend/.env`.
* **Weather panel says unavailable** → no internet; predictions still work from sensor data.
* **Port in use** → change `server.port` (backend) or `server.port` in `frontend/vite.config.js`.
