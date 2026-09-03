# Rick & Morty — Feature Backlog

Source of truth: `doc/RickAndMorty_App_Spec.pdf`. Every feature below is implemented
**offline-first**: Room is the single source of truth, the UI reads only from the local
database, and the network's job is to fill and refresh that database — never to feed the
UI directly.

Work lands one phase at a time, each a self-contained commit. Tick a row when its phase
ships with tests passing.

---

## Architecture rules that apply to every feature

| Rule | What it means in code |
|---|---|
| Room is the single source of truth | Screens collect a `Flow`/`PagingData` off a DAO. No screen ever observes a Retrofit call. |
| Paging is DB-backed | `Pager(pagingSourceFactory = dao::pagingSource, remoteMediator = <Name>RemoteMediator)`. `PagingSource` implementations that hit the network are not used. |
| Remote keys are query-keyed | One shared `remote_keys` table, `@PrimaryKey queryKey: String` — `"character"`, `"character:status=alive&name=beth"`, `"location:type=Planet"`. Filtered results are cached and readable offline too. |
| Errors are typed | Everything goes through `safeApiCall` → `Resource`/`DataError`; Paging carries them as `DataErrorException`. Wording lives in `core/presentation/DataErrorMapper`. |
| MVI per screen | `<Name>UiState` (flat data class) + `<Name>UiEvent` (sealed) + a single `onEvent`. Loading/error/empty for paged lists are read from `LazyPagingItems.loadState`, never duplicated into state. |
| Layer ownership | DTO (data/remote) → Entity (data/local) → Domain model (domain). Mappers live in `data/mapper`. |
| Refresh policy | Always refresh on screen open (`LAUNCH_INITIAL_REFRESH`) + pull-to-refresh. |
| **404 on a filter is empty, not an error** | Spec §8: `GET /character/?name=zzzzzz` → `404 {"error":"There is nothing here"}`. Render the empty state. Reserve the error state for 5xx and connectivity failures. |
| Cached content wins over an error | Offline, `LoadState.refresh` is `Error` while Room still holds rows. When `itemCount > 0`, show the cached list plus an error banner — **never** a blocking full-screen error. |

### Package layout per feature

```
feature/<name>/
  data/
    local/      entity/  dao/
    remote/     <Name>ApiService.kt  dto/
    mapper/     dto → entity → domain
    paging/     <Name>RemoteMediator.kt
    repository/ <Name>RepositoryImpl.kt
  domain/       model/  repository/  usecase/
  presentation/ <Name>Screen.kt  <Name>ViewModel.kt  <Name>UiState.kt  <Name>UiEvent.kt
  di/           <Name>Module.kt
```

---

## Cross-cutting (`X`) — Phase 1 & threaded through the rest

| ID | Feature | Mechanism | Phase | Done |
|---|---|---|---|---|
| X1 | Room as single source of truth | `RickAndMortyDatabase`, one entity + DAO per resource, `Converters` for `List<Int>` | 1–2 | [ ] |
| X2 | Offline caching of paged lists | `RemoteMediator` + shared query-keyed `remote_keys` table | 1–2 | [ ] |
| X3 | Favorites — bookmark characters | Separate `character_favorites` table, joined against cached characters | 5 | [ ] |
| X4 | Pull-to-refresh on any list | `PullToRefreshBox` → `LazyPagingItems.refresh()` | 2 | [ ] |
| X5 | Batch fetch of related items | `GET /character/{1,2,3}` and `/episode/{ids}` — **a single id returns an object, several return an array**; one shared helper handles both | 4 | [ ] |
| X6 | Loading / empty / error states | Shared `PagedContent` composable in `core/ui` encodes the decision table once | 1–2 | [ ] |
| X7 | Bottom-tab navigation | Type-safe `@Serializable` routes, 4 tabs with independent back stacks | 1 | [x] |

---

## Characters (`C`)

