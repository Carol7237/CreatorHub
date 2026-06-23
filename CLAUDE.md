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

**Strategie: monolit întâi, microservicii la final.** Construim mai întâi un
monolit Spring Boot curat și bine structurat; abia în fazele finale îl spargem
în microservicii. **NU adăuga nimic legat de microservicii / Spring Cloud /
Eureka / Gateway / Config Server / Resilience4j până nu ajungem la acele faze.**

## 2. Stack tehnologic

### Țintă finală (unde ajungem)
- Java 21, Spring Boot 3.x
- Spring Cloud: Eureka (discovery), Gateway, Config Server, Resilience4j
- PostgreSQL + MongoDB
- Redis (cache)
- Monitorizare: Prometheus + Grafana
- Frontend: React
- Build: Maven; rulare în Docker

### Implementat până acum (faza curentă — monolit)
- Java 21 (Temurin 21.0.11)
- Spring Boot **3.5.15** (parent), Maven prin **Maven Wrapper** (`./mvnw`)
- Dependențe: Spring Web, Spring Data JPA, Validation, PostgreSQL Driver,
  Lombok, DevTools, spring-boot-starter-test (test scope)
- PostgreSQL **16** în Docker (`docker-compose.yml`) — pe host portul **5433**
  (mapat la 5432 din container), HTTP pe **8081** (vezi §7 pentru de ce)
- Model de date: 7 entități JPA + 2 enum-uri (vezi §8)
- Backend: repository + DTO + mapper + service layer + excepții (vezi §9). Fără
  controllere încă (Faza Views).
- Profiluri Spring: `dev` (PostgreSQL/Docker) și `test` (H2 in-memory) — vezi §10.
- Securitate: Spring Security (sesiune, BCrypt, CSRF, roluri) — vezi §11.
- Paginare/sortare (`PagedResponse`) + logging (Logback, AOP) — vezi §12.
- Testare: JaCoCo + unit (Mockito) + integration (H2); 89 teste — vezi §13.
- Build tool: Maven (NU Gradle)

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
  `service.impl`); **55 unit tests** Mockito izolate (7 service impls) + 25 teste
  de integrare (H2). Coverage service layer: **89.1% line** (gate trece). Total
  **89 teste** verzi. Detalii în §13.
- [ ] **Faza 7 (următoarea) — Views / REST:** controllere REST pentru restul
  entităților, Bean Validation pe DTO-uri, extinderea `GlobalExceptionHandler`
  (validare input → 400 cu erori pe câmpuri), apoi microservicii, cache Redis,
  MongoDB, monitorizare, React.

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

### Unit tests (Mockito, FĂRĂ Spring/DB) — 55 teste
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
