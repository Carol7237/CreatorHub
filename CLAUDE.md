# CLAUDE.md — Ghidul proiectului CreatorHub

> Acesta este documentul de memorie al proiectului. **Citește-l integral la
> începutul fiecărei sesiuni** înainte să modifici cod. Actualizează-l când
> termini o fază sau iei o decizie importantă.

## 1. Proiectul

**CreatorHub** — platformă de tip creator-subscriptions (schelet în spiritul
Patreon / Fanvue). Creatorii publică conținut gratuit și premium; fanii se
abonează pe tiers plătite, accesează conținut premium, comentează și primesc
notificări.

Proiect de facultate (Aplicații Web cu Arhitectură de Microservicii), construit
la **calitate de producție** fiindcă va deveni un produs real.

**Strategie: monolit întâi, microservicii la final.** Am construit mai întâi un
monolit Spring Boot curat și bine structurat; acum urmează spargerea în
microservicii.

> ## ⭐ STARE CURENTĂ (handoff — citește asta primul)
> **Monolitul Spring Boot e COMPLET și funcțional pe ramura `main`**, working tree
> curat. Toate cele **8 cerințe obligatorii** sunt livrate (model JPA, CRUD+service
> layer, multi-environment, Spring Security, paginare/sortare+logging, testare,
> REST API+validare, frontend React) — **~60% din notă livrat**. **105 teste verzi**
> pe H2 (`./mvnw clean verify`), coverage service **87% line**. Frontend React (SPA
> cyberpunk) în `/frontend`, verificat end-to-end.
>
> **Următorul pas = Faza 8: microservicii (Spring Cloud), țintă notă 8–9.** Planul
> complet, incremental, e în **[`NEXT_STEPS.md`](NEXT_STEPS.md)** — citește-l înainte
> să începi. Microserviciile se construiesc pe o **ramură nouă `microservices`**
> (NU pe `main`); monolitul de pe `main` rămâne intact ca backup. **Nu sparge
> monolitul de pe `main`.**

## 2. Stack tehnologic

### Țintă finală (unde ajungem)
- Java 21, Spring Boot 3.x
- Spring Cloud: Eureka (discovery), Gateway, Config Server, Resilience4j
- PostgreSQL + MongoDB
- Redis (cache)
- Monitorizare: Prometheus + Grafana
- Frontend: React
- Build: Maven; rulare în Docker

### Implementat (monolitul — COMPLET, pe `main`)
- Java 21 (Temurin 21.0.11)
- Spring Boot **3.5.15** (parent), Maven prin **Maven Wrapper** (`./mvnw`)
- Dependențe: Spring Web, Spring Data JPA, Validation, **Security**, **AOP**,
  PostgreSQL Driver, Lombok, DevTools, springdoc-openapi (Swagger), H2 +
  spring-security-test (test), JaCoCo + spring-boot-starter-test.
- Model de date: 7 entități JPA + 2 enum-uri (vezi §8)
- Backend: repository + DTO + mapper + service layer + excepții (§9), profiluri
  dev/test (§10), Spring Security sesiune+CSRF+roluri (§11), paginare/sortare +
  logging+AOP (§12), testare JaCoCo (§13), REST controllers + validare + erori +
  CORS + Swagger (§14).
- **Frontend React** (SPA cyberpunk) în `/frontend` — React 18 + TS + Vite, vezi §15.
- Build tool: Maven (NU Gradle)

### Porturi & credențiale dev (importante)
- **PostgreSQL**: Docker, host **5433** (intern 5432). DB/user/pass = `creatorhub`.
- **Backend** (Spring): **8081** (profil `dev`). Swagger: `/swagger-ui.html`.
- **Frontend** (Vite dev): **5173** (proxy `/api` → `:8081`).
- **Admin dev** (seed doar pe `dev`): username **`admin`** / parolă **`admin123`**.
- Porneste: `docker compose up -d` → `./mvnw spring-boot:run` → `cd frontend && npm run dev`.

### Țintă finală microservicii (planul detaliat în NEXT_STEPS.md)
Spring Cloud: Eureka (discovery), Gateway, Config Server, Resilience4j; Redis
(cache), MongoDB (NoSQL); Prometheus + Grafana; JWT distribuit; Docker Compose.

## 3. Reguli de lucru (valabile pentru tot proiectul)

1. **RECON ÎNAINTE DE COD.** Înțelege ce există deja înainte să modifici. Citește
   acest fișier la începutul fiecărei sesiuni.
2. **COMMIT-URI MICI ȘI DESE.** Commit după fiecare unitate logică de lucru, cu
   mesaje clare și convenționale (`feat:`, `fix:`, `chore:`, `docs:`, ...). Fără
   commit-uri uriașe care amestecă lucruri.
3. **NU LUA DECIZII MARI SINGUR.** La o decizie arhitecturală importantă sau ceva
   ambiguu — oprește-te și întreabă, nu presupune.
4. **CALITATE DE PRODUCȚIE.** Separarea responsabilităților
   (controller → service → repository), cod curat, fără shortcut-uri urâte.
5. **ACTUALIZEAZĂ ACEST FIȘIER.** Când termini o fază sau iei o decizie
   importantă, notează în secțiunea Progres (§5).
6. **RAMURĂ SEPARATĂ PENTRU MICROSERVICII.** Lucrul la microservicii se face pe
   ramura `microservices`; `main` (monolitul complet) rămâne intact ca backup. NU
   sparge monolitul de pe `main` fără motiv explicit.
7. **NU STRICA CE E GATA.** Monolitul + frontend-ul sunt funcționale (105 teste
   verzi). Nu rescrie/șterge cod care merge decât dacă e necesar pentru pasul curent.

## 3bis. Lecții de tooling (mediul de dev — economisesc timp)

> Mediu: **Windows 10**, shell primar **PowerShell 5.1** (+ Bash tool pentru POSIX).
> Maven NU e instalat global → folosește `./mvnw` (Windows: `mvnw.cmd`). `JAVA_HOME`
> uneori trebuie setat: `$env:JAVA_HOME = Split-Path (Split-Path (Get-Command java).Source -Parent) -Parent`.

