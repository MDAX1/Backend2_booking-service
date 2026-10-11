# Bokningstjänsten

Bokningstjänsten i pensionatets microservice-uppdelning. Tjänsten äger **rum** och **bokningar**
och har ingen egen kundtabell: `customerId` sparas bara som ett tal, och kunden slås upp över REST
hos kundtjänsten när den behövs.

Tjänsten är ett self-contained system med egen databas, egen affärslogik och eget gränssnitt
(Thymeleaf). Ingen annan tjänst läser i bokningsdatabasen.

## Kom igång

Repot har ingen egen `docker-compose.yml`. Hela systemet med tre tjänster och tre databaser
startas från kundtjänstens repo, vars compose bygger den här tjänsten från en syskonklon:

```text
IdeaProjects/
├── Backend2_customer-service/      # docker compose up --build körs härifrån
├── Backend2_booking-service/       # det här repot
└── backend2_notification-service/
```

Mappnamnen måste vara exakt dessa, eftersom kundtjänstens `docker-compose.yml` bygger
syskonreporna på relativa sökvägar.

```bash
cd ../Backend2_customer-service
cat > .env <<'ENV'
JWT_SECRET=<openssl rand -base64 32>
ADMIN_PASSWORD=<valfritt lösenord>
ENV
docker compose up --build
```

Bokningstjänsten körs på <http://localhost:8081> och har egen databas (`bookingdb`).
Logga in på `/login` med samma `admin` och lösenord som i kundtjänsten; inloggningen
vidarebefordras till kundtjänstens `POST /api/auth/login` och token sparas i sessionen.

`JWT_SECRET` måste vara **identisk** i alla tre tjänsterna, annars underkänns varandras tokens.

### Utveckling

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw spring-boot:run
```

Utan `SPRING_DATASOURCE_URL` går tjänsten mot `localhost:5432/bookingdb` (eller `PGHOST`,
`PGPORT`, `PGDATABASE` om de är satta). Starta en egen databas om du kör utanför Docker:

```bash
docker run --rm -p 5432:5432 \
  -e POSTGRES_DB=bookingdb -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
  postgres:17-alpine
```

### Tester

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw test
```

22 tester, varav 6 integrationstester i `BookingApiIntegrationTest` som går genom hela kedjan:
säkerhetsfilter, controller, service, repository och databas. Testerna kör mot en H2-databas i
minnet, så Docker behövs inte. `BookingApiIntegrationTest` stubbas med `MockRestServiceServer`,
så varken kundtjänsten eller notifieringstjänsten behöver vara igång.

## API

`/api/**` svarar JSON och kräver `Authorization: Bearer <token>`. Token hämtas från kundtjänstens
`POST /api/auth/login`.

| Metod | Path | Svar |
|---|---|---|
| GET | `/api/bookings/count?customerId={id}&status=ACTIVE` | 200 `{count}` · 400 |
| POST | `/api/bookings` | 201 + bokningen · 400 · 401 · 409 · 503 |

`GET /api/bookings/count` används av kundtjänsten innan en kund raderas. Utan `status` räknas
alla bokningar; med `status=ACTIVE` räknas bara bokningar som inte checkat ut ännu.

```bash
curl -X POST http://localhost:8081/api/bookings \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"customerId": 1, "roomId": 5, "checkIn": "2026-09-10", "checkOut": "2026-09-12", "numberOfGuests": 2}'
```

Fel returneras som `application/problem+json` med ett maskinläsbart `errorCode`:

| Läge | Status | `errorCode` |
|---|---|---|
| Ogiltigt fält eller ogiltigt datumintervall | 400 | `VALIDATION_FAILED` |
| Kunden finns inte i kundtjänsten | 400 | `VALIDATION_FAILED` |
| Bokningen finns inte | 404 | `NOT_FOUND` |
| Rummet är upptaget | 409 | `ROOM_ALREADY_BOOKED` |
| Token saknas eller är ogiltig | 401 | (tom kropp) |
| Kundtjänsten svarar inte | 503 | `CUSTOMER_SERVICE_UNAVAILABLE` |

## Webbsidor

Vanliga HTML-formulär kan inte skicka en `Authorization`-header. `SessionBearerTokenResolver`
läser därför token från headern om den finns, annars från sessionen som sätts vid `/login`.

