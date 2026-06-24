# JWT_PLAN.md — Planul pentru autentificare JWT distribuită

> **Citește întâi `CLAUDE.md`** (mai ales blocul „STARE CURENTĂ" din §1 — porturi,
> credențiale, **arhitectura de securitate ACTUALĂ** — și §11/§17/§18 pentru detaliile
> de securitate, §3bis pentru lecțiile de tooling). Acest fișier descrie EXACT ce
> urmează. **NU începe să scrii cod fără să citești secțiunea de siguranță de mai jos.**

---

## 0. Obiectivul

Înlocuim autentificarea **pe sesiune** (cookie `JSESSIONID` + CSRF) cu **JWT** în TOT
sistemul de microservicii — backend ȘI frontend. Bifează cerința opțională **„Securitate
Avansată JWT"**.

**Decizia luată cu utilizatorul: OPȚIUNEA 1 — COMPLETĂ.** JWT peste tot:
- **User Service** emite un **JWT semnat** la login (conține `userId` + rol + expirare).
- **Gateway-ul validează JWT-ul** pe fiecare cerere (în loc să apeleze `/api/auth/me` pe
  sesiune) și **injectează aceleași headere `X-User-*`** către downstream.
- **Serviciile downstream NU se schimbă** — primesc tot headere de la gateway
  (`HeaderAuthenticationFilter` se refolosește neschimbat).
- **Frontend-ul** e **rebranșat de pe monolit (8081) pe stiva de microservicii (gateway
  8085)** și folosește JWT: stochează token-ul, îl trimite în `Authorization: Bearer <jwt>`,
  logout îl șterge, rutele protejate verifică token-ul.

---

## 1. ⚠️ STRATEGIE DE SIGURANȚĂ (citește înainte de orice)

- **Ramură NOUĂ `jwt-auth`**, plecată din `microservices`:
  `git checkout microservices && git checkout -b jwt-auth`.
- **`main` + `dev` + `microservices` rămân INTACTE ca backup.** Sistemul actual cu sesiune
  e complet și funcțional (în zona notei 10) — **NU trebuie pierdut**. Dacă JWT se complică
  sau strică ceva grav, **abandonezi `jwt-auth`** și revii la `microservices` fără pierderi.
- Sincronizarea pe GitHub a lui `jwt-auth` (și eventual merge în main/dev) se face **DOAR la
  final**, după ce JWT e validat complet end-to-end. Până atunci, `main`/`dev` rămân pe
  versiunea cu sesiune.
- **Monolitul (`src/`) rămâne NEATINS.** JWT se face doar în microservicii + frontend.
- **NU strica mecanismul de headere downstream.** Se schimbă DOAR sursa identității la
  gateway (sesiune → JWT). `HeaderAuthenticationFilter` + `CurrentViewerService` din `common`
  + securitatea stateless a serviciilor downstream rămân **neschimbate**.

---

## 2. STRATEGIE INCREMENTALĂ — pași verificați, NU tot deodată

La FIECARE pas: cod → build → pornit → **verificat** (curl/browser) → commit mic. Și la
fiecare pas **re-confirmă că nu s-a rupt nimic** din ce mergea (gating premium, load
balancing, notificări, Config Server). Vezi `CLAUDE.md` §18/§22 pentru cum se verifică.

### PASUL 1 — User Service emite JWT la login
- Adaugă o bibliotecă JWT (recomandat **`io.jsonwebtoken:jjwt`** — `jjwt-api` + `jjwt-impl`
  + `jjwt-jackson` runtime; versiune compatibilă cu Java 21). În `user-service`.
- La `POST /api/auth/login` (după autentificarea reușită cu `AuthenticationManager` +
  `CustomUserDetailsService` + BCrypt — **se refolosesc neschimbate**), generează un **JWT
  semnat** care conține: `sub`=userId (sau username — decide; downstream are nevoie de
  `userId` Long, deci pune `userId` ca claim), claim `roles` (ex. `["ROLE_ADMIN"]`), `iat`,
  `exp`. Întoarce token-ul în corpul răspunsului (`{ "token": "...", "type": "Bearer", ... }`)
  + opțional userul (ca acum).