- **Commit-uri:** `git commit -m` cu ghilimele duble / `<...>` (din `Co-Authored-By`)
  / heredoc-uri **se rupe** în PowerShell 5.1. Scrie mesajul într-un fișier temp și
  `git commit -F <fișier>`.
- **`Remove-Item`** e adesea blocat de sandbox (fals pozitiv, ex. pe `-split "\s+"`).
  Pentru ștergeri folosește **Bash tool** (`rm -rf`), nu PowerShell.
- **Verificări HTTP:** folosește **`curl.exe`**, NU `Invoke-WebRequest` (IWR cu body
  gol a raportat fals 200 în loc de 403 — aproape a ascuns o falsă gaură CSRF).
  JSON pentru curl: scrie-l într-un fișier și `curl.exe --data-binary @file` (evită
  problemele de quoting). CSRF în curl: `GET /api/auth/csrf` cu cookie jar, citește
  cookie-ul `XSRF-TOKEN`, trimite-l în header `X-XSRF-TOKEN` (tokenul rotește după login).
- **Run app fără a bloca shell-ul:** `Start-Process java -jar ...jar` (sau `mvnw spring-boot:run`)
  în fundal cu redirect la log, apoi poll pe `Started CreatorHubApplication`.
- **Preview UI:** `.claude/launch.json` + tool-ul de preview pe portul 5173 (oprește
  întâi Vite-ul pornit manual ca să elibereze portul).

## 4. Convenții de cod

- **Pachet rădăcină:** `com.creatorhub`
- **Structura pachetelor:**
  - `controller` — endpoint-uri REST (`@RestController`), fără logică de business
  - `service`    — logica de business; aici sunt granițele de tranzacție
  - `repository` — interfețe Spring Data JPA
  - `model`      — entități JPA (`@Entity`)
  - `dto`        — obiecte request/response; entitățile NU se expun direct prin API
  - `exception`  — excepții custom + handler global (`@RestControllerAdvice`)
  - `config`     — clase de configurare Spring (`@Configuration`)
- **Flux de dependențe:** controller → service → repository. Niciodată invers.
  Controllerele lucrează cu DTO-uri; maparea entity↔DTO se face în service.
- **Naming:** `*Controller`, `*Service` (+ impl unde e cazul), `*Repository`,
  `*Dto` / `*Request` / `*Response`, `*Exception`.
- **Validare:** Bean Validation (`jakarta.validation`) pe DTO-urile de intrare.
- **Lombok:** permis pentru boilerplate (`@Getter/@Setter`, `@Builder`,
  `@RequiredArgsConstructor`). Injecție prin constructor, nu prin câmp.
- **Config:** `application.yml` (YAML, nu `.properties`).

## 5. Progres pe faze

- [x] **Faza 0 — Setup (COMPLETĂ, 2026-06-23):** schelet Maven + Spring Boot
  3.5.15, structura de pachete sub `com.creatorhub`, `docker-compose.yml`
  (PostgreSQL 16), `application.yml`, `.gitignore` + `.gitattributes`, README,
  Maven Wrapper (`./mvnw`), repo Git inițializat (commit `201c40d`).
  Verificat: `./mvnw clean compile` → **BUILD SUCCESS**.
- [x] **Faza 1 — Model de date (COMPLETĂ, 2026-06-23):** 7 entități JPA
  (`User`, `Profile`, `SubscriptionTier`, `Post`, `Subscription`, `Comment`,
  `Tag`) + 2 enum-uri (`Role`, `SubStatus`) în `com.creatorhub.model[.enums]`.
  Verificat: Hibernate a generat 8 tabele (cele 7 + `post_tags`) și 10 FK-uri,
  aplicația pornește verde (`Started CreatorHubApplication`).
- [x] **Faza 2 — CRUD + Service layer + Exception handling (COMPLETĂ, 2026-06-23):**
  7 repository-uri Spring Data JPA, 14 DTO-uri (Request/Response) + 7 mappere
  manuale (`dto.mapper`), 7 service-uri (interfață + impl în `service.impl`) cu
  reguli de business și `@Transactional`, ierarhie de 4 excepții (+ bază). Fără
  controllere încă (vin la Faza Views). Verificat: `./mvnw clean compile` verde
  și **9 teste de integrare** trec (`Phase2ServiceFlowTests`). Detalii în §9.
- [x] **Faza 3 — Multi-Environment (COMPLETĂ, 2026-06-23):** profiluri Spring
  `dev` (PostgreSQL/Docker, 5433) și `test` (H2 in-memory). `application.yml`
  bază comună (profil implicit `dev`) + `application-dev.yml` + `application-test.yml`.
  Testele rulează pe H2 (`@ActiveProfiles("test")`), fără Docker. Detalii în §10.
- [x] **Faza 4 — Spring Security (COMPLETĂ, 2026-06-23):** autentificare din DB
  (`CustomUserDetailsService` + adapter `SecurityUser`), BCrypt, sesiune, login
  REST (`/api/auth/login`) + pagină statică `/login`, logout, CSRF cookie-based
  (SPA-ready), remember-me, roluri USER/ADMIN, handler-e 401/403 JSON, admin seed
  pe dev. Verificat: **19 teste** verzi pe H2 + flux real cu curl. Detalii în §11.
- [x] **Faza 5 — Paginare/Sortare + Logging (COMPLETĂ, 2026-06-23):** `Pageable`
  + `PagedResponse<T>` pentru Post, User, Subscription (sortare whitelisted, page
  size plafonat la 100); Logback (`logback-spring.xml`) cu fișier de erori separat,
  nivele per profil; `@Slf4j` în service, aspect AOP de logging, `GlobalExceptionHandler`
  (ERROR pentru erori neașteptate). Verificat: **25 teste** verzi + logging la
  runtime. Detalii în §12.