| ID | Feature | Screen | Endpoint | Notes | Phase | Done |
|---|---|---|---|---|---|---|
| C1 | Character list with infinite scroll | S1 | `GET /character?page={n}` | The reference implementation every later list copies. Avatar via Coil, status dot, 20/page fixed by the API. | 2 | [ ] |
| C2 | Search by name + filter by status / species / gender | S2 (same screen as S1) | `GET /character/?name=&status=&species=&gender=` | ~300 ms debounce, `flatMapLatest` rebuilds the pager per query. Filters combine. 404 → empty state. | 3 | [ ] |
| C3 | Character detail | S3 | `GET /character/{id}` | Reads `dao.observeById(id)`; a one-shot refresh upserts fresh data. Opens from cache offline. Origin & last location link to S7. | 4 | [ ] |
| C4 | Episodes a character appears in | S3 | `episode[]` → `GET /episode/{ids}` | Ids parsed from URL tails (`url.substringAfterLast('/')`). Chips navigate to S5. | 4 | [ ] |

## Episodes (`E`)

| ID | Feature | Screen | Endpoint | Notes | Phase | Done |
|---|---|---|---|---|---|---|
| E1 | Episode list, grouped by season | S4 | `GET /episode?page={n}` | 51 episodes / 3 pages. Season derived from the `S01E01` code. | 6 | [ ] |
| E2 | Episode detail | S5 | `GET /episode/{id}` | Name, air date, code. | 6 | [ ] |
| E3 | Search by name or episode code | S4 | `GET /episode/?name={q}` · `?episode=S01E01` | Same query-keyed caching as C2. | 6 | [ ] |
| E4 | Featured cast grid | S5 | `characters[]` → `GET /character/{ids}` | Reuses the character card and the batch helper (X5). Taps open S3. | 6 | [ ] |

> The Episode **data layer** (entity, DAO, api, mapper, `GetEpisodesByIdsUseCase`) lands
> early in Phase 4 because C4 needs it; Phase 6 adds the paging and the screens on top.

## Locations (`L`)

| ID | Feature | Screen | Endpoint | Notes | Phase | Done |
|---|---|---|---|---|---|---|
| L1 | Location list | S6 | `GET /location?page={n}` | 126 locations / 7 pages. | 7 | [ ] |
| L2 | Location detail | S7 | `GET /location/{id}` | Type, dimension. | 7 | [ ] |
| L3 | Filter by name / type / dimension | S6 | `GET /location/?type=Planet&name=earth` | Same query-keyed caching as C2. | 7 | [ ] |
| L4 | Residents grid | S7 | `residents[]` → `GET /character/{ids}` | Taps open S3. Enables the origin / last-location links from C3. | 7 | [ ] |

## Favorites (`X3`)

| ID | Feature | Screen | Source | Notes | Phase | Done |
|---|---|---|---|---|---|---|
| X3 | Locally saved characters | S8 | Room only | No network path at all — works offline by construction. A refresh must **not** evict a favorited character row, or the favorite vanishes from this tab. | 5 | [ ] |

---

## Screens (spec §3)

| # | Screen | Route | Reached from |
|---|---|---|---|
| S1/S2 | Character List + Search & Filter | `Characters` | Tab 1 |
| S3 | Character Detail | `CharacterDetail(characterId)` | S1, S5, S7, S8 — declared once |
| S4 | Episode List | `Episodes` | Tab 2 |
| S5 | Episode Detail | `EpisodeDetail(episodeId)` | S4, S3 |
| S6 | Location List | `Locations` | Tab 3 |
| S7 | Location Detail | `LocationDetail(locationId)` | S6, S3 |
| S8 | Favorites | `Favorites` | Tab 4 |

Bottom tabs: **Characters · Episodes · Locations · Favorites**. Detail screens are pushed
onto the active tab's back stack.

---

## Delivery phases

| Phase | Contents | Status |
|---|---|---|
| 0 | This document | [x] |
| 1 | Foundation — Room, shared `remote_keys`, `core/ui` state composables, type-safe navigation + bottom bar, Coil, package restructure, per-feature DI | [x] |
| 2 | C1 — character list, offline-first (X1, X2, X4, X6) | [ ] |
| 3 | C2 — search & filter | [ ] |
| 4 | C3, C4 — character detail + episode data layer (X5) | [ ] |
| 5 | X3 — favorites | [ ] |
| 6 | E1–E4 — episodes | [ ] |
| 7 | L1–L4 — locations | [ ] |

Per-phase gate: `./gradlew :app:assembleDebug` and `:app:testDebugUnitTest` pass, plus a
manual offline check — load online, kill the app, enable airplane mode, relaunch, and
confirm the screen still renders from Room.
