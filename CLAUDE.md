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
- [ ] **Faza 3 (următoarea) — Views / REST:** controllere REST, Bean Validation
  pe DTO-uri, `@RestControllerAdvice` global (mapează excepțiile la coduri HTTP),
  apoi Security, microservicii, cache Redis, MongoDB, monitorizare, React.

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

`Phase2ServiceFlowTests` (`@SpringBootTest @Transactional`, rollback per test) —
9 teste pe baza Docker (5433). Profil de test dedicat (H2/Testcontainers) vine la
Faza Testing.