- [x] **Faza 6 — Testing (COMPLETĂ, 2026-06-23):** JaCoCo (raport + gate 70% pe
  `service.impl`); **64 unit tests** Mockito izolate (7 service impls) + 25 teste
  de integrare (H2). Coverage service layer: **89.1% line** (gate trece). Total
  **89 teste** verzi. Detalii în §13.
- [x] **Faza 7A — REST controllers + Bean Validation + erori (COMPLETĂ, 2026-06-23):**
  controllere REST pentru toate entitățile pe convenția §11, owner-din-context
  (`CurrentUserService` + `Viewer`), gating premium + comentarii premium, Bean
  Validation pe DTO-uri, `GlobalExceptionHandler` extins (400 cu fieldErrors,
  JSON malformat → 400), pagini de eroare custom (404/500), CORS pentru React,
  Swagger (springdoc). Verificat: **105 teste** verzi + smoke runtime. Detalii §14.
- [x] **Faza 7B — Frontend React (COMPLETĂ, 2026-06-23):** SPA React 18 + TS +
  Vite în `/frontend`, temă cyberpunk dark-violet cu glow + animații (Framer
  Motion), CRUD complet prin UI, gating premium vizual, validare client-side,
  CSRF prin Vite proxy (same-origin). 11 pagini. Verificat end-to-end (login,
  feed, gating, 404). Detalii §15. **TOT textul UI în engleză.**
- [~] **Faza 8 (ÎN CURS) — Microservicii** (Spring Cloud: Eureka, Gateway,
  Config, Resilience4j), apoi Redis, MongoDB, monitorizare (Prometheus/Grafana),
  JWT distribuit. **PLANUL COMPLET ȘI PAȘII: vezi [`NEXT_STEPS.md`](NEXT_STEPS.md).**
  Se lucrează pe ramura `microservices` (nu pe `main`).
  - [x] **Pasul 1 — Infrastructură (COMPLET, 2026-06-23):** schelet multi-module
    Maven sub `services/` (parent `creatorhub-services`, Spring Cloud **2025.0.0**),
    Eureka Server (8761), API Gateway (8085, reactiv), Probe Service (8091). Monolitul
    de pe `main` rămâne NEATINS. Test-cheie validat: `GET :8085/api/probe/hello`
    rutează prin Gateway → Eureka (`lb://probe-service`) → probe → **200**. Detalii §16.
  - [ ] **Pasul 2+ — mutarea logicii** (User/Subscription/Content services), Config
    Server, Resilience4j, monitoring, Redis, Mongo, JWT. Vezi `NEXT_STEPS.md` §5.

## 6. Comenzi utile

```bash
docker compose up -d            # pornește PostgreSQL
./mvnw clean compile            # compilează (Windows: mvnw.cmd clean compile)
./mvnw spring-boot:run          # rulează aplicația (necesită DB pornită)
./mvnw clean verify             # build complet + teste
```

## 7. Decizii notabile (jurnal)

- **2026-06-23 (Faza 0):** Maven nu e instalat local → folosim **Maven Wrapper**
  (`./mvnw`) pentru build portabil și reproductibil.
- **2026-06-23 (Faza 0):** `spring.jpa.open-in-view=false` — ținem granițele de
  tranzacție în service, evităm anti-pattern-ul OSIV.
- **2026-06-23 (Faza 0):** `ddl-auto=update` doar pentru development; înainte de
  producție trecem pe migrări versionate (Flyway/Liquibase).
- **2026-06-23 (Faza 1):** Conflicte de port pe mașina de dev → PostgreSQL pe
  host **5433** (exista deja un PostgreSQL 17 nativ pe 5432) și HTTP pe **8081**
  (Oracle TNS listener ocupa 8080). Containerul folosește intern tot 5432.
- **2026-06-23 (Faza 1):** Tabela entității `User` se numește `users` — `user`
  e cuvânt rezervat în PostgreSQL.

## 8. Model de date — convenții și decizii (Faza 1)

7 entități în `com.creatorhub.model`, 2 enum-uri în `com.creatorhub.model.enums`.
Descrierea completă (entități + relații) e în `README.md` → secțiunea *Data model*.

- **ID-uri:** `Long`, `@GeneratedValue(strategy = IDENTITY)` (coloană identity în
  Postgres). Simplu și predictibil; dacă va fi nevoie de batching la scriere,
  reconsiderăm `SEQUENCE`.
- **equals/hashCode:** bazate DOAR pe `id`, sigure la proxy Hibernate
  (`Hibernate.getClass(this)` pentru comparația de tip; `hashCode` constant per
  tip via `Hibernate.getClass(this).hashCode()`). Abordare unică și consistentă
  pe toate entitățile.
- **toString:** Lombok `@ToString`, dar TOATE câmpurile de relație marcate cu
  `@ToString.Exclude` → fără recursivitate / StackOverflow și fără lazy-loading
  accidental.
- **Lombok:** `@Getter @Setter @ToString` (NU `@Data`, NU `@EqualsAndHashCode` —
  equals/hashCode sunt scrise manual). Colecțiile inițializate (`new ArrayList`/
  `HashSet`) ca să evităm NPE.
- **Fetch:** `LAZY` pe toate `@ManyToOne`, `@OneToMany`, `@ManyToMany`. La
  `@OneToOne`: owning side (`Profile`) `LAZY` + `optional=false`.
- **Enum-uri:** `@Enumerated(EnumType.STRING)` (NU ORDINAL).
- **Owning sides:**
  - `User`↔`Profile` (1:1): **Profile** deține FK-ul (`@JoinColumn user_id`),
    `User` are `mappedBy`.
  - `Post`↔`Tag` (N:N): **Post** deține `@JoinTable(post_tags)`, `Tag` are
    `mappedBy`.
