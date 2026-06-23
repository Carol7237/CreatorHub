# NEXT_STEPS.md — Planul pentru microservicii (Faza 8+)

> **Citește întâi `CLAUDE.md`** (mai ales blocul „STARE CURENTĂ" din §1, porturile
> din §2, lecțiile de tooling din §3bis, și fundația de securitate §11/§14). Acest
> fișier descrie EXACT ce urmează după monolit. Monolitul e COMPLET pe `main`.

---

## 0. Obiectivul

Transformăm monolitul Spring Boot (`/src`, complet, pe `main`) într-o arhitectură
de **microservicii cu Spring Cloud**. Ținta de notă: **8–9** (cerințele obligatorii
= ~60% sunt deja livrate de monolit). Cerințele OPȚIONALE vizate acum:

- **Config Server** (configurare centralizată).
- **Eureka** (service discovery / registry).
- **API Gateway** (rutare unică, single entry point).
- **Load balancing** (Spring Cloud LoadBalancer prin Eureka).
- **Resilience4j** (circuit breaker, retry, fallback, timeout, bulkhead).
- **Monitoring**: Prometheus + Grafana (via Micrometer / actuator).
- **Securitate distribuită**: JWT PESTE fundația existentă (se refolosesc
  `CustomUserDetailsService` + `PasswordEncoder`; vezi CLAUDE.md §11 „JWT mai târziu").
- **NoSQL + caching**: Redis (cache) + MongoDB (ex. pentru serviciul de Notification).

---

## 1. Strategia: incremental, verificat la fiecare pas

- **Ramură git nouă `microservices`** (creată de sesiunea nouă la începutul Pasului 1).
  `main` (monolitul) rămâne INTACT ca backup și ca referință de logică.
- **Construiește incremental**: validează infrastructura ÎNAINTE de a muta logica
  reală. Nu muta tot deodată. Fiecare pas: cod → pornit → verificat → commit mic.
- **Refolosește din monolit**: entitățile, DTO-urile, regulile de business, securitatea
  există deja și sunt testate. Se copiază/mută în servicii, nu se rescriu de la zero.
- **Întreabă la decizii mari** (regula 3 din CLAUDE.md), mai ales: organizarea repo
  (vezi §6), granițele exacte între servicii, și strategia de date (vezi §3).

---

## 2. Arhitectura țintă (construită INCREMENTAL, nu toată odată)

| Componentă | Port | Rol |
|---|---|---|
| **Eureka Server** | 8761 | service registry / discovery |
| **Config Server** | 8888 | configurare centralizată (Git/native) |
| **API Gateway** | **8080** | single entry point, rutare, (mai târziu) auth JWT |
| **User Service** | 8081* | User, Profile, auth/security, admin |
| **Content Service** | 8082 | Post, Comment, Tag (+ gating premium) |
| **Subscription Service** | 8083 | SubscriptionTier, Subscription |
| **Notification Service** | 8084 | notificări (NoSQL/MongoDB candidat) |

\* Atenție la conflictul de port: monolitul folosește 8081. În microservicii,
gateway-ul ia **8080** (e liber? în 7A am notat că Oracle TNS putea ocupa 8080 —
**verifică `Get-NetTCPConnection -LocalPort 8080`** la început; dacă e ocupat,
alege alt port pentru gateway și documentează). Serviciile interne pot fi pe
porturi dinamice (`server.port=0`) + Eureka, ca să nu ne batem capul cu porturi.

**Maparea monolit → servicii** (granițele de domeniu):
- **User Service** ← `User`, `Profile`, `model.enums.Role`, securitatea (login, register,
  `/api/auth/**`, `/api/creators/**`, `/api/profiles/**`, `/api/admin/**`).
- **Content Service** ← `Post`, `Comment`, `Tag` (`/api/posts/**`, `/api/comments/**`,
  `/api/tags/**`). **Aici stă gating-ul premium** → are nevoie să afle dacă un viewer
  are abonament activ (apel către Subscription Service, sau cache).
- **Subscription Service** ← `SubscriptionTier`, `Subscription` (`/api/tiers/**`,
  `/api/subscriptions/**`).
- **Notification Service** ← NOU (domeniul zice „fanii primesc notificări"); bun
  candidat pentru **MongoDB** + eveniment la abonare/postare nouă.

---

## 3. Decizia pe baze de date (ECHILIBRATĂ — important)

Monolitul are **relații FK strânse** (`Post.author→User`, `Subscription.fan→User`,
`Subscription.tier→Tier`, `Comment.author/post`, `Profile→User`). NU le sparge orbește
în apeluri de rețea — ar fi lent și fragil.

**Abordare recomandată (de confirmat cu utilizatorul):**
- **Schema-per-service în ACELAȘI PostgreSQL** (sau database-per-service unde e curat).
  Fiecare serviciu deține tabelele lui; granițele logice sunt clare, dar evităm
  complexitatea operațională a N baze fizice.
- **Referințe cross-service prin ID** (nu FK fizic cross-schema): ex. Content ține
  `author_id` (Long) ca referință la User Service, fără FK la nivel de DB. Datele
  „de afișare" (username, displayName) se iau prin apel către User Service (cu cache)
  SAU se denormalizează minimal.
- **Gating premium** (cazul cel mai delicat): Content Service trebuie să știe dacă
  viewer-ul are abonament activ la tier-ul postării → apel către Subscription Service
  (`existsActive(fanId, tierId)`), idealMENTE cu **Redis cache** pe răspuns + circuit
  breaker (Resilience4j) + fallback (dacă Subscription Service e jos → tratează ca „fără
  acces", nu crăpa).
- **MongoDB** pentru Notification Service (date de tip event/feed, nu relaționale).

> Decizia finală de date se ia la începutul mutării logicii (după ce infra merge).
> Pentru PASUL 1 (infra) NU atingem baze de date reale.

---

## 4. PASUL 1 — DOAR infrastructura ✅ COMPLET (2026-06-23)

> **STARE: GATA ȘI VALIDAT.** Eureka + Gateway + Probe Service rulează și
> test-cheie trece. Vezi rezultatele la finalul secțiunii. Următorul = §5 Pasul 2.

**Scop:** validăm că **discovery (Eureka) + routing (Gateway)** funcționează, ÎNAINTE
de a muta orice logică reală. Fără DB, fără business.

1. Creează ramura: `git checkout -b microservices`.
2. **Decide organizarea repo cu utilizatorul** (vezi §6) — blocant pentru structură.
3. **Eureka Server** (`spring-cloud-starter-netflix-eureka-server`), `@EnableEurekaServer`,
   port **8761**, `application.yml` (nu se înregistrează pe el însuși).
4. **API Gateway** (`spring-cloud-starter-gateway`), port **8080**, înregistrat în Eureka,
   cu o rută către serviciul de probă (`lb://probe-service`).
5. **Probe Service** minimal (`spring-boot-starter-web` + eureka client), un singur
   endpoint `GET /api/probe/hello` → `"hello from probe"`, înregistrat în Eureka.
6. **Spring Cloud BOM**: aliniază versiunea Spring Cloud cu Spring Boot **3.5.x**
   (ex. Spring Cloud **2024.0.x** / „Moorgate" — verifică compatibilitatea în Maven
   Central la momentul respectiv, exact ca la springdoc în 7A).

**TESTUL-CHEIE de validare (Pasul 1 e gata când):**
```
GET http://localhost:8080/api/probe/hello
→ rutează prin Gateway → rezolvă prin Eureka → ajunge la Probe Service → 200 "hello from probe"
```
Plus: Eureka dashboard la `http://localhost:8761` arată `GATEWAY` și `PROBE-SERVICE` UP.

Commit-uri mici: `feat(eureka): ...`, `feat(gateway): ...`, `feat(probe): ...`.

### ✅ REZULTATE PASUL 1 (2026-06-23)
- **Organizare repo (decizia §6 = A, aplicată sigur):** multi-module Maven cu parent
  `creatorhub-services` sub **`services/pom.xml`** (NU în rădăcina repo). Monolitul
  (`pom.xml` + `src/` din rădăcină) e **NEATINS** → `main` rămâne livrabilul sigur,
  schimbarea e reversibilă. Module: `eureka-server`, `api-gateway`, `probe-service`.
  *(Promovarea unui parent în rădăcina repo care adoptă monolitul ca modul = pas
  ulterior, mai riscant; amânat conform „varianta cea mai sigură".)*
- **Spring Cloud 2025.0.0** (verificat în Maven Central; trenul pt Boot 3.5.x).
  **CORECȚIE:** nota din §0/§4 spunea 2024.0.x/"Moorgate" — greșit, acela e pt Boot 3.4.x.
- **Porturi:** Eureka **8761**, Gateway **8085**, Probe **8091**. Conflict rezolvat:
  8080 ocupat de **Oracle TNS (TNSLSNR)** → gateway pe 8085; 8090 ocupat → probe pe 8091.
- **Gateway starter:** `spring-cloud-starter-gateway-server-webflux` (ne-deprecat în 2025.0.0).
- **TEST-CHEIE TRECUT:** `GET http://localhost:8085/api/probe/hello` → **200**
  `{"message":"hello from probe-service","port":8091,"service":"probe-service"}`
  (Gateway → Eureka `lb://probe-service` → probe). Eureka dashboard: ambele servicii UP.
- Build: `./mvnw -f services/pom.xml clean package` (din rădăcină). Detalii: CLAUDE.md §16.

---

## 5. Pașii următori (schiță — după ce infra din Pasul 1 e validată)

2. **Mută logica de business în servicii, PE RÂND** (nu tot odată). Sugerat:
   întâi **User Service** (cel mai independent), apoi **Subscription**, apoi
   **Content** (depinde de ambele pentru gating). Fiecare: mută entități/DTO/service/
   controller din monolit, conectează la DB (vezi §3), înregistrează în Eureka,
   rutează prin Gateway, verifică, commit.

   > ### ✅ PASUL 2 — User Service COMPLET (2026-06-23)
   > Primul serviciu real, migrat din monolit pe ramura `microservices` (monolitul
   > NEATINS). Detalii complete: **CLAUDE.md §17**. Pe scurt:
   > - **modul `services/common`** (tipuri partajate: excepții, `ApiErrorResponse`,
   >   `PagedResponse`, `PageableUtils`, `Viewer`, `GlobalExceptionHandler`) — decizia
   >   de cod partajat = **modul common**, nu duplicare.
   > - **`services/user-service`** (port **8092**, schema **`users_svc`**): `User`
   >   (decuplat de Post/Sub/Tier/Comment), `Profile`, auth complet (sesiune+BCrypt+
   >   CSRF+roluri). Gateway: `/api/auth|creators|profiles|admin/**` → `lb://user-service`.
   > - **§3 aplicat:** schema-per-service `users_svc`, fără FK cross-service.
   > - **probe-service eliminat** (și-a făcut treaba în Pasul 1).
   > - **Verificat:** 28 teste verzi; prin gateway register/login/me OK, admin 403(USER)/
   >   200(ADMIN), parolă BCrypt în `users_svc`.
   > - **RĂMAS de mutat:** Subscription (`tiers`, `subscriptions`), Content (`posts`,
   >   `comments`, `tags` + gating premium cross-service), Notification (MongoDB).
   >   Sugestie ordine: **Subscription** apoi **Content** (Content depinde de Subscription
   >   pentru gating). Refolosește pattern-ul din user-service (modul + common + Eureka +
   >   rută gateway + schema proprie). Monolitul se curăță DOAR la final.

   > ### ✅ PASUL 3 — Subscription + Content + gating rezilient COMPLET (2026-06-23)
   > Ambele servicii mutate împreună (Content depinde de Subscription pt gating). Detalii
   > complete: **CLAUDE.md §18**. Pe scurt:
   > - **`subscription-service`** (port **8093**, schema **`subs_svc`**): Tier (creatorId
   >   Long) + Subscription (fanId Long) + SubStatus; endpoint intern
   >   `GET /internal/subscriptions/access?fanId=&tierId=` (contractul de gating).
   > - **`content-service`** (port **8094**, schema **`content_svc`**): Post/Comment/Tag
   >   (authorId/tierId Long); gating premium = apel **OpenFeign** Content→Subscription,
   >   protejat de **Resilience4j circuit breaker + fallback FAIL-CLOSED** (post rămâne
   >   locked dacă Subscription e jos — `ResilienceConfig`, timeout 3s, prag 50%).
   > - **Identitate (Opțiunea 1, decisă cu utilizatorul):** gateway injectează
   >   `X-User-Id`/`X-User-Roles`/`X-User-Name` (rezolvând `/api/auth/me`), **ștergând**
   >   headerele venite din exterior (anti-spoofing). Downstream stateless + filtru
   >   header→SecurityContext (toate în `common`). Tranzitoriu pre-JWT.
   > - **Rute gateway:** `/api/tiers|subscriptions/**`→subscription; `/api/posts|comments|tags/**`→content.
   > - **Verificat prin gateway (8085):** gating (abonat vede / neabonat locked / autor+admin văd),
   >   circuit breaker (subscription oprit → locked, nu 500), anti-spoofing (X-User-Id fals → 401/ignorat),
   >   scheme separate. **61 teste verzi** (28 user + 14 subs + 19 content).
   > - **RĂMAS:** Notification Service (MongoDB) — vezi pașii 3-10 de mai jos.
3. **Config Server** (`spring-cloud-config-server`) — externalizează `application.yml`-urile
   (porturi, datasource, secrete) într-un repo de config (Git sau `native`).
4. **Resilience4j** pe apelurile cross-service (mai ales gating-ul premium): circuit
   breaker + retry + timeout + **fallback** (degradare grațioasă). Demonstrează cu un
   serviciu oprit → fallback, nu crash.
5. **Monitoring**: actuator + Micrometer + Prometheus (scrape `/actuator/prometheus`)
   + Grafana (dashboard-uri). Docker pentru Prometheus + Grafana.
6. **Redis** (cache) — pe gating premium / date de user „de afișare" / liste.
7. **MongoDB** — Notification Service (eveniment la abonare/postare nouă).
8. **JWT distribuit** — login-ul (User Service) emite JWT; Gateway/servicii validează
   JWT (filtru) în loc de sesiune. **Se refolosesc** `CustomUserDetailsService` +
   `PasswordEncoder` (CLAUDE.md §11). Sesiune → `STATELESS`. Frontend-ul trimite
   `Authorization: Bearer <jwt>` (adaptează interceptorul axios din `/frontend`).
9. **Docker Compose pentru TOT** (Eureka, Config, Gateway, servicii, Postgres, Redis,
   Mongo, Prometheus, Grafana) — un singur `docker compose up`.
10. **Deployment** (opțional, dacă ajungem).

La fiecare pas: actualizează `CLAUDE.md` §5 (progres) și acest fișier dacă planul
se schimbă.

---

## 6. ÎNTREBARE DESCHISĂ — ✅ REZOLVATĂ (2026-06-23)

> **DECIS de utilizator: varianta (A) multi-module Maven** cu parent pom comun.
> **Aplicare sigură:** parent-ul (`creatorhub-services`) stă sub `services/pom.xml`,
> nu în rădăcina repo, ca monolitul să rămână NEATINS (vezi §4 „REZULTATE" și
> CLAUDE.md §16). Adoptarea monolitului ca modul sub un parent în rădăcină = pas
> ulterior opțional. Decizia de date §3 (schema-per-service) rămâne pt Pasul 2+.

**Organizarea repo-ului pentru microservicii.** Două opțiuni; alege CU utilizatorul:

- **(A) Multi-module Maven** cu un `pom.xml` parent + module (`eureka-server/`,
  `gateway/`, `user-service/`, ...). Pro: build unificat, versiuni partajate, dependency
  management central. Contra: cuplare mai mare la build.
- **(B) Proiecte Maven independente** sub `services/` (fiecare cu `pom.xml`-ul lui).
  Pro: izolare reală, „cum sunt microserviciile în producție". Contra: build separat
  pe fiecare, fără parent comun.

Recomandare implicită: **(A) multi-module** pentru un proiect de facultate (mai ușor
de build-uit/demonstrat), DAR întreabă utilizatorul — e o decizie structurală majoră.

Alte întrebări de clarificat la nevoie: granițele exacte User↔Content↔Subscription;
unde trăiește gating-ul premium; dacă păstrăm monolitul rulabil în paralel în timpul
migrării.

---

## 7. Riscuri & note

- **Versiuni Spring Cloud**: trebuie aliniate la Spring Boot 3.5.x — verifică în Maven
  Central (Spring Cloud release train compatibil), nu ghici (lecția din 7A cu springdoc).
- **Gating premium cross-service** = cea mai grea piesă (Content depinde de Subscription).
  Proiecteaz-o cu cache + circuit breaker de la început.
- **Frontend-ul** (`/frontend`) consumă acum `/api/**` prin proxy către `:8081`. La
  microservicii, ținta proxy-ului devine **Gateway-ul (`:8080`)**; adaptează
  `vite.config.ts` + (la JWT) interceptorul axios.
- **Nu strica `main`.** Dacă microserviciile merg prost, monolitul rămâne livrabilul sigur.