- Cheia de semnare = **secret în config/env** (vezi §3 — puncte delicate), NU hardcodat în
  cod committed (sau hardcodat DEV documentat clar). Poate veni din **Config Server** (frumos:
  centralizezi secretul JWT) sau env var.
- **Decide cu utilizatorul: doar access token cu expirare, SAU access + refresh token?**
  (vezi §3). Pentru Pasul 1, generează cel puțin access token-ul.
- **Verificare:** `POST /api/auth/login` (prin gateway sau direct user-service) → răspunsul
  conține un JWT. Decodează-l (jwt.io sau în cod) și confirmă claim-urile (userId, roles, exp).
  `register`/BCrypt/`/api/auth/me` încă merg.

### PASUL 2 — Gateway validează JWT și injectează identitatea (în loc de sesiune)
- Modifică **`IdentityPropagationFilter`** din `api-gateway`: în loc să apeleze
  `user-service /api/auth/me` pe baza cookie-ului de sesiune, citește header-ul
  **`Authorization: Bearer <jwt>`**, **validează semnătura + expirarea** (cu aceeași cheie
  secretă) și extrage `userId` + `roles` din claim-uri. Apoi injectează **aceleași headere**
  `X-User-Id` / `X-User-Roles` / `X-User-Name` (logica de injecție rămâne, doar SURSA se
  schimbă). **Păstrează anti-spoofing-ul:** șterge orice `X-User-*` venit din exterior.
  Fără token valid → request anonim (endpoint-urile publice merg); token invalid/expirat →
  injectează nimic (downstream va da 401 pe rute protejate).
- Gateway-ul are nevoie de cheia JWT (config/env, ca user-service).
- **Serviciile downstream NU se ating** — `HeaderAuthenticationFilter` citește tot headerele.
  Subscription/Content/Notification rămân stateless, neschimbate.
- *Atenție:* gateway-ul e **reactiv (WebFlux)** — folosește o bibliotecă JWT compatibilă cu
  validare sincronă în filtru (jjwt e ok, validarea e CPU-bound rapidă). NU importa `common`
  în gateway (e servlet) — duplică constantele de headere local (ca acum, `GatewayHeaders`).
- **Verificare (curl):** login → iei token-ul → `GET /api/posts` cu `Authorization: Bearer
  <token>` → merge (identitate injectată); fără token → endpoint public merge / endpoint
  protejat 401; token stricat/expirat → 401. **Re-confirmă gating** (fan abonat cu token →
  vede premium; anonim → locked), load balancing, notificări.

### PASUL 3 — Frontend rebranșat pe gateway (8085) + JWT
- În `/frontend`:
  - **Vite proxy** (`vite.config.ts`): `/api` → **`http://localhost:8085`** (gateway), NU 8081.
  - **`src/api/client.ts`:** scoate interceptorul CSRF (cookie `XSRF-TOKEN`/`X-XSRF-TOKEN`);
    adaugă un interceptor care pune **`Authorization: Bearer <token>`** pe cereri, citind
    token-ul din storage (localStorage/sessionStorage — decide; localStorage e simplu pt demo).
  - **`src/auth/AuthContext.tsx`:** la login, salvează token-ul din răspuns în storage; la
    logout, șterge-l; la load, dacă există token valid, consideră userul logat (decodează
    `/api/auth/me` cu token-ul SAU decodează claim-urile local). `register` rămâne.
  - Rutele protejate (`ProtectedRoute`) verifică prezența/validitatea token-ului.
- **Verificare (browser, prin preview pe 5173):** login → token salvat → navigare → **gating
  premium** (post blocat vs deblocat) → logout → token șters → rută protejată redirectează la
  login. Totul prin **microservicii (gateway 8085) cu JWT**, fără sesiune/CSRF.

