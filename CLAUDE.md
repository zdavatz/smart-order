# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

SmartOrder (internally "AmikoRose") is a Play Framework 2.5 Java REST API for zur Rose. A client posts a
shopping basket of article codes; the service returns each article enriched with pricing, stock/shipping
status, and a **ranked list of therapeutic alternatives**. Almost all of the value — and almost all of the
complexity — lives in the alternative-selection and ranking rules.

## Build, run, test

Play 2.5.3 / sbt 0.13.17 / Scala 2.11.7. **This toolchain requires Java 8.** The default JDK on this machine
is 21 and sbt 0.13 dies on it immediately ("The Security Manager is deprecated"). Prefix every sbt/activator
command:

```sh
export JAVA_HOME=/usr/lib/jvm/oracle-jdk-bin-1.8
export PATH=$JAVA_HOME/bin:$PATH
```

```sh
sbt compile            # or: ./bin/activator compile
sbt run                # dev mode, hot reload; port 9001 (conf/application.conf)
sbt test
sbt "testOnly ApplicationTest"   # single test class
./smartorder.sh        # clean stage + copy rose/ into the staged dir + run the packaged binary
```

`sqlite-jdbc` is an unmanaged jar in `lib/` — it is not declared in `build.sbt`.

The two tests under `test/` are unmodified Play template boilerplate. `IntegrationTest` asserts on
"Your new application is ready.", which `MainController.index()` no longer returns, so it does not pass.
There is no real test suite; verification is done by hitting the endpoints.

Verify a change against a live server (see `MainController.test()` for the canonical shape):

```
GET /smart/full?pretty=on&authkey=<key>&glncode=<gln>&basket=(7680623190048,10)(7680573480114,5)
```

## Runtime data: the `rose/` directory

The repo ships almost no data. At startup `RoseData` (singleton, eagerly constructed via
`modules/OnStartModule` → `OnStartTasks`) loads every file from `./rose/`, and the two SQLite DBs referenced
in `conf/application.conf` must be there too. Populate it before the app is useful:

| File | Purpose |
| --- | --- |
| `rose_db_new_full.db` | main article DB (`rosedb` table) — lookup by EAN / pharma code |
| `rose_db_new_atc_only.db` | smaller ATC-indexed DB — **only** source for alternatives search |
| `rose_conditions_new.json` | customer id → `User` (GLN, generic-manufacturer preferences, `special_group`) |
| `rose_ids.ser.clear` / `rose_ids.json` | customer id ↔ GLN code |
| `rose_nota.ser.clear` / `rose_nota.json` | GLN → open back-order (nota) positions |
| `rose_auth_keys.txt` | valid `authkey` values (gitignored) |
| `rose_stock.csv` | `pharmacode;rose_stock;voigt_stock` — authoritative over the DB's stock column |
| `rose_settings.json` | `rose_preferences` → `Constants.rosePreferences` |
| `Grippeimpfstoff.json` | pharma codes forced to shipping status red |
| `rose_direct_subst.json` | pharma code → mandatory replacement pharma code |

`.ser.clear` files are Java-serialized maps; `RoseData` prefers a same-named `.json` and falls back to the
`.ser` blob.

**Hot reload:** `OnFileWatch` polls a `WatchService` on `./rose/` once a minute and calls
`RoseData.loadFile(name)`, which dispatches on the **exact filename**. Adding a new data file requires a new
branch in both `loadFile()` and `loadAllFiles()`. Note the DBs themselves are opened per request, not cached.

Order logs are appended to `./orders/<gln>/order_log_<gln>.csv` asynchronously by `OrderLogActor`.

## Request flow

`conf/routes` → `MainController.getSmartBasket(pretty, limit, authkey, glncode, basket, nota, override)`
(both `/smart/compact` with `limit=2` and `/smart/full` with `limit=0`; POST `/smart/full` funnels through
`getSmartBasketPost`).

1. Build `ShoppingRose(glncode)` — resolves customer id ↔ GLN and loads that customer's `User` preferences.
2. `checkAuthKey`. **Auth failure returns `ok("[]")`, not 401** — same for an unknown GLN. Silent empty
   results are the normal failure mode throughout this codebase.
