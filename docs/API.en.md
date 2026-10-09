# Little LUMI Model — Plugin API reference (api_version 1)

This reference describes **SDK/app 1.5.0, `api_version` 1**. It is the whole surface a plugin may rely on: only what is written here is promised to survive an app update (see the [compatibility policy](#compatibility)). `javadoc\index.html` in this folder carries the signatures and comments of the types below verbatim, and `api-src\` holds those source files. Start with the [walkthrough](../plugin-guide.en.html); look identifiers up in the [catalogue](CATALOG.en.md). [한국어](API.md).

[Manifest](#manifest) · [Lifecycle](#lifecycle) · [Methods](#context) · [Events](#events) · [Hooks](#hooks) · [Bytecode](#bytecode) · [Data fragments](#data-mods) · [Compatibility policy](#compatibility) · [Do not](#practices) · [Testing](#testing) · [Catalogue](#catalogue) · [Outside the promise](#alternatives) · [Recipes](#recipes) · [Character authoring](#character-authoring) · [Live preview](#authoring-preview) · [Linked activities](#animation-activities) · [Held motion](#held-motion) · [Workshop publishing](#workshop-publishing)

| Type (`com.group_finity.mascot.lumi.plugin`) | Role |
|---|---|
| `LumiPlugin` | entry interface — `start(PluginContext)` / `stop()` |
| `PluginContext` | the door the app hands you — speech, behaviour, state queries, control, events, hooks, settings, UI rows, hotkeys, bytecode |
| `MascotState` | one character's current state (id, image set, rectangle, feet, facing, behaviour; desktop coordinates) — the record `ctx.mascots()` returns |
| `PcmAudio` | one piece of audio to play (format, PCM bytes, length in ms) — what `ctx.speak()` takes |
| `MicCaptureLease` | a closeable hold that suppresses LUMI voice output during microphone recording — returned by `ctx.beginMicCapture()` |
| `HotkeyResult` | the answer of `ctx.bindHotkey()` — `OK` `NONE` `BAD_COMBINATION` `REFUSED` `UNSUPPORTED` |
| `PluginTheme` | the app's look — colours, the CJK-capable font, button/toggle/slider dress, card pieces (`ctx.theme()`) |
| `Json` | dependency-free JSON read/write (`parse` `get` `getString` `write`) — for LLM replies and settings files |
| `PluginEvents` | the "told after it happened" bus (topics and payloads in §4) |
| `PluginHooks` | "asked before deciding" (rules and catalogue in §5), `PluginHooks.Handler` |
| `LumiTransformer` | load-time class transformer service |
| `PluginDescriptor` | the parsed `plugin.json` |
| `PluginPrefs`, `PluginText` | your own settings store (`ctx.prefs()`) and your own five-language strings (`ctx.text()`) |
| `PluginUi.MenuHandle`, `PluginUi.FeatureToggle` | handles of menu rows and buttons, and the immediate feature switch |
| `ScheduledHandle` · preview | cancel a scheduled task, or ask whether it was cancelled |
| `PluginSettingsSchema` | the declarative-settings parser — ordinarily you only declare them in `plugin.json` |
| `PluginTestKit` · preview | a fake context, call records and a clock the test drives (§10) |
| `WorkshopContent`, `WorkshopContent.PromptSource` | Read-only Workshop creator files and subscription identities |

| `WorkshopPublisher` | User-confirmed Workshop publishing, updates and upload verification |
| `VisualLogic`, `CharacterDefinition` · preview | Visual rules and original character generation |
| `CharacterTemplates` · preview | Installed behavior, action and motion templates |
| `AuthoringPreview`, `AnimationActivities` · preview | Live engine previews and linked-activity queries/tests |
| `TerrainRect`, `TerrainHandle` · preview | Plugin terrain geometry, registration, updates and removal |


### Workshop creator data

`WorkshopContent.promptSources(imageSet)` returns an immutable list of `PromptSource` records in mod order (later providers win collisions). Each contains `promptsFile()` and `personaFile()` (`Path`, read-only, possibly absent), `workshopId()`, `subscriptionId()` and `modStyleName()` (preserve the persona label including its Workshop ID). Handle missing/unreadable files. Disabled, unsubscribed and unapproved providers are excluded; after startup only the prepared inventory is returned. Invalid character names or missing Workshop support return an empty list.

Bind an explicitly selected creator preset to both IDs: updates, toggles and temporary download waits retain subscription identity; unsubscribe/resubscribe changes it and requires a new selection. Querying never selects a prompt or grants execution consent. `WorkshopContent.hasDirectSource(id)` includes unsubscribed records and lets readers suppress old mirror fallback; it does not indicate enabled/approved status. Both methods query host state and do not require a plugin context.

<a id="manifest"></a>

## 1. plugin.json

At the root of the jar. The only thing the parser insists on is `id`. A missing or blank `name` shows the id instead, so give a distribution a readable name.

| Key | Type | Meaning |
|---|---|---|
| `id` | string | `[a-z0-9._-]`, 1..64 (the first character a lowercase letter or digit). Name of `plugindata\<id>\`, the logger, the hotkey prefix |
| `name` | string | what the Mods tab shows. The `id` when absent |
| `version` | string | the "v…" chip in the Mods tab |
| `author`, `description` | string | display only |
| `dependencies` | array | plugin ids that must load first; a missing one skips this plugin |
| `api_version` | integer | this document's compatibility number (§8). Defaults to `1` |
| `app_version` | string | the app version you tested against. When the running app differs, the Mods tab shows an informational chip; nothing is blocked |
| `affects_character` | boolean | changes behaviours or lines (chip) |
| `network` | boolean | talks to the internet (chip; not enforced) |
| `hooks` | array | hook ids it answers. Ids this app does not know are shown in the Mods tab, never blocked |
| `settings` | array | **declarative settings**. The Mods tab draws them under the plugin's row (「자세히」) in the app's theme and language; values are saved **immediately** to `ctx.prefs()` (`plugindata\<id>\settings.properties`; a failed save: §3.7). Entry: `key` (required) · `type` = `bool` `int` `string` `choice` `hotkey` · `label`/`hint` = one string or `{"ko":…,"en":…,"ja":…,"zh":…,"zh_TW":…}` (exact language → language → `en` → first value; Traditional never falls back to `zh`) · `default` · `min`/`max`/`step` (int) · `options` (choice: `[{"value","label"}]` or strings). A bad entry is logged and skipped, never fatal. Defaults are seeded before `start()` **only for keys the store does not have yet** — a value the user changed is never overwritten |

```json
{
  "id": "my.first.lumi",
  "name": "My first LUMI plugin",
  "api_version": 1,
  "app_version": "1.5.0",
  "settings": [
  { "key": "greet",  "type": "bool",   "default": true, "label": { "ko": "인사하기", "en": "Greet" } },
  { "key": "volume", "type": "int",    "default": 50, "min": 0, "max": 100, "step": 5, "label": "Volume" },
  { "key": "mood",   "type": "choice", "default": "calm", "label": { "ko": "말투", "en": "Mood" },
    "options": [ { "value": "calm", "label": { "ko": "차분", "en": "Calm" } }, { "value": "wild", "label": "Wild" } ] },
  { "key": "toggle", "type": "hotkey", "default": "Ctrl+Alt+L", "label": "Toggle key" }
  ]
}
```

Plugin side: `ctx.prefs().getBoolean("greet", true)`, `ctx.prefs().onChange((key, value) -> …)`. A `hotkey` field stores the combination text only — register it yourself from `onChange` with `bindHotkey` (that is when you learn whether another program holds it).

The entry class goes in `META-INF\services\com.group_finity.mascot.lumi.plugin.LumiPlugin` (`ServiceLoader`); a transformer in the `…LumiTransformer` service file.

<a id="lifecycle"></a>

## 2. Lifecycle

Code and data may share one mod package. Approval covers the origin and the names and contents of executable files; changes to ordinary images or audio alone do not require renewed approval. All companion files under `plugins/runtime/` are execution inputs. On Windows, executable contents are not copied: a separate execution path uses hardlinks protected by handles that deny writes and deletion. Ordinary data is read from its original location and validated when used. Disabled or unapproved code packages contribute neither code nor data. Change mod files while the app is closed.

Place Windows x64 DLLs under the mod root's `native/` or `audio-native/` directory. Bundle helper interpreters and tools with their companion files under `plugins/runtime/`. The entire directory is executable content; EXE, DLL and PYD files undergo PE format checks. The protected execution view preserves relative directories, with the corresponding `native/`, `audio-native/` and `runtime/` directories beside its classpath JARs. The app does not run these tools automatically. Code changes require renewed approval. Do not assume the original installation path or JAR filename is the execution path.

- `start(ctx)` — called once after the built-in features boot, **before** the engine (mascots) loads. Register `on`/`hook`/`addTrayItem`/`onLoad` here. Character state may still be empty at this point, so leave engine-dependent work to `ready`. Throwing marks only this plugin as failed.
- `ready` event — once, after the engine is up and every plugin's `start` has run; the app publishes it. `PluginTestKit` does not publish it for you (§10).
- `stop()` — at app exit (the default implementation is empty). It is also called right away when `start()` throws, to clean up a partial start, so tolerate fields that were never initialised. Plugins, and the service implementations inside one jar, stop in reverse order of their actual starts.
- After `stop()` **the app cleans up**: every listener, hook, global hotkey, menu row, feature toggle and scheduled task registered through this context is released by the loader, whether or not the plugin did it. A failed start is cleaned up immediately and dependents of a failed plugin are not started. There is no runtime enable/disable (the Mods-tab switch takes effect on the next launch).
- **What you created, you close**: windows, clients, streams and worker threads go in `stop()`. **A `prefs().onChange(...)` listener is not reclaimed by the context either** — it is not in the registration ledger, so keep the listener and release it with `removeOnChange(listener)` in `stop()` (§13 `QuietPlugin`). Classes you already transformed are not rolled back.
- **State is put back too**: the `claimClickGreeting()` claim is dropped only if this plugin still holds it, characters it `summon()`ed and has not `dismiss()`ed are sent away, and characters it left hidden with `setCharactersVisible(false)` are shown again. The last two apply only **while the app keeps running** (a failed start); at app exit the characters are left alone.
- From then on **the context is dead**: a late registration (`on`, `hook`, `every`, `after`, `onEdt`, `offEdt`, a menu row, `addSettingsButton`, `addFeatureToggle`, `bindHotkey`, `claimClickGreeting`, `onLoad`, `patch`, `ask`, `summon`, `dismiss`, `moveTo`, `setCharactersVisible`) and every action door (`say`, `sayQueued`, `showBusy`, `behave`, `speak`, `mouthFlap`, `heart`, `playSound`, `awardAffection`, `publish`, `setSetting`, `writeAtomic`, `manageWindow`) is ignored **without throwing** — the log gets one line per plugin, `<id>: registration after stop() ignored: <what>`, and the return value is inert (an already-cancelled `ScheduledHandle`, a `MenuHandle`/`FeatureToggle` whose `removed()` is true, `HotkeyResult.REFUSED`, -1 from `summon`, false from `dismiss`/`moveTo`/`onLoad`/`patch`). An `ask` callback gets `-2`, and only in this case it is delivered **inline on the calling thread**, not on the dedicated thread of the normal path. Reads such as `mascots()`, `prefs()` and `text()` keep answering. Exceptions: `beginMicCapture()` throws `IllegalStateException` after context shutdown. `every()`/`after()` check `task == null` (and `every`'s `periodMs <= 0`) before they check release, so they throw `IllegalArgumentException` on a dead context too. Stop your threads in `stop()` and you will never see that line.
- Two plugins with the same id: the **lower** one in the Mods list loads, the upper one is marked DUPLICATE. `dependencies` orders startup only; it is not library isolation.
- `plugins\lib\*.jar` (the app's and every enabled mod's) go on the class path — no fat jar needed. The core jar is always first, so core classes cannot be shadowed.

<a id="context"></a>

## 3. PluginContext

A "· preview" chip after a name marks a **preview** item of §8 — it may change or disappear without an `api_version` bump. Unmarked items are **stable**. The same list is under `preview` in `sdk.json`.

| Method | What it does |
|---|---|
| `id()`, `descriptor()`, `log()` | your id, your `plugin.json`, a logger (lands in the app log as `plugin.<id>`) |
| `dataDir()` | `plugindata\<id>\` — keys, memory, anything of yours. Your own window wipes it |

### 3.1 Bubbles and choices

| Method | What it does |
|---|---|
| `say(text, ms)`, `sayFor(imageSet, text, ms)` | a bubble. `ms <= 0` = default length |
| `hideBubble()`, `showBusy(imageSet)` | take the bubble down · the 「···」 thinking bubble (up to 90 s, replaced by the next line) |
| `sayTo(mascotId, imageSet, text, ms)`, `showBusyFor(mascotId, imageSet)` | to **one specific character** — with two Lumis of one set, `sayFor` goes to the first. `mascotId` is `MascotState.id` (the `mascotId` of `click`/`doubleclick`); falls back to the set's first if that one is gone |
| `sayQueued(text, ms)`, `sayQueuedFor(imageSet, text, ms)` · preview | **queue**: shows after the current bubble has gone down (250 ms gap), in order — for ordered guidance. `say` shows at once, replacing, and leaves the queue alone. 20 lines at most (the oldest is dropped beyond that). Every line passes the `bubble.text`/`bubble.duration` hooks and raises the `bubble` event. While a choice bubble is open the queue waits |
| `clearQueue()`, `queueLength()` · preview | drop this plugin's waiting lines (the showing one is `hideBubble`; its choice bubbles still **waiting** in line get `-2` too) · lines waiting, all plugins |
| `ask(text, options, timeoutMs, onPick)`, `ask(text, options, timeoutMs, maxWaitMs, onPick)` | **choice bubble**: the question plus 2–4 buttons (more are cut). `onPick` is called **once**, on a thread of its own, never the EDT: the option index (0-based) · `-1` = it opened and **the user gave no answer** (timeout, Escape, character gone) · `-2` = **the app withdrew it** before the user answered (this plugin's newer `ask` replaced it, open or waiting · `clearQueue()` · it waited in line longer than `maxWaitMs` · the line was full). **Two clocks**: `timeoutMs` starts when the bubble **opens** (time spent waiting never counts); `maxWaitMs` starts when `ask` is called (the four-argument overloads use `DEFAULT_ASK_WAIT_MS` = 30 s; `<= 0` means the same). One choice bubble shows at a time and the rest **wait in line**: asks from different plugins queue up in order and the next opens when the open one settles; a plugin holds one place in that line — its newer `ask` replaces its own open or waiting one (`-2`), never another plugin's. The question passes `bubble.text` (`source=choice`). `bubble` (`kind=choice`) fires when it opens, `choice` when it is answered. `timeoutMs <= 0` waits for the user. It does not steal keyboard focus when it opens (Escape works after a click on it). The one exception is an `ask` on a released context (§2): it calls `onPick(-2)` **inline on the calling thread**. Example in §13 |
| `askFor(imageSet, text, options, timeoutMs, onPick)`, `askFor(imageSet, …, maxWaitMs, onPick)` · preview | the same rules, asked of that set's first character |

### 3.2 Behaviour

| Method | What it does |
|---|---|
| `behave(name)`, `behaveFor(imageSet, name)` | force a behaviour by name (like `behave.txt`). Unknown names only warn |
| `currentBehavior()`, `currentBehavior(imageSet)` | current behaviour name |
| `behaviorNames(imageSet)` → `Set<String>` · preview | every behaviour name the set's `behaviors.xml` knows (what `behave` accepts). Empty for a set that is not loaded. For auto-built menus |

### 3.3 State queries

All read-only. Before the engine is up they return empty values or defaults, never an exception.

| Method | What it does |
|---|---|
| `mascots()` → `List<MascotState>` | every live character (props excluded). `MascotState` = `id`, `imageSet`, `bounds`, `anchor` (feet), `lookRight`, `behavior` — **desktop coordinates** (the space Swing windows are placed in), a copy of that instant |
| `mascotsIncludingProps()` · preview | `mascots()` plus props (the credit items during a gift, for example) |
| `mascot(imageSet)`, `mascotById(id)` | pick one — the set's first character · that id (the same value as `mascotId` in the `click`/`mascot` events). `null` when absent |
| `mascotAt(point)` · preview | the character whose picture covers that desktop point. `null` when none |
| `affectionTier()`, `affectionProgressPercent()`, `affectionPointsUntilNextTier()`, `daysTogether()` · preview | affection tier (1–6), progress inside the tier (%), points to the next tier, days together (cumulative) |
| `affectionTodayRemaining(source)` · preview | points that source can still earn today. App sources: `pat` `throwRecover` `climb` `social` `pomodoro` `alarm`; `null`/blank = **this plugin's own** allowance (`awardAffection`) |
| `focusActive()`, `focusSources()`, `screenBusy()` · preview | focus mode on? · by which routes (`manual`,`pomodoro`,`auto`, comma-separated; `""` when none) · a fullscreen program or meeting is in front |
| `bubbleVisible()`, `charactersHidden()` | a bubble is showing · the characters are hidden (boss key, manual, auto-hide) |
| `featureEnabled(id)` | the settings-window feature switches — `bongo` · `broadcast` · `island` · `game` · `focus.auto` (CATALOG §7). False when off or for an unknown id. Read only — the switches belong to the user |
| `language()` | the app's UI language — one of `ko` `en` `ja` `zh-CN` `zh-TW`. Pick your own bundle by it. Correct from `start()` on: it is the language the user chose (the settings file's `Language`), or on a first run the one picked from the OS locale |
| `installedImageSets()`, `activeImageSets()` · preview | character folders under `img\` (props excluded, name order) · the sets switched on now (`ActiveShimeji`, props included) |
| `screens()` → `List<Rectangle>` · preview | every monitor's rectangle, same space as `MascotState.bounds` |
| `quietNow()` | **is now a time to keep quiet** — hidden, a fullscreen app in front, focus mode, plus the `focus.quiet` hook's answer. The app's self-talk, greetings and celebrations all ask this one question; a plugin that speaks up on its own should too |
| `foregroundScreen()` → `Rectangle` or null | the monitor the **foreground window** is on (AWT/`Robot` coordinates); null when there is none or it touches no monitor. `screens()` cannot tell you this — only the app knows which window the user works in |
| `isCharacter(imageSet)` | a character, not a prop (credit items) — pass it as a character-menu row's `forImageSet` |
| `persona(imageSet)` → `String`, `personaFile(imageSet)` → `Path` | the character's **persona text** (identity, voice, taboos) from `img\<Set>\persona.txt`; Lumi falls back to the app's own copy. Empty when none (write your own stand-in). No output-format rules in it — every consumer appends its own. The file path is for a settings window that lets the user edit it |

### 3.4 Character control

Every one of these is a door to something the app already does — no new rules.

| Method | What it does |
|---|---|
| `summon(imageSet)` → int · preview | one more character (the tray's own summon, shiny roll included). **Active sets only**; otherwise -1. Returns the newborn's id |
| `dismiss(mascotId)` → boolean · preview | send that character away (the menu's Dismiss, without its sound). False when the id is gone |
| `moveTo(mascotId, x, y)` → boolean · preview | teleport its feet to a desktop point. It is switched to `Fall` first, so it settles on whatever is below. False when the switch was refused (nothing moved — try next tick) |
| `setCharactersVisible(boolean)` · preview | hide all / bring back — the boss key's own switch (a later boss-key press or auto-hide behaves as if the user did this). EDT |
| `awardAffection(source, points)` → int · preview | credit affection points (gifts, quests). Goes through the app's **one award gate**: the `affection.award` hook sees it first, a cap of **2 points per plugin per day** (any number of `source` labels share it), never subtracts. Returns the points actually credited |
| `heart(imageSet)` · preview | pop the head-pat heart over that character's head |
| `playSound(path)` → boolean · preview | one effect sound through the app's player (honours the user's volume, the per-sound mute list and the `sound.effect` hook). Path relative to the app root (`img/Lumi/sound/jump.wav`) or a file in your mod folder. False when sounds are off |

### 3.5 Voice

One voice line, shared by everyone.

| Method | What it does |
|---|---|
| `speak(PcmAudio, onStart, onEnd)`, `stopSpeaking()`, `isSpeaking()` | PCM playback. A new `speak` cuts the previous one (never overlapping a voice pack), the volume is the shared knob (`setSpeechVolume`), the `tts` event fires around it. `onStart`/`onEnd` run on the player thread |
| `setSpeechVolume(percent)` | the **shared** voice volume (0–100) — one knob for every plugin and voice pack; drive it from a setting the user sees |
| `mouthFlap(imageSet, whileSpeaking)`, `stopMouthFlap()` | **mouth movement** — while `whileSpeaking` says true (the `Talk` behaviour is re-run every 1.1 s), then stops by itself. Pair with `speak`: `ctx.mouthFlap(set, ctx::isSpeaking)` |
| `mouthFlapFor(imageSet, ms)` · preview | mouth movement for a fixed time — for a line whose audio you do not play yourself |
| `beginMicCapture()` → `MicCaptureLease` | Call **off the EDT**, before opening the microphone. Waits for the actual voice output line to close. While held, new voice packs, TTS, previews and other plugins' voices are discarded. Close the input device before `lease.close()` (no checked exception). Nested/multiple owners' holds must all close before new playback is accepted; discarded voices never replay. Context shutdown releases its own holds and cancels pending acquisition. Throws `IllegalStateException` on the EDT, after shutdown, on interruption or if the output line cannot close. Does not mute other apps/effects or grant microphone permission |

### 3.6 Events and hooks

| Method | What it does |
|---|---|
| `publish(topic, k, v, …)` | **plugin-to-plugin** events. The topic is forced to `<id>.<topic>` (left alone if already prefixed), so app topics cannot be forged. The payload carries `from` = your id. The other side listens with `on("<that id>.<topic>", …)` |
| `on(topic, listener)`, `off(...)` | Events (§4). Listeners run in order on **this plugin's own thread** (`lumi-plugin-events-<id>`) — a slow plugin never delays another's events. A throwing listener is logged and the next event still arrives |
| `hook(id, handler)`, `unhook(...)` | hooks (§5). Handlers run **synchronously on the caller's thread** |

### 3.7 Settings, strings and files

| Method | What it does |
|---|---|
| `prefs()` → `PluginPrefs` | **your own settings store** — `plugindata\<id>\settings.properties` (plain UTF-8 text — keep secrets out of it; those go through `writeAtomic(…, true)`). `get`·`getInt`·`getLong`·`getBoolean(key, default)`, `set(key, String/int/long/boolean)` (ignored when equal; otherwise saved synchronously right there — a failed save is logged as a WARNING and never thrown; on a file system without atomic moves the file is replaced plainly), `remove`, `keys()`, `has`, `onChange((key, value) -> …)` (after a real change, in order on **one notifier thread shared by every plugin** (`lumi-plugin-prefs`, off the EDT); a removed key arrives as `null`), `removeOnChange(listener)`, `reload()`. **An `onChange` listener is not reclaimed by the context** — release it yourself in `stop()` (§2). The `plugin.json` `settings` card writes here too |
| `setting(key, fallback)`, `setSetting(key, value)` | the app's `conf\settings.properties`. For **reading** — app keys belong to the app, do not change them (the settings window's next apply may overwrite yours). Your own state goes to `prefs()`. If you must use `setSetting`, prefix the key with your id |
| `localized(key, fallback)` | the app's 5-language bundle. Ship your own bundle for your own strings |
| `text(key, fallback)`, `text(key, fallback, args…)` → String · `text()` → `PluginText` | the plugin's **own five-language strings**: `plugin_ko.properties` `plugin_en` `plugin_ja` `plugin_zh` `plugin_zh_TW` + the base `plugin.properties` at the jar root, UTF-8. Picked by `language()`, falling back `exact → en → base file → app bundle → fallback` (zh-TW never falls to zh). With `args`, `MessageFormat` (`{0}`) |
| `writeAtomic(path, bytes or String, ownerOnly)` | writes a temp file in the same folder, then swaps it in with an atomic move (on a file system without atomic moves the file is replaced plainly). A failed write or move throws `IOException`. With `ownerOnly` the ACL that lets only this Windows account and SYSTEM read the file goes **onto the temp file before the rename**; when the ACL cannot be set (off Windows, FAT, a share that refuses) that is one FINE log line and the file is written anyway — this is not encryption. The path need not be under `dataDir`. Use it for API keys and anything personal |

### 3.8 Menus and settings UI

| Method | What it does |
|---|---|
| `addTrayItem(label, action)` → `PluginUi.MenuHandle` | a tray-menu row (below the app rows, above Quit). `action` runs on the EDT. The returned **handle** relabels, disables, checks or removes the row (`setLabel`, `setEnabled`, `setChecked(Boolean)`, `remove()`; readers `label()`, `enabled()`, `checked()`, `removed()`) — ignore it when the row never changes. Changes show the next time the menu opens (menus are rebuilt on open); a `setLabel` name also reaches tray rows, character-menu rows and settings buttons already on screen, on the EDT shortly after — even when the language event arrives after the window was rebuilt. Any thread. **The label is a plain string and does not follow a language change by itself** — listen for the `settings` event with `phase=language` and call `setLabel(text(...))` again (LUMI Chat and Super Bongo do) |
| `addTrayToggle(label, initial, onChange)` → `MenuHandle` · preview | a **toggle row** — fixed label, an on/off state chip (the app's own 「잠깐 숨기기」 grammar). Pressing flips it and calls `onChange(newValue)` on the EDT; `setChecked` changes the chip without calling `onChange` |
| `addSettingsButton(label, action)` → `MenuHandle` | a button on the settings window's Information tab, near the help buttons. `action` runs on the EDT. Label changes also reach an already open button on the EDT. Registration, enabled-state and removal changes appear on the next open |
| `addFeatureToggle(label, description, state, onChange, dependsOnFeature)` → `PluginUi.FeatureToggle` | an app-styled switch at the **bottom** of Lumi → Features. No registration means no row or spacing. The window fills the check from `state` when it opens and calls `onChange(newValue)` immediately when the check changes; returning false restores it from `state`. When `dependsOnFeature` (`bongo`, etc.) is off, the row is greyed without clearing its check. Harness: `press:feature:<label>` |
| `addCharacterMenuItem(label, forImageSet, action)` → `MenuHandle` | a row in each character's right-click menu. `forImageSet` decides per character (null = all); `action(imageSet, mascotId)`. The handle works like the tray one |
| `addCharacterMenuToggle(label, forImageSet, initial, onChange)` → `MenuHandle` · preview | a toggle row in the character menu. A toggle's state is one per row, not per character. `onChange(newValue)` runs on the EDT |
| `claimClickGreeting()` | tells the app this plugin answers a click itself (the app's click greeting goes quiet) |

### 3.9 The look

Make a plugin window look like the app.

| Method | What it does |
|---|---|
| `theme()` → `PluginTheme` | colours (`tone(darkValue)`, `inkSoft`, `inkFaint`, `line`, `cardBackground`, `danger`, `success`, `dark()`), the CJK font `font(text, size)`, dress that re-applies on theme change (`colour`, `dress`, `style`, `button(kind)`, `toggle`, `slider`, `describe`), pieces (`sectionTitle`, `hairline`, `row`, `card`, `gapRow`, `hintHtml`, `cardGap`). Speak colours as **dark design values** and they are translated to the current theme; when the theme flips, everything you hung re-runs. EDT only. The string given to `style(component, string)` is the look-and-feel's (FlatLaf) — if the app's look changes, that one call may become a no-op, so prefer the meaningful methods (`colour`, `button`, `card`, …) |
| `appIcon()` → Image | the app icon, for an owner-less dialog's title bar |
| `manageWindow(window, key)` | places the window where the user last left it (centred on the active monitor the first time, never off-screen) and remembers it on close. Call after sizing; safe on every open (idempotent) |

### 3.10 Hotkeys

| Method | What it does |
|---|---|
| `bindHotkey(name, "Win+Ctrl+Alt+Shift+key", action)` → `HotkeyResult` | a **system-wide** hotkey — heard while another program is in front. At least one modifier. Blank combination = release. The plugin id is prefixed to the **registration name** only — names never collide, but the actual key combination can still collide with another program or plugin (then `REFUSED`). The answer says **why** when nothing was bound: `OK`, `NONE`, `BAD_COMBINATION`, `REFUSED` (another program holds it), `UNSUPPORTED` — a settings window turns it into words for the user. `action` runs on the EDT |
| `unbindHotkey(name)` | release |
| `normalizeHotkey(s)`, `hotkeyFromKeyEvent(keyCode, modifiersEx)` | the canonical spelling of a combination (`Ctrl+Shift+L`) or null · a key press as a combination string for a hotkey field (null for a bare key or a modifier alone) |

### 3.11 Scheduling and bytecode

| Method | What it does |
|---|---|
| `every(initialMs, periodMs, task)`, `after(ms, task)` → `ScheduledHandle` · preview | **Scheduler**: periodic / one-shot on **this plugin's timer thread** (`lumi-plugin-timer-<id>`, daemon, never the EDT). Each plugin has one event thread and one timer thread — a slow task delays only that plugin's later tasks, never another plugin's. A run that throws is logged, a periodic task keeps going. `cancel()` · `isCancelled()`; whatever is left at exit is cancelled by the app and the thread is taken down. `task == null` throws `IllegalArgumentException` in both, `periodMs <= 0` in `every()` only — released or not (`after(ms, …)` accepts an `ms` of 0 or less) |
| `onEdt(task)`, `offEdt(task)` · preview | on the Swing event thread (windows, menus — never hold it long) · on this plugin's event thread (in line with its listeners, off the EDT) |
| `onLoad(prefix, LumiTransformer)` → boolean | rewrite classes that have not loaded yet (engine, mascots) freely at load time (§6) |
| `patch(className, ClassFileTransformer)` → boolean | retransform one already-loaded class (method bodies only — a JVM rule) (§6) |

<a id="events"></a>

### 3.12 Plugin terrain · preview

| API | Behaviour |
|---|---|
| `registerTerrain(name, List<TerrainRect>)` → `TerrainHandle` · preview | Registers desktop terrain owned by this plugin. Other plugins may use the same name. Reusing a live name within this plugin throws `IllegalArgumentException`. |
| `TerrainRect(id, left, top, right, bottom)` · preview | A rectangle in desktop logical coordinates, matching `MascotState.anchor()` and Swing screen coordinates. Negative positions are allowed. The arguments are edges, not width and height. |
| `TerrainHandle.update(rects)` → boolean · preview | Replaces all pieces in this registration. An empty list clears geometry but reserves the name. Returns `false` after removal. |
| `TerrainHandle.remove()`, `close()`, `isRemoved()` · preview | Removes the registration. Repeated removal is safe; the name may be registered again. An old handle cannot change its replacement. |

```java
TerrainHandle platform = ctx.registerTerrain("platform", List.of(
    new TerrainRect("bar", 100, 400, 500, 408)
));
platform.update(List.of(new TerrainRect("bar", 100, 420, 450, 428)));
platform.update(List.of()); // Clear pieces, retaining the name.
platform.remove();          // Release the name too.
```

Names and piece IDs must contain 1–128 characters and cannot be blank. Piece IDs must be unique within a registration; the piece limit is `TerrainRect.MAX_PIECES` (2048). Rectangles require `right > left`, `bottom > top`, with width and height fitting a positive int. Invalid submissions throw before changing the previous geometry. Submissions are copied; editing the caller's list afterwards does not change terrain.

Calls may originate on any thread. The engine applies the latest complete snapshot before a mascot's next movement tick. Success means accepted data, not a completed landing. Several updates before a tick are coalesced into the latest one. Keeping a piece ID preserves its floor reference; removing a piece invalidates that reference. Moving-platform passenger transport is not provided. Context cleanup removes outstanding terrain after stop or a failed start. Registration after stop returns an already removed handle.

Game, garden and other plugins' terrain remain independent. Candidate order is game → garden → plugins → ordinary windows. Between plugin registrations, earlier registration takes priority at overlaps; within a registration, earlier pieces in the submitted list take priority. Existing screen and floor rules still apply. The current engine uses rectangle tops as floors only when they have at least 140 logical pixels of headroom below the work area's top.

Rectangles behave as window borders. Existing behaviours handle top-edge landing, walking and loss of support at an edge. The API does not provide alpha-mask extraction, solid-body collision, uphill slope assistance, targeted jumps, or contact/digging events. Desktop terrain is suspended in broadcast mode and becomes available again upon return to desktop mode. The plugin owns rendering and coordinate conversion: update or clear terrain when a drawing moves, hides or becomes covered by another window; occlusion is not inferred.

`PluginTestKit` records `registerTerrain(name, rects)`, `updateTerrain(name, rects)` and `removeTerrain(name)`, including duplicate-name rejection and context cleanup. It does not simulate collision, rendering or engine ticks. See the [single platform example](../examples/terrain-basic/README.en.md).

## 4. Events — `ctx.on(topic, map -> …)`

Values are strings unless noted. Listeners run one after another on that plugin's own daemon thread, never on the publishing thread — they cannot block the publisher (EDT, engine locks). A listener that throws is logged and skipped. The "· preview" chip means the same as at the top of §3.

| Topic | When | Keys |
|---|---|---|
| `ready` | the engine is up and the plugins have started | none |
| `behavior` | a mascot's behaviour changed | `imageSet`, `name`, `mascotId` (Integer) |
| `bubble` | a bubble was shown | `imageSet`, `mascotId` (Integer; 0 when addressed by set), `text` (what was actually shown, after the `bubble.text` hook), `ms` (Long; the final lifetime after `bubble.duration`), `kind` (`say` = a plain line · `private` = a line such as an alarm or errand that the broadcast stage leaves out · `choice` = a choice bubble opened; `ms` is then its timeout). **A `kind=choice` payload has no `mascotId`** (`imageSet`, `text`, `ms`, `kind` only). The 「···」 thinking bubble publishes no event |
| `choice` · preview | a choice bubble was answered | `imageSet`, `mascotId` (Integer), `index` (Integer, 0-based · `-1` = opened, no answer (timeout/Escape/character gone) · `-2` = withdrawn by the app (superseded by the same plugin / `clearQueue` / waited too long / line full)), `option` (the label, `""` when negative), `from` (id of the plugin that asked) |
| `click` | a mouse button went down on a character | `imageSet`, `mascotId` (Integer), `button` (`left`/`right`/`middle`), `x`, `y` (Integer, screen coordinates) |
| `mascot` | a character appeared or left | `imageSet`, `mascotId` (Integer), `event` (`added`/`removed`) |
| `headpat` | a head-pat began | `behavior` |
| `affection` | the shown affection tier changed | `tier` (Integer), `previous` (Integer), `points` (Integer) |
| `achievement` | a Steam achievement unlocked | `id` |
| `focus` | focus mode engaged or released | `active` (Boolean) |
| `chat` | AI chat traffic (published by the LUMI Chat plugin — never arrives without it) | `phase` (`ask`/`reply`), `imageSet`, `text`. Another plugin's `ctx.publish("chat", …)` goes out as `<that id>.chat`, so the app topic `chat` cannot be forged |
| `tts` | a voice line started or ended | `phase` (`start`/`end`) |
| `settings` | the settings window was applied, or the language changed | `phase` (`apply`), `changed` (comma-joined keys the app wrote through `LumiSettings` since the previous apply — stock engine keys are not journaled) / `phase` (`language`), `language` (BCP-47) |
| `ask` | `speech\ask.txt` was written | `text` |
| `settings_open` | `speech\settings_open.txt` was written | none |
| `say_file` | a `speech\say.txt` line was shown | `text` |
| `drag` · preview | the user picked a character up or let go (`Dragged` began/ended) | `phase` (`start`/`end`), `imageSet`, `mascotId` (Integer), `x`, `y` (Integer, screen coordinates of the anchor) |
| `throw` · preview | let go with momentum (`Thrown` began) · that flight ended (the next behaviour began) | `phase` (`thrown`/`landed`), `imageSet`, `mascotId` (Integer), `x`, `y` (Integer), `next` (always present — the behaviour after the flight on `landed`, `""` on `thrown`) |
| `hover` · preview | the pointer entered or left a character's window | `phase` (`enter`/`exit`), `imageSet`, `mascotId` (Integer) |
| `doubleclick` · preview | two left presses on the same character within 600 ms and within 90 px on each axis (a 180×180 box, not a radius; the AWT click count cannot be trusted — the press starts a drag that moves the window) | `imageSet`, `mascotId` (Integer), `x`, `y` (Integer). Both presses also produce `click` |
| `alarm` · preview | a reminder was actually delivered | `kind` (`hourly`/`alarm`), `time` (`HH:mm`), `late` (Boolean — an alarm held while the screen was busy) |
| `pomodoro` · preview | the focus/break cycle moved | `phase` (`focus`/`break`/`stopped`), `minutes` (Integer, length of the phase that begins; 0 on stop), `round` (Integer, focus phases started since the cycle was switched on) |
| `bongo` · preview | bongo mode drew a keystroke or click | `kind` (`keyboard`/`left_click`/`right_click`), `frame` (Integer, the hand slot drawn), `look` (`bongo`/`small`). **Never a key code or character** (the core does not read keys) |
| `app` · preview | App exit or restart | `phase` (`exit`/`restart`). Delivered before `stop()` with a short bound. Skip farewell presentation on `restart`; save state and release resources in `stop()` for both phases |
| `island` · preview | the garden (island) process was asked to start or stop, or served a photo request | `phase` (`start`/`stop`/`photo`), `request` (photo only) |
| `game` · preview | the contraption game board opened, started a run, or closed | `phase` (`start`/`run`/`stop`) |

`mascotId` is a number that only means something during this run (same character = same number). `imageSet` is the `img\<folder>` name. There is no per-tick position topic — poll `ctx.mascots()` for positions. Not available: suspend/resume and display-configuration changes (the app does not listen for them).

Payload samples (values in their real `Map<String,Object>` types):

```
click       {imageSet=Lumi, mascotId=1867750575, button=left, x=1420, y=866}
doubleclick {imageSet=Lumi, mascotId=1867750575, x=1422, y=864}
drag        {phase=start, imageSet=Lumi, mascotId=1867750575, x=1420, y=902}
throw       {phase=landed, imageSet=Lumi, mascotId=1867750575, x=1610, y=1040, next=Fall}
behavior    {imageSet=Lumi, name=SitDown, mascotId=1867750575}
bubble      {imageSet=Lumi, mascotId=0, text=안녕하세요, 루미예요!, ms=4000, kind=say}
bubble      {imageSet=Lumi, text=가위바위보!, ms=8000, kind=choice}
choice      {imageSet=Lumi, mascotId=1867750575, index=1, option=바위, from=my.rps}
affection   {tier=3, previous=2, points=1240}
alarm       {kind=alarm, time=14:55, late=true}
pomodoro    {phase=break, minutes=5, round=2}
bongo       {kind=keyboard, frame=3, look=bongo}
settings    {phase=apply, changed=lumi.selftalk.frequency}
settings    {phase=language, language=ja}
app         {phase=exit} / {phase=restart}
```

<a id="hooks"></a>

## 5. Hooks — `ctx.hook(id, (hook, current, ctx) -> …)`

The core asks **before** it decides. A handler receives the value so far (`current`) and returns a new one, or `null` to pass.

Rules:

- **Synchronous** — on the caller's thread, right there. That may be the EDT, a timer or the engine tick: stay cheap, never block. Over 20 ms → one warning per hook and plugin.
- **Order** — Mods-tab order top to bottom, **the lower one wins**. To respect an earlier answer, look at `current` and AND it yourself.
- **Type** — must match the default's type (numbers as `Number`); otherwise pass + one warning. Numeric hooks are clamped by the core (see the table).
- **Exceptions** — all caught. Only that handler is skipped, the chain continues. Nobody answers → the default. A hook with no registered handler builds no ctx Map and returns the default immediately.
- **Empty strings** — mean different things per hook (see "Return / default" in the table): for `selftalk.line` and `speech.greeting`, `""` = skip; for `bubble.text`, a blank answer or null = the original unchanged.
- **User promises** — what the user asked for (alarms, an errand the user pressed, `say.txt`) cannot be cancelled through a hook: such places have no cancel hook, only a hold hook. App-initiated choices (self-talk, greetings) may be skipped with `""`.
- `ctx` is an immutable Map, nulls become `""`. Where a character is involved: `mascotId` (Integer), `imageSet` (String). Mascot objects are never passed.

Catalogue (15):

| Hook | Asked at | ctx | Return / default | Notes |
|---|---|---|---|---|
| `focus.quiet` | the one place chimes, celebrations, self-talk, greetings and onboarding ask "stay quiet?" | `hidden`, `fullscreenAhead`, `focusActive` | boolean, default = screen busy OR focus active | a fixed false = bubbles during screen sharing. Called several times a second → constant time |
| `focus.screen_busy` | alarm hold, pomodoro, focus greeting ask "is the screen busy?" | `hidden`, `fullscreenAhead` | boolean, default = hidden OR fullscreen ahead | a fixed true = held alarms never deliver |
| `text.localized` | the door every bundle string passes (greetings, chimes, alarms, tier names, menu labels) | `key`, `language`, `fallback` | String, default = bundle value. `""` stays `""` | called often — a heavy handler slows the UI. Greetings: `speech.greeting` |
| `sound.effect` | sound-effect path, before playback | `path`, `caller` | String path; `""` = silent. Default = `path` | volume and the per-sound off list apply after the hook. The master mute is checked **before** the hook, so a hook cannot bring the sound back once the user has muted it |
| `behavior.force` | every door where a director or a plugin's `behave` forces a behaviour by name | `name`, `caller`, `mascotId` (-1 if none), `imageSet` | String; `""` = do not force. Default = `name` | the engine's own choices are not asked. Alarm/hourly pushes are user promises — even `""` forces the original name |
| `bubble.duration` | after a bubble's lifetime is computed | `text`, `requested` (0 = auto), `source` (`say`/`private`/`selftalk`), `voiceMs`, `mascotId`, `imageSet` | long ms, default = computed. Clamp 500..120000 | an immediate `say` is overwritten by the next line however long; queued lines follow after it |
| `selftalk.line` | after a self-talk line was picked | `candidate`, `onGardenFloor`, `gardenPhase`, `tier`, `offered`, `mascotId`, `imageSet` | String; `""` = skip this turn | swallowing `offered=true` (unlock notice, friend news) loses that notice |
| `selftalk.interval` | time until the next self-talk | `baseMs`, `frequency` (0-3), `minSec`, `maxSec` | long ms, default = computed. Clamp 3000..3600000 | |
| `speech.greeting` | click / chatter greeting | `pool` (List), `last`, `trigger` (`click`/`chatter`), `imageSet` | String; `""` = no greeting. Default = random from the pool | asked even with an empty pool — the way to give a greeting to a character that has none |
| `affection.award` | the one place every affection source passes | `source`, `points`, `dailyCap`, `cooldownSeconds`, `todayPoints` | int; 0 = award nothing this time (no cooldown either). Default = `points`. Clamp 0..1000000 | negatives → 0 (never subtracts). The daily cap still applies |
| `affection.tier` | points → tier | `points` | int 1..6, default = the table | lower values are ignored (promotions only) |
| `place.allowed` | before a director places a character: "may it stand here?" | `x`, `y`, `confinement` (int[4] or `""`), `mover` | boolean, default = inside the fence | also asked from a 100 ms timer → keep it cheap. A fixed false stops the tether from pulling back |
| `bongo.key_frame` | bongo mode, right after a key frame slot was chosen | `frames`, `cycle` (the core's pick), `look` (`bongo`/`small`) | int slot, default = round-robin. Clamp -1..frames-1 | no key code or character in ctx (the core never reads keys). Every keystroke, on the EDT |
| `bongo.key_input` | every 40 ms while bongo is visible, independent of the mouse inference | `frames`, `look` | a one-shot frame slot; -1 = the plugin owns the input but has none this tick, -2/null = use the core's inference. Default -2, clamp -2..frames-1 | input poller thread: never wait. Consume each verified key once. Key data reaches neither the core nor the public event bus |
| `bubble.text` · preview | the one door right before bubble text goes on screen — `say`·`sayFor`·`sayPrivate`·`say.txt`·greetings·other plugins' `say`, self-talk and the question of a choice bubble | `text`, `source` (`say`/`private`/`selftalk`/`choice`), `mascotId`, `imageSet` | String, default = the original. **`""`, a whitespace-only answer or null = original unchanged (replace only, never swallow)** | user promises (alarms, `say.txt`) pass this door too, so there is no skip — to drop an app-chosen line use `selftalk.line` / `speech.greeting`. The `bubble` event carries the post-hook text |

<a id="bytecode"></a>

## 6. Bytecode — the last resort

Whatever hooks and events can do, do there. Only a transformer breaks when an app update changes the method it patched (outside the promise, §8).

| Path | Registered when | Can do |
|---|---|---|
| `LumiTransformer` service | right after the gate, before the app loads (`attach`) | rewrite **every** class that loads afterwards, wholesale (new methods and fields included) |
| `ctx.onLoad(prefix, t)` | from `start()` | the same freedom for classes not loaded yet (engine and mascots load after `start`) |
| `ctx.patch(className, t)` | any time | retransform one loaded class — method bodies only, a JVM rule |

- The protected list is never offered: `lumi.SteamGate`, `lumi.OfflineToken`, `lumi.Features`, `lumi.plugin.*`, `lumi.steam.*`, `lumi.AffectionStore` and `lumi.Achievements` (the affection store and the achievements — Steam achievement integrity; use `awardAffection` to give points).
- A transformer that throws is logged and the class loads unchanged. Several plugins on one class apply one after another, and because the plugin jars go on the class path in reverse Mods-list order, **the lower mod registers and runs first**. That order is decided by `ServiceLoader` and is not a contract, so never write a transformer that depends on it.
- A disabled plugin's transformers are not registered. `-Dlumi.plugins=off` (launcher `--no-plugins`) disables everything.
- JDK 25's `java.lang.classfile` is enough — `examples\plugin-transformer`.

<a id="data-mods"></a>

## 7. Data fragments (mods without code)

A mod folder may carry **additive fragments** instead of whole files. They are merged in memory; the lower mod wins.

| Place | Does |
|---|---|
| `img\<Set>\conf\actions.d\*.xml`, `behaviors.d\*.xml` | append actions/behaviours; the same `Name` replaces in place. The global `conf\actions.d\`/`behaviors.d\` apply only to sets whose base conf comes from the global `conf\` |
| `conf\language.d\<name>_<locale>.properties` | override keys of that locale; a file without suffix fills missing keys only |
| `conf\self_talk.d\`, `conf\meet_dialogues.d\<name>_<lang>.txt` | append self-talk / meeting lines after the base |
| `mod.json` `{"name","version","app_version"}` | Mods-tab chips for a mod without a plugin |

A broken fragment is skipped with one warning; if the merged set fails engine validation the base loads without fragments. Details: the [walkthrough](../plugin-guide.en.html), section "Data fragments".

<a id="compatibility"></a>

## 8. Compatibility policy

- The public surface of the plugin API is split into **stable** and **preview**. Unmarked items are stable; preview items carry a "preview" chip in the tables and are listed under `preview` in `sdk.json`.
- **Stable items only ever gain entries within one `api_version`** — nothing is removed, no type changes. A hook whose decision point leaves the app stays as an empty slot returning the default, marked deprecated in the table. Events may gain keys, never lose them — ignore keys you do not know.
- **Preview items may change or disappear without an `api_version` bump.** Each such change is listed in `CHANGELOG.md` and a removed item stays as an empty slot for one app release. A preview item that survives one app release unchanged is promoted to stable. The 1.5.0 split: doors the first-party plugins (LUMI Chat, Super Bongo) or the app itself use, and their counterparts, are stable; the rest is preview.
- **`api_version` rises only when a stable item must be removed or reshaped.** The migration list goes to `CHANGELOG.md`, and **plugins written for the previous version (N-1) keep loading for one more version** (removed hooks as empty slots, removed methods as forwarding shims). Plugins two versions behind are not loaded and get an update notice; neither is a plugin declaring a newer `api_version` than the app.
- "Additive only" also means a method, hook or topic added in a later app is absent from older apps with the same `api_version`. Declare the app version you tested in `app_version` in `plugin.json`; a running app that differs shows an informational chip in the Mods tab (it blocks nothing).
- Outside the promise: the shape of core classes a transformer touches, public classes outside `PluginContext` and this document's type table (callable from the class path, free to change per release), the app's own keys in `conf\settings.properties`.
- (Current build) The 1.5.0 loader reads `api_version` but does not act on it; the refusals and chips above are enforced by the loader from the next app version.
- The SDK version equals the app version (`sdk_version` = `app_version` in `sdk.json`); `api_version` is the separate compatibility number in this document's title. See the release notes for documentation and example revisions.

<a id="practices"></a>

## 9. Do not

- Wait for the EDT inside an event listener (the publisher may be the EDT — use `SwingUtilities.invokeLater` or `onEdt`).
- Touch files, the network or locks inside a hook handler.
- Keep user data outside `plugindata\<id>\`. Say so in the Workshop description if you handle personal data.
- Change the app's own keys in `conf\settings.properties` (only your prefixed keys).

<a id="testing"></a>

## 10. Test kit — `PluginTestKit`

SDK 1.5.0 includes a fake for `beginMicCapture()`. The kit records `beginMicCapture`/`endMicCapture` with the owner id; `speak` during a hold records `speakSuppressed` without playback callbacks. `kit.close()` releases all nested holds. No audio device is opened.

This compatible addition keeps `api_version` at 1. Plugins supporting older apps must check that the method exists and disable microphone input when it does not. `app_version` is informational, not an enforced minimum host version. Compiling against SDK 1.5.0 does not add the new method to an older host.

`lib\Shimeji-ee.jar` cannot start the app (the gate), but a plugin's **unit tests** need no app. The `PluginContext` a `PluginTestKit` hands out leads to the kit behind every door — what the plugin does is **recorded**, what it reads comes from **fakes**, and events, hooks and the clock are driven by the test. No engine, no window, no Steam. `tools\run_tests.py <plugin folder>` builds and runs `test\*.java` (see `examples\hello\test\HelloPluginTest.java` — a plain `main`, no framework).

```java
import com.group_finity.mascot.lumi.plugin.PluginTestKit;
import hello.HelloPlugin;
import java.nio.file.Path;

public final class HelloSmokeTest {
    public static void main(String[] args) throws Exception {
        Path manifest = Path.of(System.getProperty("plugin.dir"), "plugin.json");
        HelloPlugin plugin = new HelloPlugin();
        try (PluginTestKit kit = PluginTestKit.start(manifest)) {
            try {
                plugin.start(kit.context());
                kit.publish("ready");
                if (kit.calls("say").size() != 1) {
                    throw new AssertionError("Expected one startup greeting");
                }
                Object line = kit.ask("speech.greeting", "hi");
                if (!"[hello] hi".equals(line)) {
                    throw new AssertionError("Greeting hook failed: " + line);
                }
                kit.tick(60_000);
            } finally {
                plugin.stop();
            }
        }
    }
}
```

Save it as `test\HelloSmokeTest.java` in a copy of the unchanged hello example and run `python tools\run_tests.py <folder>`. The runner picks up only the `*.java` files directly under `test\` and starts one JVM per class (with `-Dplugin.dir=<the plugin folder>`); **it does not pass `-ea`, so `assert` statements are inert** — fail by throwing `AssertionError` yourself, as above. The kit does not instantiate your plugin, publish `ready` or call its `stop()` — the test does all three.

| Method | What it does |
|---|---|
| `start(pluginId)` · `start(Path pluginJson)` · `start(PluginDescriptor)` | One kit = one plugin. With a `plugin.json` the `"settings"` defaults are seeded into `prefs()` |
| `context()`, `descriptor()`, `dataDir()` | The context to hand over · the manifest · a temp folder (`prefs()` lives there; `close()` deletes it) |
| `calls()`, `calls(name)`, `lastCall(name)`, `clearCalls()` | The record: `say`·`sayQueued`·`ask`·`behave`·`behaveFor`·`summon`·`dismiss`·`moveTo`·`heart`·`playSound`·`awardAffection`·`setCharactersVisible`·`hideBubble`·`publish`·`addTrayItem`·`addTrayToggle`·`addSettingsButton`·`addCharacterMenuItem`·`addCharacterMenuToggle`·`addFeatureToggle`·`bindHotkey`·`every`·`after`… — `Call.name` + `args`. `lastCall` takes a name (there is no no-argument form) |
| `queued()`, `menuRows()`, `featureToggles()`, `hotkeys()`, `choiceOpen()`, `choiceOptions()` | Queued lines · menu rows added (live handles) · feature switches added · registered hotkeys · the open choice bubble |
| `mascots(…)`, `affection(tier, %, untilNext, days)`, `affectionRemaining(n)`, `focus(active, sources)`, `screenBusy`, `bubbleVisible`, `charactersHidden`, `language(code)`, `behaviorNames(Set<String>)`, `currentBehavior(name)`, `installedImageSets`, `activeImageSets`, `screens`, `setting(key, value)`, `appText(key, value)`, `soundsEnabled` | Fakes for what the plugin **reads**. Defaults: no mascots, tier 1, 2 points left today, `ko`, one `Lumi`, 1920×1080, sounds on |
| `feature(id, on)` | what `featureEnabled(id)` answers (all off by default) |
| `publish(topic, k, v, …)` | Delivers to the listeners **synchronously** (a throwing listener is skipped — the app's rule) |
| `ask(hook, default, k, v, …)` | Runs the hook handlers by the app's rules (null passes, wrong type passes, a throw is skipped) and returns the answer |
| `answerChoice(index)`, `choicesWaiting()` | Closes the open choice bubble with that index (or -1), calls the callback and opens the next one in line (the app's queueing: a plugin's newer `ask` supersedes its own with `-2`; `tick()` drops asks past their `maxWaitMs` with `-2`) · choice bubbles waiting behind the open one |
| `tick(ms)`, `now()` | The fake clock — runs every due `every`/`after` in order (a periodic task as often as it fits) |
| `textLoader(ClassLoader)` | Where `text()` finds `plugin_<lang>.properties` (default: the test's loader) |
| `quiet(b)`, `foregroundScreen(rect)`, `persona(set, text)`, `speaking(b)` | fakes — the answers of `quietNow`, `foregroundScreen`, `persona`, `isSpeaking` |
| `speechVolume()`, `mouthFlapping()` | the last `setSpeechVolume` · the set whose mouth is moving (null if none). `speak` completes at once in the kit (`onStart`/`onEnd` run inline) |
| `close()` | Releases what was registered through the context and deletes the temp folder (safe twice; `try-with-resources`). It does not call the plugin's `stop()` — and, as in the app, a `prefs().onChange` listener stays |
| `PluginTestKit.realContext(pluginId)` | a context on the **product backend** — drives a plugin's classes against the app's real state (focus mode, the bubble, the affection store) with no app window; for integration checks. Nothing is recorded or released. Unit tests want `start()` |
| (good to know) | the kit does not know your jar, so it looks for `plugin_ko.properties` in a **class folder** (the plugin under test) first and only then in the first jar (the product picks its own by jar name). If the plugin under test is itself a jar, point `textLoader(...)` at it |

Inside the kit, listeners, hooks, scheduled tasks and `onEdt`/`offEdt` run **inline on the calling thread** — tests are deterministic. The exception is `prefs()`: it is a real store (in the temp folder), so `onChange` notifications arrive **asynchronously**, as in the app. `patch`/`onLoad` are not simulated (no instrumentation → `false`).

<a id="catalogue"></a>

## 11. Catalogue — `CATALOG.en.md`

Behaviour names (Korean name, frequency, hidden), event topics and keys, each hook's call site, default and ctx keys, the `MascotState` fields, the app setting keys a plugin may read, the `type` values of `settings`, the feature switch ids — all generated from the app's sources when the SDK is built. Never hand-edited; the SDK does not build from a copy that disagrees with the sources.

<a id="alternatives"></a>

## 12. Doing what the promise does not cover

For anything outside §3–§5, **do not call app classes** — do it as below. App classes are on the class path and callable, but they change with every version (§8).

| You want | Do this |
|---|---|
| read/write JSON | `Json` (§3 table). For more, ship your own library under `plugins\lib\` |
| a window that looks like the app | `ctx.theme()` only — FlatLaf style strings go through `theme().style(component, "background:#131F33; …")` in dark values. The rest of the look (the app's `settings` package) is outside the promise |
| remember a window's position | `ctx.manageWindow(window, "settings")` |
| a secret on disk | `ctx.writeAtomic(path, bytes, true)` — `prefs()` is for ordinary settings (plain text); keys go `ownerOnly`. The ACL is best effort, not encryption (§3.7) |
| detect a double click | `ctx.on("doubleclick", …)` — do not install a global AWT listener. The app forwards presses that land on its own pass-through windows (heart, note) as `click` too |
| a window next to a character | check `ctx.mascotById(id)` for null, then below its `anchor()` (feet, desktop coordinates); monitors from `screens()` |
| the sets of the characters on screen | `ctx.mascots()`, `imageSet()` in order without duplicates — not the same as `activeImageSets` (enabled but maybe not yet spawned) |
| make a sound | effects: `playSound(path)`; voice (TTS, pre-baked files): decode to PCM and `speak(PcmAudio, onStart, onEnd)` — the bundled JavaSound decoders support WAV, OGG (Vorbis), MP3, FLAC, AIFF and AU; pass decoded PCM to `speak` |
| move the mouth | `mouthFlap(set, ctx::isSpeaking)` — do not loop `behave("Talk")` by hand |
| move the cursor, touch other programs' windows | the app does not do this, so there is no door. Use JNA directly (outside the promise; do not put a second jna in `plugins\lib\` — use the app's) |
| edit a persona | `writeAtomic` to `personaFile(set)` — the file convention is the app's |

<a id="recipes"></a>

## 13. Recipes

### Minimal entry class

Save it as `src\myplugin\MyPlugin.java` and put `myplugin.MyPlugin` in `src\META-INF\services\com.group_finity.mascot.lumi.plugin.LumiPlugin`. Use the manifest from §1 as it is; a minimal build may drop the `settings` array. Build with the [README commands](../README.en.md). If you replace the hello example's entry class with this one, its test must change to match.

```java
package myplugin;

import com.group_finity.mascot.lumi.plugin.LumiPlugin;
import com.group_finity.mascot.lumi.plugin.PluginContext;

public final class MyPlugin implements LumiPlugin {
    @Override
    public void start(PluginContext ctx) {
        ctx.on("ready", event -> {
            if (!ctx.quietNow()) {
                ctx.say(ctx.text("greeting", "Hello from my plugin!"), 4000);
            }
        });
    }
}
```

`ready` waits for the engine; an unprompted greeting asks `quietNow()` whether this is a time to keep quiet. Translations go in `src\plugin_ko.properties` and `src\plugin_en.properties` as `greeting=…` — they land at the jar root.

The short snippets below go inside `start(PluginContext ctx)`.

### Reply to the clicked character

```java
ctx.on("click", event -> {
    if (!"left".equals(event.get("button"))) return;
    if (!(event.get("mascotId") instanceof Integer id)) return;
    if (!(event.get("imageSet") instanceof String set)) return;
    if (ctx.mascotById(id) == null) return;
    ctx.sayTo(id, set, ctx.text("clicked", "Hello!"), 3000);
});
```

A double click also produces two `click` events — subscribe to `doubleclick` if that is the interaction you want. The app's own click greeting may show as well; use `claimClickGreeting()` only when you mean to take the click greeting over entirely. If the character disappears between the check and the display, `sayTo` falls back to the set's first character.

### Ask a question

```java
ctx.ask("Take a short break?", java.util.List.of("Yes", "Later"),
        10_000, 30_000, index -> {
            if (index < 0) return;
            if (index == 0) ctx.sayQueued("Enjoy your break!", 3000);
        });
```

`10_000` is the time to answer **after it opens**; `30_000` is how long it may wait in line. `-1` means it opened and got no answer, `-2` means the app withdrew it (your own newer `ask` replaces even an open question) — neither is an option index. The normal callback runs off the EDT, so go through `onEdt` to touch Swing. In a distributed plugin, put the question and the options through `text()` too.

### Modify an existing greeting

```java
ctx.hook("speech.greeting", (hook, current, args) -> {
    if (!"Lumi".equals(args.get("imageSet"))) return null;
    if (!(current instanceof String line) || line.isBlank()) return null;
    return "[my plugin] " + line;
});
```

Declare `"hooks": ["speech.greeting"]` in the manifest too. `null` keeps the previous handlers' answer. Never call `ctx.say()` inside a `bubble.text` handler or `ctx.quietNow()` inside a `focus.quiet` handler: both re-enter the same hook.

### Register a global hotkey

```java
com.group_finity.mascot.lumi.plugin.HotkeyResult result =
        ctx.bindHotkey("greet", "Ctrl+Alt+L", () -> ctx.say("Hello!", 3000));
if (result != com.group_finity.mascot.lumi.plugin.HotkeyResult.OK) {
    ctx.log().warning("Hotkey registration: " + result);
}
```

Registration names are scoped to your plugin, but the actual key combination is shared with other programs and plugins. A declarative `hotkey` setting only stores text: bind its initial value and rebind when the preference changes. Release it by hand with `unbindHotkey("greet")`.

### Cache a setting for a frequently called hook

Save this separate entry class as `src\myplugin\QuietPlugin.java` and, when you use it, register `myplugin.QuietPlugin` in the service file. Add `"hooks": ["focus.quiet"]` to the manifest and `{"key":"quiet","type":"bool","default":false,"label":"Keep quiet"}` to its `settings` array.

```java
package myplugin;

import com.group_finity.mascot.lumi.plugin.LumiPlugin;
import com.group_finity.mascot.lumi.plugin.PluginContext;
import com.group_finity.mascot.lumi.plugin.PluginPrefs;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public final class QuietPlugin implements LumiPlugin {
    private final AtomicBoolean quiet = new AtomicBoolean();
    private PluginPrefs prefs;
    private BiConsumer<String, String> changed;

    @Override
    public void start(PluginContext ctx) {
        prefs = ctx.prefs();
        changed = (key, value) -> {
            if ("quiet".equals(key)) quiet.set(Boolean.parseBoolean(value));
        };
        prefs.onChange(changed);
        quiet.set(prefs.getBoolean("quiet", false));
        ctx.hook("focus.quiet", (hook, current, args) ->
                quiet.get() ? Boolean.TRUE : null);
    }

    @Override
    public void stop() {
        if (prefs != null && changed != null) prefs.removeOnChange(changed);
    }
}
```

The hook reads an atomic cached value only — no file, no preference save, no store lock. Preference notifications are asynchronous, so the old value may be used for a moment right after an edit. The listener is released in `stop()` by the plugin itself, on a normal shutdown and after a failed start alike (§2).

### Choose the right thread

| Work | Where |
|---|---|
| Swing windows and widgets | `ctx.onEdt(...)` — keep callbacks brief |
| Event handling and short background work | `ctx.on(...)` / `ctx.offEdt(...)` — both share this plugin's event thread |
| Periodic or delayed work | `ctx.every(initialMs, periodMs, ...)` / `ctx.after(ms, ...)` — this plugin's timer thread |
| Hook decisions | the caller's thread — answer at once from a cached value |
| Longer network work | a worker thread of your own with timeouts and shutdown handling — never block hooks, the EDT or the event queue |

<a id="character-authoring"></a>

## Visual rules — `VisualLogic` preview · `CharacterDefinition` preview

The pure shared engine gives editor simulation and host execution the same semantics. `VisualLogic.read(Reader)` reads a UTF-8 Properties character stream into immutable, validated `Document(String id,String imageSet,Map<String,Double> variables,Map<String,Dialogue> dialogues,List<Rule> rules)`. Public records are `Dialogue(String id,String language,String text,String audio)`, `Rule(String id,String event,String variable,double increment,String comparison,double threshold,String dialogue,String behavior)` and `Effect(String ruleId,String dialogueId,String text,String audio,String behavior)`.

Properties use `schema=1`, `id`, `imageSet`, `variable.<id>=<number>`, `dialogue.<id>.text/language/audio`, `rule.count` and zero-based `rule.N.id/event/variable/increment/comparison/threshold/dialogue/behavior`. Required dialogue text may be empty. Optional language/audio/variable/dialogue/behavior default empty; increment/threshold default zero and comparison defaults `always`.

`new VisualLogic.Session(document)` supplies `dispatch(String event): List<Effect>`, `variables(): Map<String,Double>`, `restore(Map<String,Double>)` and `close()`. Events are `ready`, `click`, `menu:<id>`. Matching rules run in document order: increment the variable, then compare with `always/eq/ne/lt/le/gt/ge`, then return effects. An omitted variable requires zero increment and always comparison. Unknown events return no effects. Restore ignores unknown variables; invalid known values reject the entire restore. Null is unsupported. Numeric overflow aborts the whole dispatch without state changes or effects. Closing prevents further execution and restoration.

Identifiers are 1–64 alphanumeric/dot/underscore/hyphen characters starting alphanumeric. Limits: one million input characters, 128 variables, 256 dialogues, 128 rules, 4096 dialogue characters, finite numbers within ±1e12. Language is metadata of at most 32 characters; explicitly selected dialogue is not translated. Audio must be a package-relative WAV under `sound/`, without parent traversal. No scripts, recursion, loops, filesystem or network actions exist.

The host discovers **`plugins/runtime/visual-logic.properties`** without a plugin JAR. It is executable content: approval is required, changes require renewed approval, and protected execution begins on the next launch. Unapproved packages contribute no PNG/WAV data either. The host filters clicks to imageSet and accepts clicks at least 100ms apart. Each menu event gets a tray entry. Dialogue bubbles last five seconds, WAV playback follows effect-sound settings, and behavior names must exist for the character. Numeric state is isolated by installed mod source identity, saved at normal shutdown and restored next launch. Crashes retain the preceding normal-shutdown state. Choosing another mod's document ID does not share its state.

`CharacterDefinition.generate(String imageSet,List<Frame>)` returns `Map<String,String>` containing package-relative `img/<imageSet>/conf/actions.xml`, `behaviors.xml`, `info.xml`. `Frame(String image,int durationMs,int anchorX,int anchorY)` points to an image-set-relative PNG supplied by the caller. Limits are 1–4096 frames, 16–60000ms, anchors ±8192; built-in character names are reserved. Durations round to 60Hz engine ticks. It generates ModMakerIdle/Stand/Walk/Fall/Dragged/Thrown/ChaseMouse behaviors from the creator's frames. It does not install or overwrite files. Steam publishing is outside this API.

`CharacterDefinition.behaviorNames()` returns the immutable `Set<String>` of generated behavior names; editors validate against this set plus declared custom behaviors. Unknown/duplicate Properties fields and incomplete dialogue records are rejected.


<a id="authoring-preview"></a>

## Live desktop authoring — `AuthoringPreview` preview

`PluginContext.openAuthoringPreview(AuthoringPreview.Snapshot)` · preview creates one dedicated test character in the running desktop app and returns an `AuthoringPreview.Session`. Each plugin context can own one open session. Call it after the user starts an authoring test. It does not install a mod, approve code, change the normal summon pool, or persist variables. `PluginTestKit` throws `UnsupportedOperationException`; a released context or an already open session throws `IllegalStateException`.

- `Frame(byte[] png,int durationMs,int anchorX,int anchorY)` accepts PNG bytes, 16–60000ms, and anchors within ±8192. Both incoming and returned byte arrays are copied.
- `Snapshot(List<Frame>,VisualLogic.Document,Map<String,byte[]> audio)` supplies the complete rules and assets. Audio keys are relative `sound/...wav` identifiers referenced by dialogue rules, not requests to read filesystem paths. XML, scripts, JARs and filesystem paths are not accepted. Every audio reference must be present and behaviors must belong to `CharacterDefinition.behaviorNames()`.
- A snapshot allows 1–16384 PNG frames, up to 256 WAVs, 4MiB per file and 24MiB total. Each PNG dimension is at most 2048 and the combined image budget is 16 million pixels. Audio must be PCM WAV, 1–2 channels, at most 96kHz and 30 seconds. Invalid bytes or formats are rejected with an exception.
- `Session.replace(Snapshot)` validates a complete revision before replacing the test actor. It preserves position, resets variables to their initial values, and dispatches `ready`. Rejected revisions preserve the existing test state.
- `Session.dispatch(String)` runs `ready`, `click`, or a `menu:<identifier>` declared by the current document. Clicking the actual test character also dispatches its rules. Closed sessions and undeclared events are rejected.
- `Session.state()` returns an immutable `State(boolean closed,int mascotId,long revision,Map<String,Double> variables,List<String> trace)`. It exposes variables and up to 80 recent trace lines of at most 512 characters each. It can be read from any thread.
- Open, replace and dispatch must run off the Swing EDT. `close()` is idempotent: when called on the EDT it schedules cleanup asynchronously; otherwise it waits for cleanup. Close it when the editor closes; releasing the plugin context also releases its session. The session owns its actor, bubble, audio and temporary files.

Test state is not written to the saved project or exported mod. Persistent installation, approval and restart testing remain separate export operations.

### Per-behavior frames (preview)
`CharacterDefinition.Behavior(String name,String kind,List<Frame> frames)` and `generateBehaviors(String imageSet,List<Behavior>)` provide separate images, durations and anchors per behavior. Names match `[A-Za-z][A-Za-z0-9_-]{0,63}`; kinds come from `behaviorNames()`. Use kind `ModMakerIdle` for custom poses. Built-in names require their matching kind; duplicate names are rejected. `ModMakerIdle` is required; omitted engine behaviors fall back to its frames. Limits: 512 behaviors, 4096 frames per behavior and 16384 supplied frames in total. Existing `generate` is unchanged. `behaviorNames()` lists only built-in names; editors also validate against their declared custom names.

`AuthoringPreview.Behavior(String name,String kind,List<Frame> frames)` and `Snapshot(frames,document,audio,behaviors)` carry these sequences into the real engine. Empty behaviors preserve common-frame mode; otherwise behaviors are authoritative (pass idle frames as the legacy frames argument). All behavior sequences share the 16384-frame, 24 MiB and 16-million decoded pixel budgets. The three-argument constructor remains available. Rules may reference declared custom names; rejected replacements preserve the prior session. Approval, ownership and file access contracts are unchanged.

`generateBehaviors(String imageSet,List<Behavior>,String previewImage)` accepts an optional image-set-relative PNG thumbnail path. `null` preserves the first idle frame. Absolute paths, parent traversal and XML characters are rejected. The caller exports the PNG. It affects only `info.xml` `PreviewImage`, consumed by the host character chooser, never animation frames. This is not a mod-list or Steam thumbnail setting.

### Installed LUMI behavior templates

`CharacterTemplates.behaviors()` reads the installed Lumi graph as immutable `Template(id,displayName,frames)` records. `TemplateFrame(slot,image,durationMs,anchorX,anchorY)` carries an opaque pose slot and a host image reference such as `lumi:shime1.png`. `CharacterTemplates.image(reference)` returns original PNG bytes (up to 4 MiB); invalid references throw IllegalArgumentException and missing installed content throws IllegalStateException. Do not add this artwork to the user resource pool or redistribute it.

The appended `CharacterDefinition.Frame.templatePose` and `Behavior.templateId` default to empty through existing constructors. Template generation preserves original conditions, sequences, movement, hotspots and transitions while replacing poses. A new frame with an empty pose slot belongs to the preceding frame's animation (the first animation when leading). Reordering is applied within each original animation, never across conditional animation groups. Unchanged Duration expressions survive verbatim. Generated `@lumi/` image paths resolve to installed Lumi artwork without packaged copies. Budgets are 512 behaviors and 16384 frames.

`AuthoringPreview.Frame(png,durationMs,anchorX,anchorY,hostImage,templatePose)` and `Behavior(name,kind,frames,templateId)` follow the same contract. A nonempty hostImage replaces supplied PNG bytes. Previous constructors remain compatible. No installation, approval or publishing is performed.

`CharacterTemplates.reservedIds()` returns source action and behavior IDs. Generation rejects collisions except an own template ID or a matching legacy basic role. Reserve this set when editing executable IDs, independently of display labels.

Multiple runtime aliases may share a `templateId` only when their kind and complete frame definitions are identical. Conflicting overrides are rejected. For example, authored `Idle` and engine-role `ModMakerIdle` can share the same `StandUp` template. Live preview deduplicates identical PNG content and preserves the full template graph.

### Required behaviors and behavior sound effects

`CharacterTemplates.requiredBehaviorIds()` returns the engine-required IDs `ChaseMouse`, `Fall`, `Dragged`, and `Thrown`. `Template.required()` exposes the same classification. The maker's default-idle role is separate. Previous constructors remain supported.

The appended sound argument of `CharacterDefinition.Behavior(name,kind,frames,templateId,sound)` and the matching `AuthoringPreview.Behavior` constructor is a package-relative `sound/<modid>/<file>.wav` path. Null or empty inherits template audio. An override takes precedence over pose audio and plays once on the first playback tick after entering a behavior, regardless of animation loops. Starting a new behavior instance permits playback again. Existing sound enable, individual mute and global volume settings still apply.

Generated files may include `img/<imageSet>/conf/behavior-sounds.properties`; export it together with the other returned files. Original XML, motion, conditions and pose Sound attributes remain intact, so removing an override restores original audio. Aliases must have identical sounds as well as frames. Live preview receives WAV bytes through the existing `Snapshot.audio` map using the same key and rejects missing references. An editor can omit an override for deleted optional audio to inherit the template. A missing packaged WAV fails sound playback only, without stopping character movement.

`Template.hasDefaultSound()` reports whether any reachable pose in the installed behavior graph declares a nonblank `Sound`, including conditional animations. False means the UI may show no default sound. It does not assert file availability, decoding success or current mute settings. Existing manual constructors default this unknown metadata to false; `behaviors()` supplies the authoritative installed metadata.

### Final export without guide assets

`generateStandaloneBehaviors(imageSet, behaviors, previewImage)` generates final packages. It retains authored action/condition graphs and the seven execution roles from `behaviorNames()`. Omitted roles use the author's idle frames; fallback Stand/Walk remain non-autonomous. Missing next-behavior targets are redirected to default idle or an authored template alias, and hotspots without an authored target are removed.

Final export rejects `lumi:` frame/thumbnail references and inherited LUMI audio in active graphs. Supply a user WAV or explicitly mute default audio. Existing `generateBehaviors` remains available for editing and guide previews.

The sixth parameter of `CharacterDefinition.Behavior` and `AuthoringPreview.Behavior` is `boolean muteDefaultSound`; previous constructors default to false. Empty sound plus true explicitly silences inherited audio. An empty sound-map value means mute, whereas an absent key inherits. Standalone output removes native sound references while preserving motion, conditions and frame settings. Use this flag to export deleted optional user audio as silence.

### Shared primitive actions — new 25 Hz APIs

`CharacterTemplates.actions()` returns `ActionTemplate(id,displayName,frames,hasDefaultSound)` entries for named actions with directly owned animations, never flattened Sequence graphs. Walk, Run, Stand and Sit each have one shared frame definition. Slots are `Action/animationIndex/poseIndex`; durations use the real 25 Hz engine clock (40 ms/tick). Use `CharacterDefinition.actionTickDurationMs(ms)` for matching preview quantization. Legacy behavior APIs retain their existing 60-based conversion for compatibility.

`CharacterDefinition.Action(name,templateId,frames,sound,muteDefaultSound)` and `generateActions(imageSet,actions,previewImage)` edit shared actions. The required authored action is `Idle`, using templateId `Stand` when based on LUMI. Empty templateId creates an original stationary action. Pure-custom projects generate without installed Lumi XML. `generateStandaloneActions` rejects guide assets in authored actions and replaces unauthored native/inline poses with the author's idle image and silence while retaining native scenario motion and conditions. Missing seven execution roles also receive own-idle fallbacks.

Native action IDs remain shared: editing Walk updates references from WalkLeftAlongFloorAndSit, WalkAlongWorkAreaFloor and other scenarios. Callable behaviors are supplied for authored IDs. New invocation wrappers provide default targets and a maximum 100-tick duration, then return to Idle; existing same-name native behaviors retain their original context. Native ActionReference parameters and floor/wall/ceiling requirements are preserved.

Effects use `conf/action-sounds.properties`, keyed by physical action ID. Each action entry plays once, including repeated entries inside one looping behavior; an already-playing clip restarts on a new entry. Empty mappings explicitly mute, absent mappings inherit pose audio. `AuthoringPreview.Action` has matching fields; use `Snapshot.forActions(actions,document,audio)`. Frame fields/audio keys are unchanged. Legacy behaviors and new actions cannot coexist in one snapshot.

A new frame with an empty slot joins the preceding source animation (or the first animation when leading), retaining movement attributes such as Velocity from its original pose prototype. Reordering across conditional animations is rejected.

### Frame audio — shared action APIs

`TemplateFrame` appends `sound` and `volumeDb` to its existing five fields. Native refs use `lumi:<relative>.wav`, or empty for silence. `CharacterTemplates.sound(reference)` reads at most 4 MiB from installed `img/Lumi/sound`; invalid/traversing references throw IllegalArgumentException and unavailable files throw IllegalStateException. Do not copy guide sounds into user resources.

`CharacterDefinition.Frame` appends `sound`, `muteDefaultSound`, `loopSound` after its five existing fields; `AuthoringPreview.Frame` appends them after six. Previous constructors default to empty/false/false. User sound is a package-relative `sound/...wav`; empty inherits native audio, mute removes it. Default once plays on entry and on every animation-cycle re-entry. Loop repeats while the frame is active and stops on frame/animation/behavior exit or actor disposal. Once tails may finish across later frames, bounded to 16 voices per actor. Clips belong to individual actors.

`frame-sounds.properties` maps `<cue>.path` and `<cue>.loop` to XML `Sound="@frame/<cue>.wav"`. Export this file too. Native Volume is preserved; user replacements use 0 dB. Explicit legacy action-level audio/mute takes precedence; new makers use frame settings.

Adjacent positive-integer-tick poses in one direct Animation are combined only when image, ImageRight, anchor, motion and other attributes match and no following pose declares a new Sound. Range slots use `Action/animationIndex/start-end`. Walk becomes four 240 ms frames and Run four 120 ms frames without shifting sound onsets. New XML emits one pose per logical frame and uses entry-based audio instead of the old one-tick-request/silent-hold split. Old single slots remain readable. Guide PNG/WAV references remain forbidden in final exports.

The optional trailing `Double soundVolumeDb` on both frame records preserves gain when embedding a native WAV as a user resource. Only finite -80..24 dB values are accepted. `null` keeps the previous policy (inherited native gain; 0 dB for user WAV). Existing constructors remain compatible.

## Shared motion authoring
`CharacterTemplates.motions()` groups execution actions reachable from actual Behavior entry points using SDK-owned semantic bindings. `MotionTemplate(id,displayName,frames,bindings,hasDefaultSound)` uses its first binding as the canonical animation. Unused actions are excluded; other reachable actions remain singleton motions. Jump uses Jumping, FaceGlide uses FaceGlideMid, and Land uses SoftLandSilent.
Pass `CharacterDefinition.Motion(name,templateId,frames)` to `generateMotions(imageSet,motions,previewImage)` or `generateStandaloneMotions(...)`. templateId is a motion ID (empty for custom); name=Idle is required. Frame slots retain canonical native action slots. Artwork, durations, anchors, frame sound and gain are shared across bindings. Each target retains its native class, targets, gravity, reference duration, conditions and physical pose attributes. Canonical sound replaces original per-role sound differences. Added frames use the first physical pose of their animation group; reordering across conditional animations is rejected.
`AuthoringPreview.Motion(name,templateId,frames)` and `Snapshot.forMotions(motions,document,audio)` provide the same live execution; `Snapshot.motions()` exposes the input. Mixing motions with actions/behaviors is rejected. A callable Behavior uses each authored motion name for event binding. Existing Action/Behavior APIs and constructors remain compatible. Final standalone output rejects guide images and inherited guide audio; preview permits them.

Walk binds Walk/ChatApproach/LeadParade/JoinParade/GatherJoin/HiFiveApproach/GiftApproach; Dash binds Dash/CreditDash/TagDashLeg/TagChase. Canonical Walk/Dash sound is shared intentionally, adding footsteps to previously silent execution roles.

### Sound playback scope
`CharacterTemplates.SoundPlayback` provides `FRAME_ENTRY` (each frame/animation-cycle entry) and `BEHAVIOR_ENTRY` (one occurrence of a logical sound during a behavior episode). The trailing `TemplateFrame.soundPlayback` uses the native SoundGate policy: repeatable sounds such as footsteps use FRAME_ENTRY; non-repeatable sounds such as pinch use BEHAVIOR_ENTRY. In a native slot `action/animationIndex/poseIndex`, different animation indices may be conditional alternatives rather than sequential frames. Pinched contains 29 such alternatives.
`CharacterDefinition.Frame` and `AuthoringPreview.Frame` have an optional trailing `CharacterTemplates.SoundPlayback soundPlayback`. null inherits canonical source policy, or FRAME_ENTRY for custom frames; explicit values override it. loopSound=true takes precedence. When embedding native WAV bytes as a resource, persist and forward this policy together with gain.
The frame sidecar stores `.scope`; absent scope retains the legacy FRAME_ENTRY policy. BEHAVIOR_ENTRY is gated per actor, actual behavior episode and logical `.path`, so branch changes and single-frame cycles do not retrigger it, while releasing and grabbing again does. Sessions and actors do not share gates. The stock character SoundGate is unchanged.

## Minimal character runtime profile
`CharacterTemplates.limitedMotions()` supplies Stand (Idle), Walk, Run, Jump, Falling, Land, Pinched, Sit and Sprawl only. Pinched supplies one neutral `Pinched/14/0` pose instead of 29 conditional alternatives. Use `generateLimitedMotions`/`generateStandaloneLimitedMotions(imageSet,motions,previewImage)` and `AuthoringPreview.Snapshot.forLimitedMotions(motions,document,audio)`; inspect `Snapshot.limitedRuntime()`. Existing generation APIs retain their contracts.
Idle is required. Authored Walk/Run/Sit/Sprawl become floor-conditioned autonomous candidates and explicit invocation targets; absent entries never become candidates. Jump is explicit and floor-only. Falling and Land supply fall/throw and landing visuals. Pinched supplies actual dragging without the stock resistance timeout. Missing physical visuals use silent owned Idle artwork. Custom motions run one authored sequence, then return to Idle; they never enter random selection.
Include generated `character-profile.properties` in the package. No full native scenario graph is copied. Same-motion reentry is dropped, and grab/fall takes priority without queuing user requests. Ready/click/menu are supported; no idle-random event is introduced. Exported ready fires once per actor after its first safe landing, with lifecycle cleanup on removal/stop. Limited live previews start grounded in Idle so ready is immediately testable. This lifecycle readiness check is distinct from a motion request queue.

Explicit Pinched invocation is a finite visual sequence. Actual dragging uses a separate internal physical action sharing its animation, so an event cannot capture the mouse indefinitely. A requested native template missing from the installation is rejected rather than silently treated as custom.

## Exit event (`exit`)
Visual rules support ready, click, exit and menu:<id>. Exit runs once per actor **before** character removal or normal application shutdown. Empty/unmatched reactions remove immediately. Actual motion/text/audio reactions run concurrently across actors for at most 2 seconds, then clean up; longer content is truncated. Explicit exit motions require the limited profile and use position-fixed visual actions, safe in midair or while dragging. Empty behavior preserves the current animation identity, progress and paused state rather than issuing a replacement command.
`AuthoringPreview.Session.dispatch("exit")` returns immediately; after the real reaction, state().closed() becomes true. Further replace/dispatch calls during exit are ignored. Session.close, preview replacement and module deactivation cancel immediately without generating exit. Duplicate requests share one reaction lifecycle. An independent deadline resolves removal even when preparation or the EDT stalls. Missing optional audio does not discard valid text/motion; missing audio alone adds no delay.
Character dismissal runs its own reaction. Main.exit/direct restart runs custom reactions before plugin/ticker cleanup. Normal exitWithFarewell excludes only custom exit targets from stock farewell and runs both phases concurrently; stock-only behavior and timing remain unchanged. Mixed application shutdown still includes the existing stock farewell and OS cleanup time. Existing microphone leases and the shared voice channel remain in force, so simultaneous voices are not guaranteed; actor visuals and bubbles are independent. Forced process termination/crashes cannot guarantee a farewell.

### Basic automatic animation authoring

`CharacterTemplates.automaticAnimations()` provides Stand (Idle), Walk, Run, Jump, Falling, Land, Pinched, Sit, Sprawl, GrabWall, ClimbWall, GrabCeiling and ClimbCeiling. Authors supply frame images, timing, anchors and sound effects; the engine owns eligibility and movement. Walls and ceilings include window and work-area geometry. Dragging uses the neutral pose; wall animation editing uses one basic pose branch. Wall-grab/climb anchors are authored relative to screen-wall contact. Window sides add the native template's window-versus-screen anchor delta, preserving the same authored adjustment on both surfaces. The engine handles left/right mirroring and the currently contacted window.

`CharacterTemplates.automaticAnimationIds()` returns supported IDs without reading installed artwork. The original 13 entries are extended with HardLand, Stunned, Bouncing, GetUpHop, LookAround, TiltHead, SitAndDangleLegs, AlarmNotice, Farewell, HappyHop and HeadPat (24 total). Frame slots and bindings are SDK-owned opaque identifiers; callers must not interpret them as native XML structure.

- Falling/throwing: a floor landing after a drop greater than 550 engine units plays authored HardLand → Stunned → GetUpHop in order. If none is authored, evaluation continues. A drop greater than 320 with Bouncing authored makes two physical bounces using the same timeline. Otherwise Land is used, or Idle when absent. Thresholds belong to the engine. Attaching to a wall is not a floor landing.
- LookAround, TiltHead, SitAndDangleLegs and HappyHop are autonomous floor candidates. HappyHop performs two hops using one visual timeline and also receives the app's affection celebration and shortcut gesture requests. GetUpHop/Bouncing retain engine hop/landing physics independently of authored frame counts or timeline length.
- AlarmNotice responds only to app alarms/hourly notices. Only characters with the slot receive a hold; wall/ceiling characters descend first. Short authored timelines keep looping during the per-character hold and obey existing user-interruption rules.
- Farewell connects to the existing normal-exit presentation, independently of general Chatter requests. Existing quiet/screen-busy exclusions and bounded exit deadlines remain. It renders authored images even in midair and takes precedence over an active alarm. An absent slot adds no exit animation.
- HeadPat uses a fixed input region: the central horizontal 1/6–5/6 of the image's upper half. The app recognizes rubbing/holding; authors configure no directional parameters. One authored timeline stays active while patting; release uses HappyHop if authored, otherwise Idle. Body presses retain normal dragging.

No editable event conditions or runtime plugins are emitted. Missing optional animations never import built-in guide assets. Existing native and limited-profile behavior remains unchanged. Multi-character, garden/bongo/game/transformation, mouse-tracking-only and window-throw scenarios are outside this catalog.


`CharacterDefinition.generateAutomaticAnimations(imageSet,animations,previewImage)` and `generateStandaloneAutomaticAnimations(...)` accept existing `Motion`/`Frame` DTOs. Idle is required with empty or Stand templateId. Other names and template IDs must equal a provided catalog ID. Custom scenarios are rejected. Missing optional animations never execute; missing falling/dragging physics use a silent idle pose. Walking/running/climbing velocities are engine-owned so changing image count cannot disable locomotion. Floor candidates require floor contact, wall/ceiling candidates require the respective border, and airborne actors can return through Fall.

Output contains actions.xml/behaviors.xml/info.xml, character-profile.properties and necessary frame-sound metadata, without event/dialogue logic or exit-animation aliases. Standalone output rejects guide images. `AuthoringPreview.Snapshot.forAutomaticAnimations(animations,audio)` tests the same profile in the real engine without accepting an event document; inspect `Snapshot.automaticRuntime()`. Existing generators and plugin event/dialogue APIs remain available.

Automatic generators accept a project folder name as `imageSet`. `CharacterDefinition.validAutomaticImageSetName(name)` checks a safe single Windows directory component of 1–64 UTF-16 code units. Unicode, spaces and valid punctuation remain exact, without trimming, case conversion or Unicode normalization. Empty names, separators, `<>:"/\\|?*`, control characters, malformed surrogates, XML-invalid U+FFFE/U+FFFF, trailing spaces/dots, dot-prefixed names hidden by the character chooser, Windows device names (including extensions and superscript COM/LPT variants), and Lumi/Credit/Unused are rejected. Generation applies the same rule and escapes the display name as XML text in info.xml. This is the `img/<imageSet>/` directory name; plugin/mod identifiers retain their ASCII contract. Existing general and limited generators keep their existing name contracts.


### Sound folders named after the project

`CharacterDefinition.validSoundPath(path)` validates a relative WAV path under `sound/`. Folder and file names can include Unicode and spaces, for example `sound/My cat & 친구/effects/step.wav`. Existing ASCII paths and frame playback, looping and gain policies are retained. Frame/action/behavior inputs in `CharacterDefinition` and `AuthoringPreview`, and the host frame/action/behavior sound tables, use this same validation.

Absolute paths, empty components, `.`/`..` components, backslashes, control characters, malformed UTF-16, Windows-invalid characters, device names and trailing dots/spaces are rejected. Passing this syntax check grants no file access: existing approved mod resource resolution and verification still apply. Live preview audio map keys must match the frame's full relative path.


### Automatic activities and interactions (45 animations)

The original 24 entries are extended with Dash, SprintBrake, FallingThrown, FaceGlide, WindowCarry, WindowThrow, Parade, ParadeBump, HiFivePrepare, HiFive, Talk, Humming, Backstep, GatherCall, GatherSit, Tag, GiftHold, GiftThrow, GiftAwait, GiftCatch and Flying. The SDK supplies fixed app activities and roles from authored slots; creators configure no conditions, partners or event rules.

- Dash/SprintBrake enable sprinting and braking. An authored SprintBrake also follows an authored Run. FallingThrown replaces the Thrown visual while preserving release momentum.
- FaceGlide connects window-front traversal and reaching the window top. WindowCarry/WindowThrow obey existing app window-throw settings and target eligibility. Default carrying offsets are scaled from authored frame anchors.
- Parade/ParadeBump, HiFive/HiFivePrepare, Talk/Humming/Backstep, GatherCall/GatherSit, Tag and GiftHold/GiftThrow/GiftAwait/GiftCatch enable parade, high-five, conversational gestures, gathering, tag and gifting respectively. One related slot is sufficient to generate a complete flow. Missing preparation/travel poses use authored shared visuals or silent Idle; native character art/audio is never silently exported. New autonomous activities respect focus mode.
- Partners must use the same image set. Approach/contact roles and gift giver/receiver ownership are retained; gifts cannot trigger another set's receiver. Credit discovery/pickup and transformation branches are omitted. Gift props use the app's built-in Credit resources at runtime without copying them into the mod.
- After a catch, authored automatic characters wait in silent Idle instead of repeatedly hopping, then run the end reaction when no further delivery arrives. Catch/end requests refused while airborne remain pending until they can run. This does not change the built-in character's gift behavior.
- Talk also receives existing app speech requests. Flying plays only during app-directed travel. Speech visuals do not interrupt physics or active interactions.
- Authored frame count/duration cannot multiply braking distance or change the high-five hop path. Preparation/contact/window-throw phases remain engine-timed; frames play with authored timings within those phases and may end when their owning state completes.
- Real `forAutomaticAnimations` previews own three temporary actors for parade/gathering, two for other paired activities and one otherwise. State.mascotId identifies the primary actor. Replacement, close and owner shutdown clean up companions and owned gift props. Dismissing a companion does not freeze editing. Ordinary app character queries/achievements continue to exclude preview actors.

Only frame visuals, sound effects, timing and anchors are authored. Generated internal behavior names, connections and invocation metadata are implementation details. Held rotation/strength slots and credit discovery/pickup are not part of this extension.

<a id="animation-activities"></a>

## Linked animation metadata and full activity testing

`AnimationActivities.forAnimation(animationId, registered)` returns activities that use the selected animation. Pass the IDs authored in the current project as `registered`. `Idle` and the `Stand` template identify the same idle animation. This query neither imports assets nor registers animations.

- `Activity(id, roles, related, participantCount, available)` describes the activity, ordered stages per role, shared supporting poses and total participant count. `available` means the registered animation set enables the activity; it does not guarantee that terrain or partners are ready.
- `Role(id, steps)` describes logical stage order, not a fixed clock or a script for issuing behavior names. Movement, contact, branches and repetitions remain host controlled.
- `Step(id, animationId, effectiveAnimationId, muted, skipped)` distinguishes the editable slot from the actual visual donor. The resolver is shared with the host generator. A skipped stage has an empty effectiveAnimationId. `muted` means host-enforced silence, independently of authored audio settings.

On an automatic animation preview session, `startActivity(activityId)` starts the real engine activity using only session-owned characters and companions. `pauseActivity(true/false)` pauses or resumes it; `stopActivity()` cancels it. Call control methods off the EDT. Read `activityState()` for `ActivityState(activityId, phase, actors, reason)`. Each `ActivityActor(mascotId, roleId, animationId, stepId)` describes an actual executing role/stage; IDs may be empty outside known stages. Do not infer successful stage transitions from elapsed time.

Phases are `IDLE`, `RUNNING`, `PAUSED`, `COMPLETED`, `CANCELLED` and `FAILED`. Only `COMPLETED` represents normal completion. Missing conditions/partners, interruptions and timeouts are distinct outcomes. Replacement, closure and plugin shutdown release activity-owned props and audio. Ordinary characters, other sessions and user feature settings are not modified. The API does not accept arbitrary host behavior names or executable code.

```java
var activities = AnimationActivities.forAnimation("HiFive", registeredIds);
var activity = activities.stream().filter(AnimationActivities.Activity::available).findFirst();
if (activity.isPresent()) session.startActivity(activity.get().id());
var state = session.activityState();
```

`AnimationActivities` and activity transport are preview APIs. Default methods preserve existing `Session` implementations; unsupported implementations reject control calls with `UnsupportedOperationException`. Check support or use the corresponding host version.

A separate controlled configuration is prepared only after explicit activity start. Ordinary automatic app previews retain their autonomous activity. Controlled actors do not participate in unrelated automatic matchmaking. Stop and completion hold session actors at Idle until the next activity or snapshot replacement. Gift catch/wait/end uses the same finite flow as ordinary authored automatic characters, while head-pat runs one authored frame cycle before the release continuation. Ordinary character repetition/input rules are unchanged. A request that does not finish within 90 seconds is reported as `FAILED`.

<a id="held-motion"></a>

## Held physics rotation — `CharacterDefinition.HeldMotion` preview

`generateAutomaticAnimations(imageSet,animations,previewImage,HeldMotion)` and `generateStandaloneAutomaticAnimations(imageSet,animations,previewImage,HeldMotion)` opt this character into continuous held rotation with `PHYSICS`. `LEGACY` and the existing three-argument calls preserve sprite behavior and generated file bytes. Null is rejected. Built-in Lumi, other image sets and other actors keep independent behavior/state. The generated physical action class and `heldMotion=PHYSICS` in `character-profile.properties` persist the selection without the editor running. A host implementing this contract is required; the product version string alone does not establish support. Older hosts do not silently substitute legacy motion for physics output.

The pivot uses the existing hold `Offset=0,120` and each frame's ImageAnchor: image coordinates `ImageAnchor − (0,120)`, or `(96,72)` for Lumi's `(96,192)` anchor. It does not infer the click location or opaque image region. The pivot follows the cursor while the existing virtual-foot inertia/damping model drives continuous rotation. Faster mouse movement produces larger deflection, which settles after stopping. Mirroring and scaling preserve the same pivot. Frame order/timing/anchors, FRAME_ENTRY/BEHAVIOR_ENTRY, loop and gain remain intact; cursor movement never restarts animation time. Release, state transitions and disposal clear rotation. Public `MascotState.bounds` describes the transformed displayed area; `anchor` retains its logical position. Display bounds are not physics collision bounds.

`AuthoringPreview.Snapshot.forAutomaticAnimations(animations,audio,HeldMotion)` carries the same option into the real engine, exposed by `Snapshot.heldMotion()`. It is the ninth Snapshot component; the existing eight-argument constructor and other factories/constructors default to `LEGACY`. `PHYSICS` outside the automatic profile is rejected. The `held` activity returned by `AnimationActivities.forAnimation("Pinched",registered)` is available when Pinched is registered and uses one actor. `Session.startActivity("held")` prepares one idle test actor and waits for the user to pick it up and move it on the desktop. It never synthesizes cursor motion or a scripted timeline. It reports the actual `Pinched` stage, completes after release and return to Idle, and fails with timed-out after 90 seconds. Existing pause/resume, stop/replacement/close ownership and audio cleanup contracts apply. UIs should close the session after completion/failure/stop to remove the owned actor.

### Read-only reconstruction of historical automatic output

New automatic characters and live tests use the existing `generateAutomaticAnimations(..., HeldMotion)` / `generateStandaloneAutomaticAnimations(..., HeldMotion)` and `AuthoringPreview.Snapshot.forAutomaticAnimations(..., HeldMotion)` APIs. Unregistered activities are not added implicitly.

`CharacterDefinition.HelperVisuals` and the **five-argument automatic generation overloads** are compatibility APIs for byte comparison and recovery of historical output. `LEGACY` reproduces the original output, `SHARED` reproduces brief, silent first-Idle helper poses, and `SHARED_MOVEMENT` reproduces historical tag/parade activities and image/sound donor rules. Supply the original frames, order, timing, anchors, audio and policies to reconstruct identical files. Compare the returned files in a separate temporary location; generation itself does not modify an installed mod or user project.

These policies are not new authoring features. No helper-policy argument or field is added to `AnimationActivities` or `AuthoringPreview.Snapshot`. Activity metadata and live tests retain their existing APIs and default execution rules. Existing generation overloads, built-in Lumi and existing mod behavior remain unchanged.

<a id="workshop-publishing"></a>

## Direct Workshop publishing

`PluginContext.workshopPublisher()` publishes an exported mod to the explicitly confirmed current Steam account. Call `WorkshopPublisher.account()` off EDT; it returns availability, Steam ID and display name. `publish(Request, Listener)` is asynchronous with off-EDT callbacks. The caller must confirm the account, title, new/update target and initial visibility before invoking it.

`Request(contentDirectory, previewFile, title, description, existingItemId, expectedSteamId, clientProjectId, visibility)` uses an empty item ID for a new item and explicit `Visibility.PUBLIC/PRIVATE`. Updates preserve existing visibility. Keep `clientProjectId` stable across saves and renames. The host validates and copies content to an independent snapshot, rechecks the account, and verifies existing item ownership through Steam. Titles accept at most 128 UTF-8 bytes, descriptions fewer than 8000 bytes, PNG/JPEG previews less than 1 MiB.

`created(itemId, ownerSteamId)` means item creation, not successful content upload. Before submission the host journals the identity per plugin/project/account so a retry updates the same item. Also retain it in the project. `completed(Result)` includes ID, owner, web URL and whether agreement acceptance remains. `failed(Failure)` can carry an ID after creation followed by submission failure. Steam creation and local journal persistence cannot be atomic: a forced process exit in that small gap may leave an empty item to inspect on Steam.

`close()` refuses further requests and suppresses UI callbacks, stopping work before submission when possible. Accepted Steam submissions cannot be rolled back; the host retains its snapshot until completion. Context release closes all its services. `PluginTestKit` returns an unavailable account without connecting to Steam. Tests must not automatically publish actual Workshop content.

### Exact upload inventory and caller-source lifetime

The existing eight-argument `WorkshopPublisher.Request` constructor retains general mod inspection. To publish a fixed file inventory, use the ten-argument constructor ending in `Map<String,String> expectedFiles, String expectedPreviewSha256`. A non-null map selects strict mode and requires both the **relative path→lowercase SHA-256** inventory fixed before user confirmation and the SHA-256 of the separate raw PNG/JPEG preview. Paths use `/`; absolute paths, traversal and case-insensitive collisions are rejected. The request defensively copies the map.

Strict inspection compares every actual source and SDK-copy file, including hidden files and local markers such as `workshop-upload.txt`. Missing, additional or changed files stop publishing; the host never rebaselines confirmation against changed bytes. The separate preview is read with a bound below 1MiB, checked against its raw hash, then reencoded as PNG. Prepared content and the encoded preview are checked again immediately before setting Steam content and submitting the update, including after an asynchronous new-item creation response. General source-marker exclusions remain compatible, but unexpected markers subsequently added to the transfer copy are rejected.

`Listener.sourceReleased()` runs exactly once off EDT when the SDK will no longer read `contentDirectory` or `previewFile`. It runs immediately after successful independent preparation and also on preparation failure, rejection or cancellation. Unlike UI callbacks, this lifetime notification still runs after `close()`, allowing the caller to delete its temporary distribution directory. It is not publication success. Existing close-time suppression of `progress/created/completed/failed` UI callbacks remains unchanged. The SDK retains and cleans its own snapshot through Steam completion/failure; local mod originals are not changed. Transient file locks receive bounded cleanup retries; a final cleanup failure is logged locally without turning remote success into failure.

Strict SDK snapshots use owner-private POSIX permissions or Windows current-user/SYSTEM ACLs. Only default byte streams are copied, without propagating source ACLs, attributes or NTFS alternate streams. This is not a sandbox against approved in-process code or a malicious process under the same account, and it cannot roll back a submission already accepted by Steam.
