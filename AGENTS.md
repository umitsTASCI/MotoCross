# AGENTS.md

Guidance for coding agents working in **MotoCross (OEM-Cluster Pro)**.

## Web research (Exa)

This repo's agent sessions have Exa semantic web search. Use it for facts that live
outside the repository, not for code that is already here:

- OEM instrument-cluster layouts, TFT/NegLCD/analog-digital styles, shift-light behavior
  of the 105 catalog models (`app/src/main/java/com/oemcluster/pro/catalog/`,
  `.../renderer/Oem*Renderers.kt`).
- Motorcycle mechanical data: sprockets, gear ratios, primary reduction, tire sizes,
  idle/redline RPM, torque and power curves (`.../domain/BikeMechanicalProfile.kt`,
  `.../data/BikePresetCatalog.kt`).
- Current Android/NDK/Compose/CMake API behavior and release notes
  (prefer Exa, then Context7 for library docs).
- Release, regulatory, or unit-conversion facts that would otherwise be recalled
  from memory.

Do not use it to answer questions about this codebase, and do not use it where a
`grep`/`read` of the repo gives the answer.

### Tools available

| Tool | Use for |
| --- | --- |
| `exa_web_search_exa` (MCP) | Ranked web results with excerpts. Takes `query` **and** `objective`. |
| `exa_web_fetch_exa` (MCP) | Full page text for URLs you already have. |
| `websearch` (built-in) | Same Exa provider, one-shot search from the agent loop. |
| `POST https://api.exa.ai/search` | Scripted checks, batch lookups, JSON you want to parse. |
| `POST https://api.exa.ai/contents` | Extraction from known URLs, freshness control, per-URL status. |

### Rules

1. **Always send `objective` with a search.** It is required by the MCP tool and it is
   what makes the second search better than the first. State the goal of the step,
   which sources should rank first, what to exclude, and which facts to pull.
2. **Search is one request, not a form.** Send `query` plus `contents: { highlights: true }`
   and nothing else. Add a parameter only when this task needs it:
   `type: "deep"` for synthesis across many pages, `numResults` when the context
   budget is a deliberate choice, domain filters when the user asked for a hard
   allowlist/blocklist.
3. **Describe the page you want in natural language.** Subject, model name, year,
   document type ("owner's manual", "service manual", "spec sheet"), and the figure
   you need. Recency belongs in the query ("2026", "latest"), not in `maxAgeHours`
   (that controls crawl cache age, not publication date).
4. **Pick one content mode.** `highlights` by default; `text` only when broad page
   context is genuinely needed; `summary` only when Exa-side synthesis was asked for.
   Never stack them. On `/search` they live inside `contents`; on `/contents` they are
   top-level fields.
5. **Quote the source.** When a value in code, a preset, or a README table came from
   the web, keep the URL next to it (code comment, commit message, or the reply).
6. **Verify before trusting.** Cross-check the number against a second source, then
   write it into a test (`app/src/test/java/com/oemcluster/pro/BikeAndGaugeAuditTest.kt`)
   if it must stay correct.
7. **Don't build lists of companies, people, or long multi-hop research with `/search`.**
   That is the Exa Agent API (`POST https://api.exa.ai/agent`): set `effort` explicitly,
   poll until a terminal status, and check how the run ended before reading `output`.

### Raw request (Windows PowerShell)

The key lives in the `EXA_API_KEY` user environment variable. Never inline it.

```powershell
$body = '{"query":"2023 Yamaha MT-07 owner manual final drive gear ratio","contents":{"highlights":true}}'
Invoke-RestMethod -Uri 'https://api.exa.ai/search' -Method Post `
  -Headers @{ 'x-api-key' = $env:EXA_API_KEY } -ContentType 'application/json' -Body $body |
  Select-Object -ExpandProperty results | Select-Object title, url, @{ n = 'hl'; e = { $_.highlights[0] } }
```

`Authorization: Bearer $EXA_API_KEY` is accepted in place of the `x-api-key` header.

### Credentials

- `EXA_API_KEY` is a **user environment variable**; restart the editor or terminal after
  changing it.
- The Exa MCP server in `~/.config/opencode/opencode.json` reads it through
  `"x-api-key": "{env:EXA_API_KEY}"`. Never write the literal key into that file, into
  `.env` files, into `gradle.properties`, or into the app.
- The Android app has **no `INTERNET` permission** and speaks to no network. Adding an
  in-app Exa call is a product and security decision (key exposure in a distributed
  APK), not a refactor: it needs a proxy you control, plus `INTERNET` in
  `app/src/main/AndroidManifest.xml`. Do not add it on your own.

## Build

No Gradle wrapper scripts are committed (only `gradle/wrapper/gradle-wrapper.properties`).
CI installs Gradle 8.11.1 and calls it directly, so do the same locally:

```bash
gradle assembleDebug   # APK + C++20 NDK (JDK 17, NDK 27.0.12077973, CMake 3.22.1)
gradle test            # JVM unit tests (BikeAndGaugeAuditTest)
```

The Golden Rule of the README still applies: `BikeMechanicalProfile` (My Garage) is the
only mechanical source of truth, and the 105 catalog clusters are pure visual skins.
