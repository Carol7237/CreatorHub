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
- PostgreSQL **16** în Docker (`docker-compose.yml`)
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
- [ ] **Faza 1 (următoarea) — Model de date:** entități JPA + repository-uri
  controllere REST, validare & handler global de erori, autentificare
  (Spring Security — abia în faza ei), apoi spargerea în microservicii,
  cache Redis, MongoDB, monitorizare, frontend React.

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