- **Cascade (decis per relație, nu „ALL peste tot"):**
  - `User → Profile`: **ALL + orphanRemoval** (compoziție; ștergi userul → piere
    profilul). Cerut explicit.
  - `Post → Comment`: **ALL + orphanRemoval** (un comentariu nu există fără
    postare; lifecycle-ul comentariului e deținut de Post, nu de autor).
  - `Post → Tag` (N:N): **PERSIST + MERGE**, niciodată REMOVE (tag-urile sunt
    partajate între postări — nu le ștergem când ștergem o postare).
  - Restul `@OneToMany`/`@ManyToOne` (tiers, posts, subscriptions, author etc.):
    **fără cascade** — ștergerea creatorului/fanului se va trata explicit în
    business logic (faze viitoare), ca să nu cascadăm orbește peste entități
    referite în altă parte. *(De confirmat când ajungem la ștergeri reale.)*

## 9. Service layer & exception handling (Faza 2)

Arhitectură: **Service → Repository → Entity**. Controllerele NU există încă
(Faza Views). Service-urile: `@Service @Transactional`, citirile cu
`@Transactional(readOnly = true)`, injecție prin constructor (`@RequiredArgsConstructor`),
maparea entity↔DTO în mappere statice (`com.creatorhub.dto.mapper`). DTO-urile
sunt plate (ids + reprezentări minime). **`UserResponse` nu conține NICIODATĂ
password.**

### Ierarhia de excepții → cod HTTP intenționat (mapate la Faza Views)

Rădăcină: `CreatorHubException` (abstractă) → toate moștenesc din ea.

| Excepție | Cod HTTP | Când |
|---|---|---|
| `ResourceNotFoundException` | **404** | entitate inexistentă după id/cheie (toate find/update/delete) |
| `DuplicateResourceException` | **409** | username/email/nume tag duplicat; a 2-a abonare ACTIVE la același tier |
| `ResourceInUseException` | **409** | ștergere blocată de dependențe (user cu tiers/posts/comments/subs; tier cu subs/posts; tag folosit de postări) |
| `BusinessRuleException` | **400** | reguli de business pe date (post premium fără tier, post free cu tier, preț ≤ 0, auto-abonare, tier-ul altui creator, tag gol) |

### Reguli de business implementate

- **User.create:** username & email unice (altfel `Duplicate`); creează automat
  `Profile` (displayName = cel din request sau, implicit, username); rol implicit
  `USER`.
- **User.delete (decizie — varianta sigură):** dacă userul are tiers/posts/
  comments/subscriptions → `ResourceInUseException` (NU ștergere oarbă). Doar
  `Profile` cade în cascadă. Ștergerea conținutului în cascadă explicită se poate
  adăuga ulterior dacă se dorește.
- **SubscriptionTier.create/update:** `priceMonthly > 0` (altfel `BusinessRule`);
  creatorul e imutabil la update. **delete:** blocat dacă are subscriptions/posts.
- **Post.create/update:** premium ⇒ tier obligatoriu; free ⇒ fără tier; tier-ul
  trebuie să aparțină autorului (altfel `BusinessRule`). Tag-urile: get-or-create
  după nume (în `PostServiceImpl` prin `TagRepository`). **delete** simplu
  (comentariile cad prin orphanRemoval; rândurile din `post_tags` se șterg, dar
  tag-urile rămân).
- **Subscription.create:** fan ≠ creatorul tier-ului (`BusinessRule`); fără a 2-a
  abonare ACTIVE (`Duplicate`); `startDate = azi`, `status = ACTIVE`. „Update"-ul
  cu sens e tranziția de status → metoda `cancel(id)`.
- **Comment.create:** doar existența post+author. *Controlul accesului premium
  (doar abonații comentează) e lăsat intenționat pentru faza de Security/Views.*
- **Profile:** entitate dependentă → `ProfileService` expune doar read + update
  (create/delete se fac prin `User`, conform compoziției din §8).

### Teste

`Phase2ServiceFlowTests` (`@SpringBootTest @ActiveProfiles("test") @Transactional`,
rollback per test) — 9 teste. Rulează pe **H2 in-memory** (vezi §10), fără Docker.

## 10. Multi-environment (Faza 3)

Minimum 2 profiluri Spring + 2 baze diferite, configurare separată pe fișiere.

| Profil | Bază de date | `ddl-auto` | `show-sql` | Port HTTP |
|---|---|---|---|---|
| `dev` (implicit) | PostgreSQL 16 în Docker, host `5433` | `update` | true | 8081 |
| `test` | H2 in-memory (`creatorhub_test`) | `create-drop` | false | — (`@SpringBootTest` MOCK) |

- **`application.yml`** = config comună (nume app, `spring.profiles.active=dev`,
  `open-in-view=false`, `format_sql`). Rularea normală (`./mvnw spring-boot:run`)
  pornește pe **dev**.
- **`application-dev.yml`** = datasource PostgreSQL (5433), `ddl-auto=update`,
  `show-sql=true`, `server.port=8081`.
- **`application-test.yml`** = datasource H2 in-memory, `ddl-auto=create-drop`,
  `show-sql=false`.
- Testele: `@ActiveProfiles("test")` → H2, **fără dependență de Docker**
  (verificat: `docker compose down` + `./mvnw clean test` → 9/9 verde).
- **H2 dependency** în `pom.xml` cu scope `test` (nu ajunge în producție).

**Compatibilitate H2 ↔ PostgreSQL:** rezolvată curat prin **`MODE=PostgreSQL`** în
URL-ul H2 (`jdbc:h2:mem:creatorhub_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL`) — fără
hack-uri în entități. Aliniază tipurile (TEXT pe `Post.body`, `numeric(10,2)`,
enum-uri ca STRING, FK-uri, IDENTITY). Tabela `users` (plural) nu e cuvânt rezervat
în H2 (spre deosebire de `user`), deci redenumirea din Faza 1 ajută și aici.
`DB_CLOSE_DELAY=-1` ține baza in-memory vie pe toată durata JVM-ului.

**Profil de producție:** NU există încă (intenționat). Vine mai târziu (faza de
microservicii/deployment), cu secrete externalizate (env vars / Config Server).

## 11. Spring Security (Faza 4)

Autentificare din DB, pe **sesiune**, expusă prin REST (consumabilă de un SPA),
fundație 100% refolosibilă pentru JWT la microservicii.

### Cum funcționează autentificarea
- `CustomUserDetailsService` încarcă userul din `UserRepository` după username și
  îl mapează prin adaptorul `SecurityUser` (entitatea `User` NU implementează
  `UserDetails` — domeniul rămâne decuplat de framework). Rolul → authority
  `ROLE_<role>` (prefix `ROLE_` pentru `hasRole`).
- `PasswordEncoder` = `BCryptPasswordEncoder`. `UserServiceImpl.create/update`
  encodează parola cu BCrypt înainte de salvare (nu se mai stochează în clar).
- Login: `POST /api/auth/login` (JSON) autentifică programatic via
  `AuthenticationManager`, salvează `SecurityContext` în sesiune
  (`HttpSessionSecurityContextRepository`), întoarce `UserResponse` (200) sau 401.
- Logout: `POST /api/auth/logout` (filtrul Spring) invalidează sesiunea, șterge
  `JSESSIONID` + `remember-me`, întoarce 200.
- `GET /api/auth/me` = userul curent (util pentru SPA).

### Decizia despre login page (hibrid)
Cerința „pagină de login custom" + frontend React = tensiune reală (am întrebat).
Ales: **hibrid** — pagină statică `/login` (`static/login.html`, forward din
`WebConfig`) ca soluție de tranziție care bifează cerința + testare manuală ACUM,
ȘI endpoint REST `/api/auth/login` ca sursă principală pe care o va consuma React
la Faza Views. Ambele pe aceeași sesiune. Pagina statică poate fi păstrată sau
eliminată la final.

### CSRF (activ, pentru ambele moduri — fără conflict)
`CookieCsrfTokenRepository.withHttpOnlyFalse()` + `SpaCsrfTokenRequestHandler`
(handler-ul oficial Spring care acceptă tokenul ȘI din header SPA `X-XSRF-TOKEN`
ȘI din param de formular clasic `_csrf`) + `CsrfCookieFilter` (emite cookie-ul
`XSRF-TOKEN`). Pagina statică și React folosesc **același** flux: citesc cookie-ul
`XSRF-TOKEN`, îl trimit în header. Deci NU e conflict între „form clasic" și SPA —
un singur mecanism. Verificat real (curl): logout/register fără token → **403**,
cu token → 200/201.

### Remember-me
Token-based (`TokenBasedRememberMeServices`, cheie dev în `SecurityConfig` — de
externalizat la prod), validitate 14 zile. Declanșat din login când
`rememberMe=true` (request wrapper care expune parametrul către serviciu).

### Convenția de URL pentru autorizare (construim controllerele la Faza Views consecvent!)
- `POST /api/auth/register`, `POST /api/auth/login` → **public**
- `POST /api/auth/logout`, `GET /api/auth/me` → autentificat
- `GET /api/creators/**`, `/api/profiles/**`, `/api/posts/**`, `/api/tags/**` →
  **public** (citire/browsing). *Gating-ul premium NU se face pe URL — se enforce
  în service layer (verificare abonament) la Faza Views.*
- `/api/admin/**` → **`hasRole('ADMIN')`** (+ `@PreAuthorize` pe metodă, defense
  in depth). Exemplu existent: `GET /api/admin/users`.
- static (`/`, `/login`, `/css/**`, ...) → public; **orice altceva** → autentificat.

### Erori de securitate → cod HTTP
- Neautentificat pe resursă protejată → **401** (`RestAuthenticationEntryPoint`).
- Autentificat fără rol → **403** (`RestAccessDeniedHandler`).
- Ambele întorc JSON `ApiErrorResponse` (nu HTML), aliniat cu viitorul
  `@RestControllerAdvice` (care va reutiliza `ApiErrorResponse` pentru 404/409/400).

### Admin (seed pe dev)
`DevDataSeeder` (`@Profile("dev")`, idempotent) creează un admin la pornire DOAR
pe dev: **username `admin` / parolă `admin123`** (doar pentru development; nu în
test/prod). Register-ul public creează DOAR `USER` (fără câmp `role` în
`RegisterRequest` → fără escaladare de privilegii).

### JWT mai târziu (la microservicii)
Se adaugă un filtru JWT PESTE această fundație. **Se refolosesc neschimbate:**
`CustomUserDetailsService` și `PasswordEncoder`. Se schimbă doar: sesiune →
stateless (`SessionCreationPolicy.STATELESS`), iar login-ul întoarce un token JWT
în loc să creeze sesiune. Regulile de autorizare pe URL/rol rămân identice.

### Teste
`SecurityIntegrationTests` (MockMvc + spring-security-test, `@ActiveProfiles("test")`)
— 10 teste: register+hash BCrypt, login ok/greșit (401), remember-me cookie,
protejat fără auth (401), admin ca USER (403) / ca ADMIN (200), logout, CSRF
enforced (register+logout fără token → 403). Plus cele 9 de la Faza 2 = **19 verzi**
pe H2.

## 12. Paginare/Sortare + Logging (Faza 5)

### Paginare & sortare
Entități paginate (`Pageable` în repository + service): **Post, User, Subscription**.
Service-urile întorc **`PagedResponse<T>`** (DTO stabil: `content, page, size,
totalElements, totalPages, first, last`) — NU expunem `Page` din Spring.

- **Câmpuri de sortare permise (whitelist, în `service.impl`):**
  - Post: `id, title, createdAt, premium`
  - User: `id, username, email, role, enabled` *(NICIODATĂ `password`)*
  - Subscription: `id, startDate, status`
- **`PageableUtils.sanitize(pageable, allowed)`** (în `com.creatorhub.common`):
  validează sortarea (câmp neautorizat → `BusinessRuleException` 400) și plafonează
  page size la **`MAX_PAGE_SIZE=100`**. Asta blochează și sortarea după câmpuri
  interne/sensibile (ex. `password`) și evită `PropertyReferenceException`.
- **Dimensiuni pagină:** default **20**, max **100** — declarativ în `application.yml`
  (`spring.data.web.pageable`, pentru Pageable din request la Faza Views) ȘI în
  service (defense in depth, testabil acum).

### Logging
`logback-spring.xml` (SLF4J + Logback). Appendere: **CONSOLE** + **FILE**
(`logs/creatorhub.log`, rolling size+time) + **ERROR_FILE**
(`logs/creatorhub-error.log`, cu `ThresholdFilter` ERROR → DOAR ERROR+). `logs/`
e în `.gitignore`.

- **Nivele per profil:** `dev` → `com.creatorhub`=DEBUG, root=INFO, consolă+fișiere;
  `test` → root=WARN, consolă (fără fișiere, ca să nu polueze testele); fallback
  (prod) → INFO, consolă+fișiere.
- **În cod:** `@Slf4j` în service-uri. INFO (user/post creat, abonament activat),
  WARN (auto-abonare/abonare duplicată respinsă, delete blocat), ERROR (în
  `GlobalExceptionHandler` pentru erori neașteptate). Parametrizat (`{}`).
  **SECURITATE: niciodată parole/token-uri** — se loghează doar username/id.
- **AOP (bonus, cerut):** `ServiceLoggingAspect` (`@Around` pe `service.impl`)
  loghează la DEBUG intrarea/ieșirea + timpul. **Loghează doar numele metodei +
  NUMĂRUL de argumente, nu valorile** (altfel ar scurge parola din `UserRequest`).
  Guard `isDebugEnabled()` → no-op în test/prod.
- **`GlobalExceptionHandler`** (`@RestControllerAdvice`): mapează excepțiile de
  domeniu la codurile §9 (404/409/400, log WARN) + `AccessDeniedException`→403 +
  catch-all `Exception`→500 (log **ERROR**). *Decizie: l-am adus acum (Faza Views
  îl extinde cu validarea input). Limitare cunoscută: input malformat (JSON
  invalid) dă momentan 500, nu 400 — Faza Views adaugă handler-ul de validare.*

### Verificat
- 25 teste verzi pe H2 (paginare 6 + securitate 10 + flux business 9).
- Runtime dev: INFO `User created: id=.. username=..` (fără parolă) în
  `creatorhub.log`, AOP DEBUG `call/done ...() [N args] in X ms`; `creatorhub-error.log`
  conține DOAR ERROR (0 INFO/WARN). Eroare provocată: JSON malformat → 500 ERROR.

## 13. Testare (Faza 6)

**89 teste**, două categorii clar separate:

### Unit tests (Mockito, FĂRĂ Spring/DB) — 64 teste
`*ServiceImplTest` în `src/test/java/com/creatorhub/service/impl/`. Folosesc
`@ExtendWith(MockitoExtension.class)`, `@Mock` pe repository-uri/`PasswordEncoder`,
`@InjectMocks` pe `ServiceImpl`. Rapide, izolate, **nu ating baza de date**. Câte
unul per service impl: User (12), Post (13), Subscription (9), SubscriptionTier
(12), Comment (6), Tag (7), Profile (5). Acoperă căi fericite + reguli de business
(parolă encodată, profil auto-creat, duplicate, premium/tier, auto-abonare, preț
≤ 0, delete blocat etc.).

### Integration tests (`@SpringBootTest`, H2) — 25 teste, ≥3 scenarii end-to-end
`*IntegrationTest` (+ `SecurityIntegrationTests`) pe profilul `test` (H2, fără
Docker). Cele **3 scenarii end-to-end**:
1. **`BusinessFlowIntegrationTest`** (9): creator → tier → post premium → fan se
   abonează → comentariu; + regulile de business prin tot stack-ul de service.
2. **`SecurityIntegrationTests`** (10): autentificare + autorizare pe rol (login,
   acces protejat 401, admin 200 vs user 403, CSRF, remember-me).
3. **`PaginationSortingIntegrationTest`** (6): paginare + sortare end-to-end.

### Separarea bazei de date
- **Unit** = fără DB (Mockito mock-uiește repository-urile).
- **Integration** = H2 in-memory (profil `test`, `@ActiveProfiles("test")`), fără Docker.

### Coverage (JaCoCo)
- Plugin JaCoCo: raport HTML la `mvn test` (`target/site/jacoco/index.html`) +
  **gate de 70% line coverage pe `com.creatorhub.service.impl`** la `mvn verify`.
- **Coverage service layer atins: 89.1% line** (244/274), branch 66.9%. Per clasă
  toate ≥ 82%. `mvn verify` trece (gate-ul de 70% e respectat).
- **Excluse din coverage** (nu conțin logică de business reală): entități (`model`),
  DTO-uri + mappere (`dto`), config + seeder (`config`), wiring de securitate
  (`security`), aspectul AOP (`aspect`), clasa main, și clasele simple de excepție
  (`*Exception`). Astfel coverage-ul reflectă logica reală din service layer.

### Comenzi
- `./mvnw clean test` → toate testele + raport coverage.
- `./mvnw clean verify` → în plus, gate-ul de coverage (eșuează dacă service < 70%).

> Notă: la Faza 7A coverage-ul pe service e **87% line** (105 teste); detalii §14.

## 14. REST API + Validare + Erori (Faza 7A)

### Userul curent & owner-din-context (fix de securitate)
- `CurrentUserService` (security) citește `SecurityContextHolder` → întoarce un
  `Viewer(userId, admin)` (record în `common`). `currentViewer()` (anonim permis),
  `requireViewer()` (403 dacă anonim).
- **Controllerele** resolvă `Viewer` și-l pasează în service. **Owner-ul NU mai
  vine din body** — la create, owner = `viewer.userId()` (Post.author, Tier.creator,
  Subscription.fan, Comment.author). DTO-urile de Request **NU mai au** authorId/
  creatorId/fanId. Update/delete: doar owner-ul sau ADMIN (`AccessDeniedException`→403).

### Gating premium (citire)
- `PostResponse` are flag `locked`. Pentru un post premium, **body-ul e vizibil
  doar pentru: autor, ADMIN, sau fan cu abonament ACTIV la tier-ul postării**.
  Altfel `locked=true` + `body=null` (omis din JSON via `@JsonInclude(NON_NULL)`).
  Coerent la listă ȘI la `GET /{id}` (NU 403 — metadata rămâne vizibilă, doar
  body-ul e ascuns). Implementat în service (`canAccessBody`).
- **Comentarii premium** (regula amânată de la Faza 2): pe un post premium pot
  comenta doar autor/ADMIN/abonat activ; altfel **403**.

### Endpoint-uri REST (resource → metode → auth → cod)
| Endpoint | Auth | Cod |
|---|---|---|
| `POST /api/auth/register` | public | 201 |
| `POST /api/auth/login` · `GET /api/auth/csrf` | public | 200 |
| `POST /api/auth/logout` · `GET /api/auth/me` | auth | 200 |
| `GET /api/posts` (paged, gated) · `GET /api/posts/{id}` · `GET /api/posts/{id}/comments` | public | 200 |
| `POST /api/posts` | auth | 201 |
| `PUT/DELETE /api/posts/{id}` | owner/admin | 200/204 |
| `GET /api/creators` · `/{id}` · `/{id}/posts` · `/{id}/tiers` | public | 200 |
| `GET /api/profiles/{id}` · `/user/{userId}` | public | 200 |
| `PUT /api/profiles/{id}` | owner/admin | 200 |
| `GET /api/tags` · `/{id}` | public | 200 |
| `POST /api/tags` | auth | 201 |
| `DELETE /api/tags/{id}` | ADMIN | 204 |
| `GET /api/tiers/{id}` | auth | 200 |
| `POST /api/tiers` | auth | 201 |
| `PUT/DELETE /api/tiers/{id}` | owner/admin | 200/204 |
| `GET /api/subscriptions` (ale mele) | auth | 200 |
| `POST /api/subscriptions` | auth | 201 |
| `POST /api/subscriptions/{id}/cancel` · `DELETE /{id}` | owner/admin | 200/204 |
| `POST /api/comments` · `PUT/DELETE /{id}` | auth (owner/admin) | 201/200/204 |
| `GET /api/admin/users` · `DELETE /api/admin/users/{id}` | ADMIN | 200/204 |

### Bean Validation (pe Request DTO-uri, cu `@Valid` în controllere)
- `RegisterRequest`: username `@NotBlank @Size(3,50)`, email `@NotBlank @Email`,
  password `@NotBlank @Size(min=8)`, displayName `@Size(max=100)`.
- `PostRequest`: title `@NotBlank @Size(max=200)`, body `@Size(max=20000)`.
- `SubscriptionTierRequest`: name `@NotBlank @Size(max=100)`, priceMonthly
  `@NotNull @DecimalMin("0.01")`, perks `@Size(max=2000)`.
- `SubscriptionRequest`: tierId `@NotNull`. `CommentRequest`: text
  `@NotBlank @Size(max=1000)`, postId `@NotNull`. `TagRequest`: name
  `@NotBlank @Size(max=50)`. `ProfileRequest`: câmpuri `@Size` (update parțial).

### Răspuns de eroare (validare) — `GlobalExceptionHandler` extins
- `MethodArgumentNotValidException` → **400** cu `fieldErrors` (câmp→mesaj):
```json
{ "timestamp":"...", "status":400, "error":"Bad Request", "message":"Validation failed",
  "path":"/api/tiers", "fieldErrors": { "name":"Tier name is required",
  "priceMonthly":"Monthly price must be greater than 0" } }
```
- `HttpMessageNotReadableException` (JSON malformat) → **400** (rezolvă limitarea
  de la Faza 5). Restul: 404/409/400 domeniu, 401/403 securitate — toate
  `ApiErrorResponse` (cu `@JsonInclude(NON_NULL)`).

### Pagini de eroare custom
`static/error/404.html` + `static/error/500.html` (servite de `BasicErrorController`
pentru rute de browser; înlocuiesc Whitelabel). API → JSON. Rutele necunoscute
non-API → 404 custom (config: `/api/**` rămâne securizat, restul `permitAll` ca să
randeze 404 nu 401). React va avea propriile pagini la 7B.

### CORS (pentru React) + CSRF
`CorsConfigurationSource`: origini dev `http://localhost:5173`, `:3000`,
`allowCredentials=true`, header-e `*`. **Tensiune CORS+CSRF cross-origin:** un SPA
pe alt origin NU poate citi cookie-ul `XSRF-TOKEN` (JS e same-origin). Soluții:
(a) **recomandat** — Vite dev proxy (`/api` → `:8081`) → same-origin, cookie-ul e
citibil, CSRF merge ca la pagina statică; (b) endpoint `GET /api/auth/csrf` care
întoarce tokenul în body (citibil cross-origin) → React îl pune în `X-XSRF-TOKEN`.
În prod: același origin (reverse proxy) + origini CORS restrânse.

### Swagger / OpenAPI
`springdoc-openapi-starter-webmvc-ui` 2.8.17. UI: `/swagger-ui.html` (→ `/swagger-ui/index.html`),
spec: `/v3/api-docs`. Permise în `SecurityConfig` (dev). Verificat 200 la runtime.

## 15. Frontend React (Faza 7B)

SPA separat în **`/frontend`** (NU se amestecă cu backend-ul Spring din `/src`).

### Stack
React 18 + **TypeScript** + **Vite**. Rutare: **React Router v6**. Data fetching:
**TanStack Query** (axios `withCredentials`). Animații: **Framer Motion**.
Formulare + validare client-side: **React Hook Form**. Styling: **CSS custom**
(temă în `src/index.css` + CSS per componentă) — ales pentru control total pe
glow/gradienturi/animații.

### Structură
- `src/api/` — `client.ts` (axios + interceptor CSRF), `endpoints.ts` (toate
  endpoint-urile §14). `src/types.ts` (oglindesc DTO-urile).
- `src/auth/AuthContext.tsx` — user curent (`/api/auth/me`), login/register/logout.
  `components/ProtectedRoute.tsx` (redirect la `/login`, `requireAdmin`).
- `src/components/` — Layout (header/nav), PostCard (cu overlay-ul premium blocat),
  Page (tranziție), Field, Pagination, ui (Avatar/Badge/Spinner/Skeleton/CountUp/
  EmptyState/Alert + variante de stagger).
- `src/pages/` (11): Feed, Login, Register, PostDetail, Creator (listă + profil),
  CreatePost (create/edit), Tiers, Subscriptions, ProfileEdit, Admin, NotFound (404).

### Integrare CSRF (decizia §14, opțiunea recomandată)
**Vite dev proxy** (`vite.config.ts`): `/api` → `http://localhost:8081`. Astfel SPA
e same-origin cu API-ul → cookie-ul `XSRF-TOKEN` e citibil de JS. Interceptorul
axios citește cookie-ul și-l pune în header-ul `X-XSRF-TOKEN` pe POST/PUT/DELETE
(amorsează cu `GET /api/auth/csrf` la nevoie). Sesiunea pe cookie (backend).
Verificat end-to-end prin proxy: csrf → login admin → me → 200.

### Temă de design
Cyberpunk / night-owl: fundal near-black cu glow radial violet + grilă animată;
accent **violet electric** (`#a855f7`/`#7c3aed`/`#c026d3`) cu gradienturi și
**glow** (box-shadow violet) pe butoane/carduri/avatare. Fonturi Google: **Space
Grotesk** (display) + **Inter** (body). Badge-uri FREE (teal) / PREMIUM (gradient
+ lacăt). Cardul premium blocat = momentul-vedetă (body blurat, orb-lacăt cu glow
pulsatil, buton "Unlock"). Animații: page transitions (fade+slide), stagger pe
liste, hover lift+glow pe carduri, like cu puls, count-up pe statistici, skeleton
shimmer. Respectă `prefers-reduced-motion`.

### Validare client-side + erori
React Hook Form validează pe client (required, lungimi, email, preț > 0). La 400
de la backend, `fieldErrors` se mapează pe câmpurile formularului (`setError`).
401 → redirect login; 403 → mesaj "no access"; 404 → pagina 404 React.

### Rulare
`cd frontend && npm install && npm run dev` → `http://localhost:5173` (backend pe
dev + Docker trebuie pornite). Build: `npm run build` (0 erori TS). **Tot textul
UI e în engleză.** Admin dev: `admin` / `admin123`.

## 16. Microservicii — Infrastructură (Faza 8, Pasul 1)

> Ramura `microservices`. Monolitul de pe `main` (root `pom.xml` + `/src`) e
> NEATINS. Detaliile pe pași și ce urmează: [`NEXT_STEPS.md`](NEXT_STEPS.md).

### Organizarea repo (decizia §6 din NEXT_STEPS = varianta A, aplicată în siguranță)
Multi-module Maven cu parent **`creatorhub-services`**, dar plasat sub **`services/pom.xml`**
(NU în rădăcina repo-ului). Motiv: rădăcina e deja ocupată de `pom.xml`-ul monolitului;
punând parent-ul microserviciilor sub `services/` **monolitul rămâne 100% neatins** și
schimbarea e complet reversibilă (`main` rămâne livrabilul sigur). Parent-ul moștenește
`spring-boot-starter-parent` 3.5.15 și importă BOM-ul Spring Cloud; build unic din
`services/`. *Promovarea unui parent în rădăcina repo-ului care să adopte și monolitul
ca modul e amânată intenționat pentru un pas ulterior (mai riscant).*

```
AWBD/
├── pom.xml, src/        ← MONOLIT (neatins, pe main)
├── frontend/            ← React (neatins)
└── services/
    ├── pom.xml          ← parent multi-module (creatorhub-services)
    ├── eureka-server/   ← :8761
    ├── api-gateway/     ← :8085
    └── probe-service/   ← :8091 (throwaway, doar pt validare; se șterge)
```

### Versiuni & porturi
- **Spring Cloud 2025.0.0** ("Northfields") — trenul compatibil cu Spring Boot 3.5.x.
  *Verificat în Maven Central; ATENȚIE: 2024.0.x/"Moorgate" e pentru Boot 3.4.x, NU 3.5.x*
  (nota din NEXT_STEPS presupunea greșit 2024.0.x).
- **Eureka Server: 8761** (standalone). **API Gateway: 8085**. **Probe: 8091**.
- **Conflict de port rezolvat:** 8080 e ocupat de **Oracle TNS listener (TNSLSNR)** pe
  mașina de dev → gateway-ul ia **8085**. La fel, 8090 ocupat → probe pe **8091**.

### Gateway
Starter **`spring-cloud-starter-gateway-server-webflux`** (reactiv; cel ne-deprecat în
2025.0.0 — vechiul `spring-cloud-starter-gateway` e deprecat). Rută explicită:
`/api/probe/**` → `lb://probe-service` (discovery prin Eureka + Spring Cloud LoadBalancer).

### Rulare & test-cheie (Pasul 1)
Pornește în ordine (fiecare jar din `services/<modul>/target/`): **eureka-server →
probe-service → api-gateway**. Build: `./mvnw -f services/pom.xml clean package`.
- Eureka dashboard: `http://localhost:8761` → `PROBE-SERVICE` și `API-GATEWAY` UP.
- **Test-cheie (validat 2026-06-23):** `GET http://localhost:8085/api/probe/hello`
  → rutează prin Gateway → rezolvă prin Eureka → ajunge la probe (8091) → **200**
  `{"message":"hello from probe-service","port":8091,"service":"probe-service"}`.

### Note de tooling specifice
- Build din rădăcină cu `-f services/pom.xml` (wrapper-ul monolitului e refolosit).
- Rulare în fundal: `Start-Process java -jar ...` + poll pe `Started <App>` în log
  (logurile de validare sunt în `services/.run-logs/`, ignorate de `*.log` din `.gitignore`).
- Verificări HTTP: `curl.exe` (nu Invoke-WebRequest).