---

## 3. PUNCTE DELICATE de reținut

- **CSRF se reconfigurează.** Cu JWT în header `Authorization` (NU cookie), atacurile CSRF
  clasice (care exploatează cookie-uri trimise automat) nu se mai aplică la fel → **CSRF
  cookie-based nu mai e necesar** pe fluxul JWT. În user-service: endpoint-urile de auth
  (login/register) pot rămâne fără CSRF dacă auth devine stateless, SAU păstrează login-ul
  ca POST simplu. **Documentează schimbarea clar.** (Downstream e deja CSRF-off.) Decide dacă
  user-service devine complet stateless (`SessionCreationPolicy.STATELESS`) — probabil DA
  pentru JWT pur (fără sesiune deloc).
- **Refresh token — DECIDE CU UTILIZATORUL.** Două opțiuni:
  - *(a) Doar access token* cu expirare rezonabilă (ex. 1–24h) + re-login. **Mai simplu**,
    suficient pentru un proiect academic. RECOMANDAT pentru viteză.
  - *(b) Access + refresh token* (refresh cu expirare lungă, endpoint `/api/auth/refresh`).
    Mai „ca la carte", dar mai mult de implementat (stocare/rotire refresh token). Întreabă
    utilizatorul ce preferă la începutul Pasului 1.
- **Cheia de semnare JWT:** secret **externalizat** (Config Server sau env var), **NU
  hardcodat în cod committed**. Acceptabil un secret DEV documentat ca atare (ca remember-me
  key-ul actual), dar ideal prin Config Server (`creatorhub.jwt.secret`) + env în Docker.
  HS256 (secret simetric) e cel mai simplu; RS256 (cheie publică/privată) e mai „enterprise"
  dar mai complex — pentru proiect, **HS256 cu secret partajat** (user-service semnează,
  gateway validează cu același secret) e suficient. Documentează.
- **Ordinea claim-urilor:** downstream are nevoie de `userId` (Long) — pune-l ca claim
  explicit (`userId`) sau ca `sub`. `roles` cu prefix `ROLE_` (ca să meargă `hasRole`).

---

## 4. CE NU SE STRICĂ (checklist)

- ✅ Monolitul (`src/`) — NEATINS.
- ✅ Mecanismul de headere downstream (`HeaderAuthenticationFilter`, `CurrentViewerService`,
  securitatea stateless a serviciilor) — se refolosește, doar SURSA la gateway se schimbă.
- ✅ Gating premium fail-closed, notificări fail-open, circuit breaker, load balancing,
  Config Server, monitoring — toate trebuie să meargă identic după JWT (re-verifică la fiecare pas).
- ✅ `main` / `dev` / `microservices` — rămân pe versiunea cu sesiune până JWT e validat 100%.

---

## 5. După implementare (la final, DUPĂ ce JWT merge end-to-end)
1. Commit-uri logice pe `jwt-auth` (jjwt în user-service, emitere token, validare la gateway,
   frontend rebranșat, docs).
2. Actualizează `CLAUDE.md`: secțiune nouă pentru JWT (cum semnează user-service, cum validează
   gateway-ul, schimbarea CSRF/sesiune→stateless, frontend pe 8085, cheia de semnare),
   actualizează blocul „STARE CURENTĂ" și §11/§17/§18 (nota „JWT mai târziu" devine „JWT
   implementat"). Marchează cerința JWT ca livrată.
3. Verificare finală completă în Docker + browser. Apoi sincronizează `main` + `dev` cu
   `jwt-auth` și urcă pe GitHub (ca la pașii anteriori: ff main/dev → branch, push).
4. Lecții de tooling: `git commit -F` (nu `-m`), Bash pentru ștergeri, `curl.exe`, fără ternar
   PowerShell — vezi `CLAUDE.md` §3bis.
