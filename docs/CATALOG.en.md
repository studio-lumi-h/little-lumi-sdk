<!-- 자동 생성 — 손대지 말 것 · python build/gen_plugin_catalog.py -->
# Little LUMI Model — plugin catalogue

Lookup tables of names and values for **SDK/app 1.3.0, API 1**. Generated from the sources by the build — never hand-edited (the SDK build refuses a stale copy). Rules and examples live in [API.en.md](API.en.md); this file is names and values only. [한국어](CATALOG.md) · [Getting started](../README.en.md).

[Behaviours](#behaviours) · [Events](#events) · [Hooks](#hooks) · [Mascot state](#mascot-state) · [App settings](#app-settings) · [Setting types](#setting-types) · [Feature IDs](#feature-ids)

For what a running character can do, query `ctx.behaviorNames(imageSet)`. Keep plugin options in `ctx.prefs()`, and prefer a public getter over an app setting key.

<a id="behaviours"></a>

## 1. Behaviour names — `ctx.behave(name)` · `ctx.behaviorNames(imageSet)`

Every `<Behavior Name>` of `img\Lumi\conf\behaviors.xml`, with the Korean display names from `행동목록.txt`. Other image sets follow their own `behaviors.xml`, so at run time `ctx.behaviorNames(imageSet)` is the truth. The 150 donation jumps `JumpDono_x<0..9>_y<0..14>` are generated per coordinate and folded into one row.

**hidden** (`Hidden="true"`) only hides the behaviour from the character menu. Whether it is picked automatically is decided by **Frequency** and `Condition` — Frequency is a selection weight, not a duration or a fixed percentage; zero excludes it from automatic selection, but it can still be called by name or reached from another behaviour. Several hidden behaviours have Frequency > 0. **toggle** (`Toggleable="true"`) can coexist with hidden. `Dragged` and `Thrown` are the behaviours the engine starts from mouse input.

| Name | Korean | Frequency | Hidden |
|---|---|---|---|
| `ChaseMouse` | 마우스 쫓아가기 | 0 | hidden |
| `SitAndFaceMouse` | 앉아서 마우스 향하기 | 0 | toggle |
| `SitAndSpinHead` |  | 0 | hidden |
| `Fall` | 추락 | 0 | hidden |
| `Dragged` | 들려 있음(엔진) | 0 | hidden |
| `Thrown` | 던져짐(엔진) | 0 | hidden |
| `Talk` | 입 뻐끔(말하는 동안 — ctx.mouthFlap 이 반복해 건다) | 0 | hidden |
| `Flying` | 자리로 날아가기(자바만 부른다) | 0 | hidden |
| `AlarmNotice` | 알람 「어!」(자바만 부른다) | 0 | hidden |
| `HeadPat` | 쓰다듬기 | 0 | toggle |
| `HeadPatTilt2` |  | 0 | hidden |
| `HeadPatTilt3` |  | 0 | hidden |
| `HeadPatSit` |  | 0 | hidden |
| `HeadPatSitTilt2` |  | 0 | hidden |
| `HeadPatSitTilt3` |  | 0 | hidden |
| `HeadPatSitFacing` |  | 0 | hidden |
| `HeadPatSitFacingTilt2` |  | 0 | hidden |
| `HeadPatSitFacingTilt3` |  | 0 | hidden |
| `HeadPatSitLedge` |  | 0 | hidden |
| `SelfTalk` | 혼잣말 | 0 | toggle |
| `ChatHost` |  | 0 | hidden |
| `ChatGuest` |  | 0 | hidden |
| `HiFiveHost` |  | 0 | hidden |
| `HiFiveGuest` |  | 0 | hidden |
| `GiftGiver` |  | 0 | hidden |
| `GiftReceiver` |  | 0 | hidden |
| `GiftDing` |  | 0 | hidden |
| `GiftEnd` |  | 0 | hidden |
| `GiftCatch` |  | 0 | hidden |
| `GiftHopLoop` |  | 0 | hidden |
| `CreditSpot` |  | 0 | hidden |
| `ParadeBump` |  | 0 | hidden |
| `GatherSit` |  | 0 | hidden |
| `GatherCallAgain` |  | 0 | hidden |
| `TagWin` |  | 0 | hidden |
| `TagCaught` |  | 0 | hidden |
| `CreditGet` |  | 0 | hidden |
| `SkyReturn` |  | 900 | hidden |
| `VoidRescue` |  | 50 | hidden |
| `EventDrop` |  | 0 | hidden |
| `PullUp` |  | 0 | hidden |
| `Divided` |  | 0 | hidden |
| `StandUp` | 일어서기 | 60 | hidden toggle |
| `LookAround` | 도리도리 | 120 | toggle |
| `TiltHead` | 흥얼흥얼 | 120 | toggle |
| `Chatter` | 인사하기 | 90 | toggle |
| `CreditEvent` | 크레딧 획득 | 150 | toggle |
| `CreditRetry` | 남은 크레딧 주우러 가기 | 60 | hidden toggle |
| `ChatInvite` | 말 걸기 | 20 | toggle |
| `ChatApproach` |  | 70 | hidden toggle |
| `LeadParade` | 줄줄이 행진 | 20 | toggle |
| `JoinParade` |  | 70 | hidden toggle |
| `GatherCall` | 모여 모여 | 20 | toggle |
| `GatherJoin` |  | 70 | hidden toggle |
| `TagRun` | 술래잡기 | 20 | toggle |
| `TagChase` |  | 70 | hidden toggle |
| `HiFiveInvite` | 하이파이브 | 20 | toggle |
| `HiFiveApproach` |  | 70 | hidden toggle |
| `GiftInvite` | 크레딧 나눠주기 | 20 | toggle |
| `GiftApproach` |  | 70 | hidden toggle |
| `SprintAndBrake` | 전력질주 | 130 | toggle |
| `ClimbIEFace` | 창 타고 오르기 | 90 | toggle |
| `ClimbIEFaceDrop` | 창 타다 미끄러지기 | 50 | hidden toggle |
| `FloorGrabIEWall` | 창 옆면 매달리기 | 70 | toggle |
| `WindowDive` | 창에서 뛰어내리기 | 0 | hidden |
| `HappyHop` | 폴짝폴짝 | 150 | toggle |
| `SitDown` | 앉기 | 80 | toggle |
| `SitWhileDanglingLegs` | 걸터앉기 | 50 | toggle |
| `LieDown` |  | 0 | hidden |
| `SplitIntoTwo` |  | 0 | hidden |
| `HoldOntoWall` | 벽 붙잡기 | 100 | hidden toggle |
| `FallFromWall` | 벽에서 떨어지기 | 50 | hidden toggle |
| `FocusHoldOntoIEWall` | 벽 조용히 붙잡기 | 100 | hidden |
| `HoldOntoCeiling` | 천장 붙잡기 | 0 | hidden |
| `FallFromCeiling` | 천장에서 떨어지기 | 50 | hidden toggle |
| `FocusHoldOntoCeiling` | 천장 조용히 붙잡기 | 100 | hidden |
| `WalkAlongWorkAreaFloor` | 작업 공간 바닥 걷기 | 100 | hidden toggle |
| `RunAlongWorkAreaFloor` | 작업 공간 바닥 달리기 | 100 | hidden toggle |
| `CrawlAlongWorkAreaFloor` | 바닥 기어가기 | 0 | hidden |
| `WalkLeftAlongFloorAndSit` | 왼쪽으로 걷다가 앉기 | 100 | hidden toggle |
| `WalkRightAlongFloorAndSit` | 오른쪽으로 걷다가 앉기 | 100 | hidden toggle |
| `GrabWorkAreaBottomLeftWall` |  | 0 | hidden |
| `GrabWorkAreaBottomRightWall` |  | 0 | hidden |
| `WalkLeftAndSit` | 왼쪽으로 달려가서 앉기 | 100 | hidden toggle |
| `WalkRightAndSit` | 오른쪽으로 달려가서 앉기 | 100 | hidden toggle |
| `RunAndGrabBottomLeftWall` |  | 0 | hidden |
| `RunAndGrabBottomRightWall` |  | 0 | hidden |
| `JumpFromBottomOfIE` | 창 아래에서 뛰어오르기 | 40 | hidden toggle |
| `PullUpShimeji` |  | 0 | hidden |
| `ClimbHalfwayAlongWall` | 벽 따라 일부 오르기 | 50 | hidden toggle |
| `ClimbAlongWall` | 벽 따라 오르기 | 100 | hidden toggle |
| `ClimbAlongCeiling` | 천장 따라 오르기 | 100 | hidden toggle |
| `WalkAlongIECeiling` | 창 천장 따라 걷기 | 100 | hidden toggle |
| `FaceFromTop` |  | 80 | hidden toggle |
| `RunAlongIECeiling` | 창 천장 따라 달리기 | 100 | hidden toggle |
| `CrawlAlongIECeiling` |  | 0 | hidden |
| `SitOnTheLeftEdgeOfIE` | 창 왼쪽 가장자리에 앉기 | 100 | hidden toggle |
| `SitOnTheRightEdgeOfIE` | 창 오른쪽 가장자리에 앉기 | 100 | hidden toggle |
| `JumpFromLeftEdgeOfIE` | 창 왼쪽 가장자리에서 뛰기 | 100 | hidden toggle |
| `JumpFromRightEdgeOfIE` | 창 오른쪽 가장자리에서 뛰기 | 100 | hidden toggle |
| `WalkLeftAlongIEAndSit` | 창 따라 왼쪽으로 걷다가 앉기 | 100 | hidden toggle |
| `WalkRightAlongIEAndSit` | 창 따라 오른쪽으로 걷다가 앉기 | 100 | hidden toggle |
| `WalkLeftAlongIEAndJump` | 창 따라 왼쪽으로 걷다가 뛰기 | 100 | hidden toggle |
| `WalkRightAlongIEAndJump` | 창 따라 오른쪽으로 걷다가 뛰기 | 100 | hidden toggle |
| `HoldOntoIEWall` | 창 벽 붙잡기 | 100 | hidden toggle |
| `ClimbIEWall` | 창 벽 오르기 | 100 | hidden toggle |
| `ClimbGardenLadder` | 섬 사다리 오르기 | 120 | hidden toggle |
| `ClimbIEBottom` | 창 바닥 오르기 | 100 | hidden toggle |
| `GrabIEBottomLeftWall` | 창 왼쪽 아래 벽 붙잡기 | 100 | hidden toggle |
| `GrabIEBottomRightWall` | 창 오른쪽 아래 벽 붙잡기 | 100 | hidden toggle |
| `JumpFromLeftWall` | 창 왼쪽 벽에서 뛰기 | 0 | hidden |
| `JumpFromRightWall` | 창 오른쪽 벽에서 뛰기 | 0 | hidden |
| `JumpOnIELeftWall` | 창 왼쪽 벽으로 뛰기 | 50 | hidden toggle |
| `JumpOnIERightWall` | 창 오른쪽 벽으로 뛰기 | 50 | hidden toggle |
| `ThrowIEFromLeft` |  | 120 | toggle |
| `ThrowIEFromRight` |  | 120 | toggle |
| `RunAndThrowIEFromLeft` |  | 120 | toggle |
| `RunAndThrowIEFromRight` |  | 120 | toggle |
| `JumpDono_x<n>_y<m>` ×150 | | 0 | hidden |

<a id="events"></a>

## 2. Event topics and payloads — `ctx.on(topic, event -> …)`

Items marked “preview” may change or disappear without an `api_version` bump (the same list is under `preview` in `sdk.json`). Unmarked items are stable — [API.en.md §8](API.en.md#compatibility).

Values are strings unless a type is given. `mascotId` is valid only while that character lives (it may be reused after it leaves); coordinates are desktop coordinates. In the app, listeners run on the plugin's own event thread; the test kit runs them on the calling thread. Payload examples and integration caveats: [the events section of API.en.md](API.en.md#events).

| Topic | Status | Meaning and fields |
|---|---|---|
| `ready` |  | the engine is up and the plugins have started — none |
| `behavior` |  | a mascot's behaviour changed — `imageSet`, `name`, `mascotId` (Integer) |
| `bubble` |  | a bubble went up — `imageSet`, `mascotId` (Integer, 0 = by image set; absent for `kind=choice`), `text` (as shown — after the `bubble.text` hook), `ms` (Long, the final duration after `bubble.duration`; the timeout for `choice`), `kind` (`say` = a plain line incl. queued lines and `say.txt`, `private` = an alarm/errand line the broadcast stage skips, `choice` = a bubble with buttons opened). The 「···」 busy indicator publishes nothing |
| `choice` | preview | a choice bubble was answered — `imageSet`, `mascotId` (Integer), `index` (Integer, 0-based · `-1` = opened, no answer (timeout/Escape/character gone) · `-2` = withdrawn by the app (superseded by the same plugin / `clearQueue` / waited too long / line full)), `option` (the label, `""` when negative), `from` (id of the plugin that asked) |
| `click` |  | a mouse button went down on a character — `imageSet`, `mascotId` (Integer), `button` (`left`/`right`/`middle`), `x`, `y` (Integer, screen coordinates) |
| `mascot` |  | a character appeared or left — `imageSet`, `mascotId` (Integer), `event` (`added`/`removed`) |
| `headpat` |  | a head-pat began — `behavior` |
| `affection` |  | the shown affection tier changed — `tier` (Integer), `previous` (Integer), `points` (Integer) |
| `achievement` |  | a Steam achievement unlocked — `id` |
| `focus` |  | focus mode engaged or released — `active` (Boolean) |
| `chat` |  | Chat traffic topic listed by the app API — `phase` (`ask`/`reply`), `imageSet`, `text`. Availability depends on the installed chat integration. `ctx.publish("chat", ...)` publishes `<your-id>.chat`, not this app topic. |
| `tts` |  | a voice line started or ended — `phase` (`start`/`end`) |
| `settings` |  | the settings window was applied, or the language changed — `phase` (`apply`), `changed` (comma-joined keys the app wrote through `LumiSettings` since the previous apply — stock engine keys are not journaled) / `phase` (`language`), `language` (BCP-47) |
| `ask` |  | `speech\ask.txt` was written — `text` |
| `settings_open` |  | `speech\settings_open.txt` was written — none |
| `say_file` |  | a `speech\say.txt` line was shown — `text` |
| `drag` | preview | the user picked a character up or let go (`Dragged` began/ended) — `phase` (`start`/`end`), `imageSet`, `mascotId` (Integer), `x`, `y` (Integer, screen coordinates of the anchor) |
| `throw` | preview | let go with momentum (`Thrown` began) · that flight ended (the next behaviour began) — `phase` (`thrown`/`landed`), `imageSet`, `mascotId` (Integer), `x`, `y` (Integer), `next` (always present — the behaviour after the flight on `landed`, `""` on `thrown`) |
| `hover` | preview | the pointer entered or left a character's window — `phase` (`enter`/`exit`), `imageSet`, `mascotId` (Integer) |
| `doubleclick` | preview | two left presses on the same character within 600 ms and 90 px (the AWT click count cannot be trusted — the press starts a drag that moves the window) — `imageSet`, `mascotId` (Integer), `x`, `y` (Integer). Both presses also produce `click` |
| `alarm` | preview | a reminder was actually delivered — `kind` (`hourly`/`alarm`), `time` (`HH:mm`), `late` (Boolean — an alarm held while the screen was busy) |
| `pomodoro` | preview | the focus/break cycle moved — `phase` (`focus`/`break`/`stopped`), `minutes` (Integer, length of the phase that begins; 0 on stop), `round` (Integer, focus phases started since the cycle was switched on) |
| `bongo` | preview | bongo mode drew a keystroke or click — `kind` (`keyboard`/`left_click`/`right_click`), `frame` (Integer, the hand slot drawn), `look` (`bongo`/`small`). **Never a key code or character** (the core does not read keys) |
| `app` | preview | the app is going down through its own exit door — `phase` (`exit`). Before the plugins' `stop()`; delivery is awaited with a short bound |
| `island` | preview | the garden (island) process was asked to start or stop, or served a photo request — `phase` (`start`/`stop`/`photo`), `request` (photo only) |
| `game` | preview | the contraption game board opened, started a run, or closed — `phase` (`start`/`run`/`stop`) |

<a id="hooks"></a>

## 3. Hooks and ctx keys — `ctx.hook(id, (hook, current, ctx) -> …)`

Items marked “preview” may change or disappear without an `api_version` bump (the same list is under `preview` in `sdk.json`). Unmarked items are stable — [API.en.md §8](API.en.md#compatibility).

For every id in `PluginHooks.KNOWN`: where the core asks (the app source file — several are joined with ` · `), the asking method, the default expression and the keys of the ctx map. The source paths say where the question is asked; those files are not part of the SDK. See [the hook table of API.en.md](API.en.md#hooks) first for return types, bounds and special values. `null` passes (the earlier `current` stays); a later handler that returns a non-null value replaces the earlier answer. Never touch files, the network or locks inside a hook.

| Hook | Status | Asked at | Method | Default | ctx keys |
|---|---|---|---|---|---|
| `affection.award` |  | `com/group_finity/mascot/lumi/AffectionDirector.java` | `decideInt` | `points (clamp 0..1_000_000)` | `source`, `points`, `dailyCap`, `cooldownSeconds`, `todayPoints` |
| `affection.tier` |  | `com/group_finity/mascot/lumi/AffectionDirector.java` | `decideInt` | `tier (clamp 1..MAX_TIER)` | `points` |
| `behavior.force` |  | `com/group_finity/mascot/LumiBridge.java` | `decideString` | `name == null ? "" : name` | `name`, `caller`, `mascotId`, `imageSet` |
| `bongo.key_frame` |  | `com/group_finity/mascot/lumi/BongoWindow.java` | `decideInt` | `chosen (clamp -1..keyFrames() - 1)` | `frames`, `cycle`, `look` |
| `bongo.key_input` |  | `com/group_finity/mascot/lumi/BongoWindow.java` | `decideInt` | `-2 (clamp -2..bongo.keyFrames() - 1)` | `frames`, `look` |
| `bubble.duration` |  | `com/group_finity/mascot/lumi/SelfTalkDirector.java` · `com/group_finity/mascot/lumi/SpeechDirector.java` | `decideLong` | `life (clamp 500..120_000)` · `computed (clamp 500..120_000)` | `text`, `requested`, `source`, `voiceMs`, `mascotId`, `imageSet` |
| `bubble.text` | preview | `com/group_finity/mascot/lumi/SpeechDirector.java` | `decideString` | `text` | `text`, `source`, `mascotId`, `imageSet` |
| `focus.quiet` |  | `com/group_finity/mascot/lumi/FocusDirector.java` | `decideBoolean` | `quiet` | `hidden`, `fullscreenAhead`, `focusActive` |
| `focus.screen_busy` |  | `com/group_finity/mascot/lumi/FocusDirector.java` | `decideBoolean` | `busy` | `hidden`, `fullscreenAhead` |
| `place.allowed` |  | `com/group_finity/mascot/lumi/TetherDirector.java` | `decideBoolean` | `allowed` | `x`, `y`, `confinement`, `mover` |
| `selftalk.interval` |  | `com/group_finity/mascot/lumi/SelfTalkDirector.java` | `decideLong` | `interval (clamp 3_000..3_600_000)` | `baseMs`, `frequency`, `minSec`, `maxSec` |
| `selftalk.line` |  | `com/group_finity/mascot/lumi/SelfTalkDirector.java` | `decideString` | `line` | `candidate`, `onGardenFloor`, `gardenPhase`, `tier`, `offered`, `mascotId`, `imageSet` |
| `sound.effect` |  | `com/group_finity/mascot/Mascot.java` | `decideString` | `soundPath` | `path`, `caller` |
| `speech.greeting` |  | `com/group_finity/mascot/lumi/SpeechDirector.java` | `decideString` | `picked == null ? "" : picked` | `pool`, `last`, `trigger`, `imageSet` |
| `text.localized` |  | `com/group_finity/mascot/lumi/SpeechDirector.java` | `decideString` | `value` | `key`, `language`, `fallback` |

<a id="mascot-state"></a>

## 4. `MascotState` — the element of `ctx.mascots()`

A Java record, so read it through methods: `state.id()`, `state.imageSet()`, `state.anchor()`. The value is a snapshot of that moment, and `ctx.mascotById(id)` returns `null` when that character is gone.

| Field | Type |
|---|---|
| `id` | `int` |
| `imageSet` | `String` |
| `bounds` | `Rectangle` |
| `anchor` | `Point` |
| `lookRight` | `boolean` |
| `behavior` | `String` |

<a id="app-settings"></a>

## 5. App setting keys — `ctx.setting(key, fallback)` (read only)

● = `Preferences.KEYS` (preference values that go to Steam Cloud; mirrored in `conf\preferences.properties`). The rest are keys the app really reads from and writes to `conf\settings.properties`, collected from the call sites that go through the settings store (`LumiSettings`). All values are strings, the list grows with app versions, and a row here is not a fixed contract.

Read with `ctx.setting(key, fallback)` only when there is no public getter, and handle a missing value — the language is `language()`, the active sets `activeImageSets()`, the features `featureEnabled(id)`. **A plugin does not write app keys** — its own state goes to `ctx.prefs()`.

| Key | Preference (cloud) |
|---|---|
| `Language` | ● |
| `ActiveShimeji` | ● |
| `Scaling` | ● |
| `Opacity` | ● |
| `Sounds` | ● |
| `Theme` | ● |
| `SettingsAlwaysOnTop` | ● |
| `AlwaysShowInformationScreen` | ● |
| `HideOnFullscreen` | ● |
| `SpeechFarewell` | ● |
| `AffectionCelebrate` | ● |
| `AlarmEnabled` | ● |
| `AlarmTime` | ● |
| `HourlyChime` | ● |
| `Pomodoro` | ● |
| `PomodoroFocusMinutes` | ● |
| `PomodoroBreakMinutes` | ● |
| `lumi.sound.volume` | ● |
| `lumi.sound.off` | ● |
| `lumi.voice.volume` | ● |
| `lumi.voice.pack` | ● |
| `lumi.selftalk.frequency` | ● |
| `lumi.shortcuts.gesture` | ● |
| `lumi.input.reaction` | ● |
| `lumi.focus.enabled` | ● |
| `lumi.focus.auto` | ● |
| `lumi.island.enabled` | ● |
| `lumi.game.enabled` | ● |
| `lumi.windowthrow.enabled` | ● |
| `lumi.steam.presence.enabled` | ● |
| `lumi.bongo.look` | ● |
| `lumi.bongo.input` | ● |
| `lumi.bongo.size` | ● |
| `lumi.bongo.enabled` | |
| `lumi.bongo.gamemode` | |
| `lumi.bongo.x` | |
| `lumi.bongo.y` | |
| `lumi.broadcast.enabled` | |
| `lumi.broadcast.private.hide` | |
| `lumi.broadcast.source.lan` | |
| `lumi.broadcast.source.port` | |
| `lumi.broadcast.window.minimized` | |
| `lumi.confguard.all` | |
| `lumi.debug.enabled` | |
| `lumi.island.tutorialShown` | |
| `lumi.onboarding.step` | |
| `lumi.shake.credit.state` | |
| `lumi.steam.enabled` | |
| `lumi.transform.art.enabled` | |

<a id="setting-types"></a>

## 6. `type` of a `plugin.json` `"settings"` entry

Values: `bool`, `int`, `string`, `choice`, `hotkey`. Entry shape and per-language labels: [the manifest section of API.en.md](API.en.md#manifest).

<a id="feature-ids"></a>

## 7. Feature switch ids — `ctx.featureEnabled(id)`

The settings-window switches. Read only — a plugin never flips them. False when off or for an unknown id.

| id | App setting key |
|---|---|
| `bongo` | `lumi.bongo.enabled` |
| `broadcast` | `lumi.broadcast.enabled` |
| `island` | `lumi.island.enabled` |
| `game` | `lumi.game.enabled` |
| `focus.auto` | `lumi.focus.auto` |