3. If `basket` is empty and `nota=de|fr`, synthesise the basket from the customer's open nota positions.
4. Parse `basket` with the regex `\((\d+),(\d+)\)`; codes shorter than 7 digits are zero-padded to a
   pharma code.
5. `searchSingleEan` per code against the full DB → `GenericArticle`.
6. `listSimilarArticles` per article: ATC-prefix query against the **ATC-only** DB, then filter.
7. `ShoppingRose.updateShoppingCart()` sorts, filters again, and maps to `RoseArticle` / `RoseOrder`.
8. Serialise with Jackson, fire-and-forget the CSV log actor, return.

`MainController.cursorToArticle` maps the `rosedb` result set **by column index** (1–25). Any change to the
DB schema silently shifts every field.

## Where the business rules live

Three separate stages decide what a client sees. When a bug report says "wrong alternative", identify which
stage is at fault before editing.

1. **Candidate selection** — `MainController.listSimilarArticles`. Same ATC prefix, not off-market, plus a
   size/dosage similarity check (`checkSimilarity`: pack size within 90.1%, unit string equal ignoring
   spaces/case, with a numeric-only fallback in `compareDosageWithoutUnit`). Originals that are green when
   the ordered article is not are collected separately and only the best one is appended. An
   "Ersatzartikel" (`replace_pharma`) is force-added if missing.
2. **Ordering** — `ShoppingRose.sortSimilarArticles`, a chain of `sortX` comparators applied until one
   returns non-zero: customer preference → (medix only) zur Rose preference → original → same product other
   pack size → shipping status → dosage → size → zur Rose preference. The German comment block above the
   comparator is the authoritative spec.
3. **How many to show** — `ShoppingRose.generateAlt` returns the `alt` field: the number of alternatives the
   GUI should display *by default*. The `alternatives` array is often longer. `limit` (2 for `/smart/compact`)
   truncates the array itself.

`updateShoppingCart` applies a final filter: a direct substitution short-circuits everything (exactly one
alternative, `alt` unset); otherwise alternatives that are off-market, unavailable, or compute to shipping
status red/black are dropped.

`override=dm` (dailymed) sets `showAllAlternatives`, which bypasses the original/generic distinction in both
stage 1 and stage 3 and sets `alt` to the full list size.

### Shipping status

`ShoppingRose.shippingStatus` returns an int, serialised via `shippingStatusColor`:
`1` green, `2` orange, `3` red, `4` half-green, `10` black. Stock is the sum of zur Rose and Voigt stock from
`rose_stock.csv` (falling back to the DB column); blacklisted pharma codes are hard-coded to red.
Beware: `sortSimilarArticles` and `generateAlt` both test `status == 4 || status == 5` for "orange or red" —
5 is not a status this function produces.

### Customer preferences

`User.neu_map` maps a generic-manufacturer *name* → 1. `GenericArticle.eanNameMap` maps the four supported
manufacturers to names — despite the name, its keys are matched against **author GLN codes**
(`isEanPreferred(article.getAuthorGln())`), which is why `setAuthorGln` reads the column formerly used for
replacement EANs. `special_group` containing "medix" activates a separate branch of the ranking rules.

## Conventions

- Changes trace back to issues at github.com/zdavatz/smart-order. The established pattern is a `fix-NNN`
  branch, a PR, and an inline comment linking the issue URL at the code that implements it. Follow it —
  those comments are the only documentation for most rules.
- `// UseCase N` comments refer to an external spec document, not to anything in the repo.
- `app/models/Medication.java` is dead code from AmiKo.

## Swagger

`swagger.yml` is **generated** — edit `swagger.yml.template` instead, then regenerate with schemas from the
sibling [ymeteor](https://github.com/zdavatz/ymeteor) repo:

```sh
./bin/update-swagger ../ymeteor/public/schema
```

The `/cockpit/*.json` endpoints in the spec are static files served by `CockpitAssets` from `./cockpit/`
(gitignored, also produced by ymeteor), gated by the same `authkey` list.