| Path | Beskrivning |
|---|---|
| `/` | Startsida |
| `/bookings` | Bokningslista |
| `/bookings/new` | Skapa bokning |
| `/bookings/{id}/edit` | Ändra bokning |
| `/bookings/{id}/delete` | Avboka (POST) |
| `/bookings/search` | Sök lediga rum på datum och antal personer |
| `/rooms`, `/rooms/new`, `/rooms/{id}/edit` | Rumshantering |
| `/login`, `/logout` | Inloggning mot kundtjänsten |
| `/customers` | Skickar vidare till kundtjänstens kundsida med token |

## Tillgänglighet och dubbelbokningar

Vid varje bokning kontrolleras att rummet är ledigt över det valda intervallet:

```java
countByRoomIdAndCheckInBeforeAndCheckOutAfter(roomId, checkOut, checkIn)
```

Vid uppdatering av en bokning används `...AndIdNot(...)` så att bokningen inte krockar med sig
själv. Rummet måste också ha kapacitet för `numberOfGuests`.

## Beroenden till de andra tjänsterna

Innan en bokning sparas verifieras kunden:

```
GET {CUSTOMER_SERVICE_URL}/api/customers/{id}
Authorization: Bearer <anroparens token>
```

Saknas kunden, eller är den mjukraderad, avvisas bokningen. Är kundtjänsten nere blir svaret
**503** och ingen bokning sparas: tjänsten misslyckas stängt.

Efter att bokningen sparats skickas en bekräftelse till notifieringstjänsten:

```
POST {NOTIFICATION_SERVICE_URL}/api/notifications
```

Det anropet är **best effort**. Är notifieringstjänsten nere loggas en varning och bokningen
ligger kvar, eftersom en utebliven bekräftelse inte ska riva en genomförd bokning.

## Databas

Egen Postgres, ingen annan tjänst läser i den. Schemat skapas av Hibernate
(`spring.jpa.hibernate.ddl-auto=update`). Tabellerna är `rooms` och `bookings`;
`bookings.customer_id` är ett vanligt tal utan främmande nyckel mot kundtjänstens databas.

`DataInitializer` seedar tolv rum (enkel- och dubbelrum med extrasängar) samt några demo-bokningar
vid uppstart, men bara om `rooms` är tom. Utan det skulle bokningsformuläret vara tomt första
gången systemet startar.

## Miljövariabler

| Variabel | Standard | Beskrivning |
|---|---|---|
| `PORT` | `8081` | |
| `PGHOST` / `PGPORT` / `PGDATABASE` | `localhost` / `5432` / `bookingdb` | |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://${PGHOST}:${PGPORT}/${PGDATABASE}` | överstyr PGHOST m.fl. |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | |
| `JWT_SECRET` | — | 32 byte Base64, identisk i alla tjänster |
| `CUSTOMER_SERVICE_URL` | `http://localhost:8080` | |
| `NOTIFICATION_SERVICE_URL` | `http://localhost:8082` | |

## JWT

Tjänsten utfärdar inga egna tokens, den validerar dem. Kundtjänsten signerar med HS256 och samma
`JWT_SECRET`, och `JwtConfig` kontrollerar signatur, utgångstid, `iss` och `aud`.

Läsoperationer är öppna, medan allt som skapar eller ändrar data kräver en giltig token:
`POST /bookings`, `POST /bookings/{id}`, `POST /bookings/{id}/delete` och `POST /api/bookings`.
`/api/**` svarar **401** vid saknad token, medan webbsidorna skickar vidare till `/login`.

## Kubernetes

`k8s/` innehåller `booking-service.yaml` och `booking-db.yaml` med Deployment, Service, PVC,
probes och JWT-hemligheten från `pensionat-secrets`. De är kopior av manifesten i kundtjänstens
repo, där hela systemet ligger samlat. Skapa secret och kör:

```bash
./k8s/create-secret.sh   # finns i kundtjänstens repo
kubectl apply -f k8s/
```

## Docker

`Dockerfile` bygger jar-filen i ett Maven-steg och kör den på `eclipse-temurin:21-jre-alpine`.
Tjänsten lyssnar på `8081` och healthchecken går mot `/actuator/health/readiness`.
`railway.json` pekar på samma healthcheck för deployment på Railway.
