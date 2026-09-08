# Little LUMI Model — 플러그인 API 참조 (api_version 1)

**SDK/앱 1.3.0, `api_version` 1** 기준 참조입니다. 이 문서는 플러그인이 기대도 되는 표면 전부이고, 여기 적힌 것만이 앱 업데이트를 건너 살아남는다고 약속합니다([호환 정책](#compatibility)). 같은 폴더의 `javadoc\index.html` 은 아래 타입의 시그니처·주석을 그대로 옮긴 것이고, `api-src\` 는 그 소스 파일들입니다. 처음이면 [첫걸음 안내](../plugin-guide.html)부터 따라 하고, 이름을 찾을 때는 [카탈로그](CATALOG.md)를 쓰세요. [English](API.en.md).

[매니페스트](#manifest) · [생명주기](#lifecycle) · [메서드](#context) · [이벤트](#events) · [훅](#hooks) · [바이트코드](#bytecode) · [데이터 조각](#data-mods) · [호환 정책](#compatibility) · [하지 말 것](#practices) · [테스트](#testing) · [카탈로그](#catalogue) · [약속 밖](#alternatives) · [자주 쓰는 코드](#recipes)

| 타입 (`com.group_finity.mascot.lumi.plugin`) | 역할 |
|---|---|
| `LumiPlugin` | 진입점 인터페이스 — `start(PluginContext)` / `stop()` |
| `PluginContext` | 앱이 건네는 문 — 말하기·행동·상태 조회·제어·이벤트·훅·설정·UI 자리·단축키·바이트코드 |
| `MascotState` | 캐릭터 하나의 지금 상태(id·이미지셋·사각형·발끝·방향·행동, 바탕화면 좌표) — `ctx.mascots()` 가 돌려주는 레코드 |
| `PcmAudio` | 재생할 소리 한 토막(형식·PCM 바이트·길이 ms) — `ctx.speak()` 가 받는 레코드 |
| `HotkeyResult` | `ctx.bindHotkey()` 의 답 — `OK` `NONE` `BAD_COMBINATION` `REFUSED` `UNSUPPORTED` |
| `PluginTheme` | 앱의 옷 — 색·CJK 글꼴·단추/토글/슬라이더 손질·카드 조각 (`ctx.theme()`) |
| `Json` | 의존성 없는 JSON 읽기·쓰기(`parse` `get` `getString` `write`) — LLM 답장·설정 파일에 |
| `PluginEvents` | 「일어난 뒤 알림」 버스(토픽·페이로드 표는 §4) |
| `PluginHooks` | 「정하기 전 질문」(훅 규약·카탈로그는 §5), `PluginHooks.Handler` |
| `LumiTransformer` | 로드 전 클래스 변환기 서비스 |
| `PluginDescriptor` | 읽힌 `plugin.json` |
| `PluginPrefs`, `PluginText` | 제 설정 저장소(`ctx.prefs()`)와 제 5개 언어 문구(`ctx.text()`) |
| `PluginUi.MenuHandle`, `PluginUi.FeatureToggle` | 메뉴 줄·단추의 손잡이와 [적용] 시점의 기능 스위치 |
| `ScheduledHandle` · 실험 | 예약 작업의 취소·취소 여부 |
| `PluginSettingsSchema` | 선언형 설정 파서 — 보통은 `plugin.json` 에 선언만 하면 됩니다 |
| `PluginTestKit` · 실험 | 가짜 컨텍스트·호출 기록·직접 미는 시계(§10) |

<a id="manifest"></a>

## 1. plugin.json

jar 뿌리에 둡니다. 파서가 필수로 검증하는 것은 `id` 뿐입니다. `name` 이 없거나 비면 id 가 대신 보이니, 배포할 때는 알아볼 이름을 적으세요.

| 키 | 형 | 뜻 |
|---|---|---|
| `id` | 문자열 | `[a-z0-9._-]` 1..64(첫 글자는 영문 소문자·숫자). `plugindata\<id>\` 폴더 이름, 로그 이름, 단축키 접두 |
| `name` | 문자열 | 모드 탭에 보이는 이름. 없으면 `id` |
| `version` | 문자열 | 모드 탭 「v…」 칩 |
| `author`, `description` | 문자열 | 표시용 |
| `dependencies` | 배열 | 먼저 실려야 하는 플러그인 id. 없으면 이 플러그인은 건너뜁니다 |
| `api_version` | 정수 | 이 문서의 호환 번호(§8). 생략하면 `1` |
| `app_version` | 문자열 | 검증한 앱 버전. 실행 중인 앱과 다르면 모드 탭에 「N용」 칩(안내일 뿐 막지 않습니다) |
| `affects_character` | 불리언 | 행동·대사를 바꿉니다(칩) |
| `network` | 불리언 | 인터넷을 씁니다(칩; 강제 아님) |
| `hooks` | 배열 | 답하는 훅 id. 이 앱에 없는 id 는 모드 탭에 표시만 하고 막지 않습니다 |
| `settings` | 배열 | **선언형 설정**. 모드 탭이 그 플러그인 줄의 「자세히」 안에 앱 테마·앱 언어로 그려 주고, 값은 `ctx.prefs()`(`plugindata\<id>\settings.properties`)에 **바꾸는 즉시** 저장합니다(저장 실패는 §3.7). 항목: `key`(필수) · `type` = `bool` `int` `string` `choice` `hotkey` · `label`/`hint` = 문자열 하나 또는 `{"ko":…,"en":…,"ja":…,"zh":…,"zh_TW":…}`(정확한 언어 → 언어 → `en` → 첫 값; 번체는 `zh` 로 새지 않습니다) · `default` · `min`/`max`/`step`(int) · `options`(choice: `[{"value","label"}]` 또는 문자열). 틀린 항목은 로그에 남기고 건너뛰며 실행은 막지 않습니다. 기본값은 `start()` 전에 **저장소에 없는 열쇠에만** 심어집니다 — 사용자가 바꾼 값은 덮지 않습니다 |

```json
{
  "id": "my.first.lumi",
  "name": "My first LUMI plugin",
  "api_version": 1,
  "app_version": "1.3.0",
  "settings": [
  { "key": "greet",  "type": "bool",   "default": true, "label": { "ko": "인사하기", "en": "Greet" } },
  { "key": "volume", "type": "int",    "default": 50, "min": 0, "max": 100, "step": 5, "label": "Volume" },
  { "key": "mood",   "type": "choice", "default": "calm", "label": { "ko": "말투", "en": "Mood" },
    "options": [ { "value": "calm", "label": { "ko": "차분", "en": "Calm" } }, { "value": "wild", "label": "Wild" } ] },
  { "key": "toggle", "type": "hotkey", "default": "Ctrl+Alt+L", "label": "Toggle key" }
  ]
}
```

플러그인 쪽: `ctx.prefs().getBoolean("greet", true)`, `ctx.prefs().onChange((key, value) -> …)`. `hotkey` 칸은 조합 글자만 저장합니다 — 실제 등록은 `onChange` 를 받아 `bindHotkey` 로 하세요(다른 프로그램이 쥐고 있는지는 그때 압니다).

진입 클래스는 `META-INF\services\com.group_finity.mascot.lumi.plugin.LumiPlugin` 에 적습니다(`ServiceLoader`). 변환기는 `…LumiTransformer` 서비스 파일에 적습니다.

<a id="lifecycle"></a>

## 2. 생명주기

- `start(ctx)` — 내장 기능이 뜬 뒤, 엔진(마스코트)이 실리기 **전**에 한 번 불립니다. 여기서 `on`/`hook`/`addTrayItem`/`onLoad` 를 겁니다. 이때 캐릭터 상태는 비어 있을 수 있으니 엔진이 필요한 일은 `ready` 에서 하세요. 던지면 그 플러그인만 실패로 표시됩니다.
- `ready` 이벤트 — 엔진이 뜨고 모든 플러그인의 `start` 가 끝난 뒤 한 번, 앱이 냅니다. `PluginTestKit` 은 대신 내 주지 않습니다(§10).
- `stop()` — 앱 종료 때 불립니다(기본 구현은 빈 것). `start()` 가 던진 경우에도 부분 시작을 정리하려고 곧바로 불리므로, 아직 초기화하지 못한 필드가 있어도 견디게 작성하세요. 플러그인과 한 jar 안의 서비스 구현들은 실제 시작의 역순으로 멈춥니다.
- `stop()` 뒤 **앱이 거둡니다**: 이 컨텍스트로 건 리스너·훅·전역 단축키·메뉴 줄·기능 스위치·예약 작업은 플러그인이 정리하지 않았어도 로더가 전부 해제합니다. 시작 실패 때도 곧바로 해제하며, 실패한 플러그인의 의존자는 시작하지 않습니다. 실행 중 켜기·끄기는 없습니다(모드 탭의 스위치는 다음 실행부터).
- **직접 만든 것은 직접 닫습니다**: 창·클라이언트·스트림·작업 스레드는 `stop()` 에서 닫으세요. **`prefs().onChange(...)` 리스너도 컨텍스트가 거두지 않습니다** — 등록 장부에 없으니 리스너를 들고 있다가 `stop()` 에서 `removeOnChange(listener)` 로 푸세요(§13 `QuietPlugin`). 이미 변환한 클래스는 되돌리지 않습니다.
- 바꾼 **상태도 되돌립니다**: `claimClickGreeting()` 의 점유는 이 플러그인이 아직 쥔 경우에만 풀리고, `summon()` 으로 부르고 아직 `dismiss()` 하지 않은 캐릭터는 보내며, `setCharactersVisible(false)` 로 숨겨 둔 채였으면 다시 보입니다. 뒤의 둘은 **앱이 계속 도는 동안**(시작 실패)에만 하고, 앱 종료 때는 캐릭터에 손대지 않습니다.
- 그 뒤로 **컨텍스트는 죽은 것**입니다: 늦게 오는 등록(`on`·`hook`·`every`·`after`·`onEdt`·`offEdt`·메뉴 줄·`addSettingsButton`·`addFeatureToggle`·`bindHotkey`·`claimClickGreeting`·`onLoad`·`patch`·`ask`·`summon`·`dismiss`·`moveTo`·`setCharactersVisible`)과 행동 문 전부(`say`·`sayQueued`·`showBusy`·`behave`·`speak`·`mouthFlap`·`heart`·`playSound`·`awardAffection`·`publish`·`setSetting`·`writeAtomic`·`manageWindow`)는 **던지지 않고** 무시됩니다 — 로그에 플러그인마다 한 줄 `<id>: registration after stop() ignored: <what>` 이 남고, 돌아오는 것은 무해한 값입니다(이미 취소된 `ScheduledHandle`, `removed()` 가 참인 `MenuHandle`/`FeatureToggle`, `HotkeyResult.REFUSED`, `summon` 은 -1, `dismiss`/`moveTo`/`onLoad`/`patch` 는 false). `ask` 의 콜백에는 `-2` 가 가는데, 이때만은 정상 경로의 전용 스레드가 아니라 **부른 스레드에서 그 자리에서** 전달됩니다. `mascots()`·`prefs()`·`text()` 같은 읽기는 계속 답합니다. 유일한 예외: `every()`/`after()` 는 `task == null`(그리고 `every` 의 `periodMs <= 0`)을 해제 여부보다 먼저 검사하므로 죽은 컨텍스트에서도 `IllegalArgumentException` 을 던집니다. `stop()` 안에서 제 스레드를 멈추면 이 줄을 볼 일이 없습니다.
- 같은 id 가 둘이면 모드 목록의 **아래** 것이 실리고 위 것은 DUPLICATE 로 표시됩니다. `dependencies` 는 시작 순서일 뿐 라이브러리 격리가 아닙니다.
- `plugins\lib\*.jar`(앱 것 + 켜진 모드 것)는 클래스패스에 오릅니다 — fat jar 는 필요 없습니다. 코어 jar 가 항상 앞이라 코어 클래스는 가릴 수 없습니다.

<a id="context"></a>

## 3. PluginContext

이름 뒤의 「· 실험」 칩은 §8 의 **실험** 항목입니다 — `api_version` 을 올리지 않고 바뀌거나 없어질 수 있습니다. 표시가 없는 항목은 **안정**입니다. 같은 목록이 `sdk.json` 의 `preview` 에 있습니다.

| 메서드 | 하는 일 |
|---|---|
| `id()`, `descriptor()`, `log()` | 자기 id·`plugin.json`·로거(앱 로그 파일에 `plugin.<id>` 로 남습니다) |
| `dataDir()` | `plugindata\<id>\` — 키·기억 같은 제 데이터는 여기에. 제 창에서 스스로 지웁니다 |

### 3.1 말풍선과 선택지

| 메서드 | 하는 일 |
|---|---|
| `say(text, ms)`, `sayFor(imageSet, text, ms)` | 말풍선. `ms <= 0` = 기본 길이 |
| `hideBubble()`, `showBusy(imageSet)` | 말풍선 내리기 · 「···」 생각 중 표시(최대 90초, 다음 말이 덮습니다) |
| `sayTo(mascotId, imageSet, text, ms)`, `showBusyFor(mascotId, imageSet)` | **그 아이 하나**에게 — 같은 셋의 루미가 둘일 때 `sayFor` 는 첫째에게 갑니다. `mascotId` 는 `MascotState.id`(`click`·`doubleclick` 이벤트의 `mascotId`); 그 아이가 사라졌으면 셋의 첫째로 물러납니다 |
| `sayQueued(text, ms)`, `sayQueuedFor(imageSet, text, ms)` · 실험 | **큐**: 지금 말이 내려간 뒤(틈 250 ms) 차례로 뜹니다 — 순서 있는 안내용. `say` 는 즉시 덮되 큐는 남깁니다. 상한 20 줄(넘치면 가장 오래된 줄이 빠집니다). 줄마다 `bubble.text`·`bubble.duration` 훅과 `bubble` 이벤트가 그대로 지나갑니다. 선택지가 떠 있는 동안 큐는 기다립니다 |
| `clearQueue()`, `queueLength()` · 실험 | 이 플러그인의 대기 줄 지우기(떠 있는 말은 `hideBubble`; 줄에서 **기다리는** 선택지도 `-2` 로 빠집니다) · 전체 대기 줄 수 |
| `ask(text, options, timeoutMs, onPick)`, `ask(text, options, timeoutMs, maxWaitMs, onPick)` | **선택지 말풍선**: 물음 + 단추 2~4개(더 주면 잘립니다). `onPick` 은 **한 번**, EDT 아닌 전용 스레드에서 불립니다: 보기 번호(0부터) · `-1` = 떴는데 **사람이 답하지 않았다**(시간 초과·Esc·캐릭터 사라짐) · `-2` = **앱이 치웠다**, 사람에게 닿지 않은 채(이 플러그인의 새 `ask` 가 뜬 것이든 기다리는 것이든 대체함 · `clearQueue()` · 줄에서 `maxWaitMs` 를 넘겨 기다림 · 줄이 참). **시계 둘**: `timeoutMs` 는 말풍선이 **뜬 순간부터**(줄에서 기다린 시간은 세지 않습니다), `maxWaitMs` 는 `ask` 를 부른 순간부터(4-인자 꼴은 `DEFAULT_ASK_WAIT_MS` = 30 초; `<= 0` 도 그 값). 한 번에 하나만 뜨고 나머지는 **줄을 섭니다**: 다른 플러그인의 `ask` 는 차례를 기다리고 뜬 것이 닫히면 다음이 뜹니다; 한 플러그인은 줄에 자리 하나 — 제 새 `ask` 는 제 옛 표(뜬 것이든 기다리는 것이든)를 대체하고(`-2`), 남의 표는 밀어내지 않습니다. 물음도 `bubble.text`(`source=choice`)를 지납니다. 뜰 때 `bubble`(`kind=choice`), 답할 때 `choice` 이벤트가 납니다. `timeoutMs <= 0` = 사람이 고를 때까지. 뜰 때 자판 초점을 뺏지 않습니다(Esc 는 한 번 누른 뒤부터). 해제된 컨텍스트(§2)의 `ask` 만은 예외로 `onPick(-2)` 를 **부른 스레드에서 즉시** 부릅니다. §13 예 |
| `askFor(imageSet, text, options, timeoutMs, onPick)`, `askFor(imageSet, …, maxWaitMs, onPick)` · 실험 | 같은 규칙으로 그 셋의 첫째에게 묻습니다 |

### 3.2 행동

| 메서드 | 하는 일 |
|---|---|
| `behave(name)`, `behaveFor(imageSet, name)` | 행동 이름으로 강제(`behave.txt` 와 같음). 모르는 이름은 경고만 남깁니다 |
| `currentBehavior()`, `currentBehavior(imageSet)` | 지금 행동 이름 |
| `behaviorNames(imageSet)` → `Set<String>` · 실험 | 그 셋의 `behaviors.xml` 이 아는 행동 이름 전부(`behave` 가 받는 이름). 안 실린 셋은 빈 집합. 메뉴 자동 생성용 |

### 3.3 상태 조회

전부 읽기입니다. 엔진이 뜨기 전에는 빈 값·기본값을 돌려주고 예외를 던지지 않습니다.

| 메서드 | 하는 일 |
|---|---|
| `mascots()` → `List<MascotState>` | 지금 떠 있는 캐릭터 전부(소품 제외). `MascotState` = `id`·`imageSet`·`bounds`·`anchor`(발끝)·`lookRight`·`behavior` — **바탕화면 좌표**(Swing 창을 놓는 그 공간), 그 순간의 사본 |
| `mascotsIncludingProps()` · 실험 | `mascots()` 에 소품(선물 때의 크레딧 아이템 등)까지 |
| `mascot(imageSet)`, `mascotById(id)` | 하나 집기 — 그 셋의 첫째 · 그 id(`click`/`mascot` 이벤트의 `mascotId` 와 같은 값). 없으면 `null` |
| `mascotAt(point)` · 실험 | 그 바탕화면 점을 그림이 덮고 있는 아이. 없으면 `null` |
| `affectionTier()`, `affectionProgressPercent()`, `affectionPointsUntilNextTier()`, `daysTogether()` · 실험 | 호감도 단계(1~6)·단계 안 진행률(%)·다음 단계까지 점수·함께한 날(누적) |
| `affectionTodayRemaining(source)` · 실험 | 그 적립원이 오늘 더 벌 수 있는 점수. 앱 적립원은 `pat` `throwRecover` `climb` `social` `pomodoro` `alarm`; `null`/빈 값 = **이 플러그인 제 몫**(`awardAffection`) |
| `focusActive()`, `focusSources()`, `screenBusy()` · 실험 | 집중 모드 켜짐 여부 · 어느 길로(`manual`,`pomodoro`,`auto` 쉼표 나열; 없으면 `""`) · 전체화면·회의가 앞에 있는가 |
| `bubbleVisible()`, `charactersHidden()` | 말풍선이 떠 있는가 · 캐릭터가 숨어 있는가(보스키·수동·자동 숨김) |
| `featureEnabled(id)` | 설정 창의 기능 스위치 — `bongo`·`broadcast`·`island`·`game`·`focus.auto`(CATALOG §7). 안 켰거나 모르는 id 면 거짓. 읽기만 — 스위치는 사용자의 것입니다 |
| `language()` | 앱 UI 언어 — `ko` `en` `ja` `zh-CN` `zh-TW` 가운데 하나. 제 번들은 이것으로 고릅니다. `start()` 시점부터 사용자가 고른 언어(설정 파일의 `Language`)를 돌려주고, 첫 실행이면 OS 언어에서 고른 값입니다 |
| `installedImageSets()`, `activeImageSets()` · 실험 | `img\` 에 설치된 캐릭터 폴더(소품 제외, 이름순) · 지금 켜진 셋(`ActiveShimeji`, 소품 포함) |
| `screens()` → `List<Rectangle>` · 실험 | 모니터 사각형들, `MascotState.bounds` 와 같은 좌표 |
| `quietNow()` | **지금은 조용히 할 때인가** — 숨김·전체화면 앱이 앞·집중 모드에 `focus.quiet` 훅의 답까지. 앱의 혼잣말·인사·축하가 전부 이 하나를 묻습니다; 스스로 말을 거는 플러그인도 이것을 물으세요 |
| `foregroundScreen()` → `Rectangle` 또는 null | **앞 창이 놓인 모니터**의 AWT 경계(`Robot` 좌표). 앞 창이 없거나 모니터에 안 걸치면 null. `screens()` 로는 모릅니다 — 어느 창에서 일하는지는 앱만 압니다 |
| `isCharacter(imageSet)` | 소품(크레딧 아이템 등)이 아닌 캐릭터인가 — 캐릭터 메뉴 줄의 `forImageSet` 에 그대로 넣을 수 있습니다 |
| `persona(imageSet)` → `String`, `personaFile(imageSet)` → `Path` | 캐릭터의 **페르소나 글**(정체성·말투·금기) — `img\<Set>\persona.txt`, 루미는 앱의 예비 사본으로 물러납니다. 없으면 빈 글(대역은 직접 쓰세요). 출력 형식 규칙은 없습니다 — 읽는 쪽이 제 것을 덧붙입니다. 파일 자리는 설정 창이 편집하게 하려는 것입니다 |

### 3.4 캐릭터 제어

전부 앱이 이미 하는 일의 문입니다 — 새 규칙은 없습니다.

| 메서드 | 하는 일 |
|---|---|
| `summon(imageSet)` → int · 실험 | 하나 더 부르기(트레이의 그것, 이로치 뽑기 포함). **켜진 셋만**; 아니면 -1. 태어난 아이의 id 를 돌려줍니다 |
| `dismiss(mascotId)` → boolean · 실험 | 그 아이 보내기(우클릭 「보내기」, 소리 없이). id 가 없으면 false |
| `moveTo(mascotId, x, y)` → boolean · 실험 | 발끝을 바탕화면 좌표로 순간이동. 먼저 `Fall` 로 갈아 끼우고 옮기므로 아래 바닥에 내려앉습니다. 못 갈아 끼우면 false(아무것도 안 움직임, 다음 틱에 다시) |
| `setCharactersVisible(boolean)` · 실험 | 전부 숨기기/되돌리기 — 보스키의 그 스위치(뒤의 보스키·자동 숨김과 그대로 맞물립니다). EDT |
| `awardAffection(source, points)` → int · 실험 | 호감도 직접 적립(선물·퀘스트). 앱과 **같은 문**을 지납니다: `affection.award` 훅이 먼저 보고, **플러그인 하나당 하루 2점** 상한(`source` 를 여럿 지어도 하나로 묶임), 절대 빼지 않습니다. 돌려주는 값 = 실제로 적립된 점수 |
| `heart(imageSet)` · 실험 | 쓰다듬기 하트를 그 아이 머리 위에 |
| `playSound(path)` → boolean · 실험 | 효과음 한 번(앱 플레이어 — 사용자 음량·하나씩 끈 목록·`sound.effect` 훅을 다 지납니다). 경로는 저장소 뿌리 기준(`img/Lumi/sound/jump.wav`) 또는 모드 폴더 안 파일. 소리가 꺼져 있으면 false |

### 3.5 목소리

앱의 음성 줄 하나를 모두가 나눠 씁니다.

| 메서드 | 하는 일 |
|---|---|
| `speak(PcmAudio, onStart, onEnd)`, `stopSpeaking()`, `isSpeaking()` | PCM 재생. 새 `speak` 는 앞 것을 끊고(보이스팩과 겹치지 않게), 음량은 공유 손잡이(`setSpeechVolume`)를 따르고, `tts` 이벤트가 앞뒤로 납니다. `onStart`/`onEnd` 는 재생기 스레드 |
| `setSpeechVolume(percent)` | **공유** 음성 음량(0–100) — 모든 플러그인과 보이스팩이 같은 손잡이. 사용자가 보는 설정에서 미세요 |
| `mouthFlap(imageSet, whileSpeaking)`, `stopMouthFlap()` | **입 뻐끔** — `whileSpeaking` 이 참인 동안(1.1초마다 `Talk` 행동을 다시 겁니다) 입을 움직이다 스스로 멎습니다. `speak` 와 짝: `ctx.mouthFlap(set, ctx::isSpeaking)` |
| `mouthFlapFor(imageSet, ms)` · 실험 | 정한 시간만큼 입 뻐끔 — 소리를 직접 틀지 않는 줄에 |

### 3.6 이벤트와 훅

| 메서드 | 하는 일 |
|---|---|
| `publish(topic, k, v, …)` | **플러그인끼리** 이벤트. 토픽은 `<id>.<topic>` 으로 강제되어(이미 붙어 있으면 그대로) 앱 토픽을 가장할 수 없습니다. payload 에 `from`=내 id 가 붙습니다. 받는 쪽은 `on("<그 id>.<topic>", …)` |
| `on(topic, listener)`, `off(...)` | 이벤트(§4). 리스너는 **이 플러그인 전용 스레드**(`lumi-plugin-events-<id>`)에서 차례로 돕니다 — 느린 플러그인이 남의 이벤트를 늦추지 않습니다. 던지면 로그만 남고 다음 이벤트는 옵니다 |
| `hook(id, handler)`, `unhook(...)` | 훅(§5). 핸들러는 **부른 스레드에서 그 자리**에서 돕니다 |

### 3.7 설정·문구·파일

| 메서드 | 하는 일 |
|---|---|
| `prefs()` → `PluginPrefs` | **제 설정 저장소** — `plugindata\<id>\settings.properties`(UTF-8 평문 — 비밀은 넣지 마세요, 그건 `writeAtomic(…, true)`). `get`·`getInt`·`getLong`·`getBoolean(key, 기본)`, `set(key, 문자열/int/long/boolean)`(같은 값이면 무시, 바뀌면 그 자리에서 동기 저장 — 저장 실패는 WARNING 로그로 남고 던지지 않습니다; 원자적 이동이 안 되는 파일 시스템에서는 일반 교체), `remove`, `keys()`, `has`, `onChange((key, value) -> …)`(값이 실제로 바뀐 뒤, **모든 플러그인이 나눠 쓰는 알림 스레드 하나**(`lumi-plugin-prefs`, EDT 밖)에서 차례로; 지운 열쇠는 `null`), `removeOnChange(listener)`, `reload()`. **`onChange` 리스너는 컨텍스트가 거두지 않습니다** — `stop()` 에서 직접 푸세요(§2). `plugin.json` `settings` 카드의 값도 여기에 삽니다 |
| `setting(key, fallback)`, `setSetting(key, value)` | 앱의 `conf\settings.properties`. **읽기**용 — 앱 키는 앱 것이라 바꾸지 마세요(설정 창의 다음 [적용]이 덮을 수 있습니다). 제 상태는 `prefs()` 에. `setSetting` 을 꼭 써야 하면 키에 제 id 를 접두로 붙이세요 |
| `localized(key, fallback)` | 앱의 5개 언어 번들. 제 문구는 제 번들에 두세요 |
| `text(key, fallback)`, `text(key, fallback, args…)` → String · `text()` → `PluginText` | **제 5개 언어 문구**: jar 뿌리의 `plugin_ko.properties` `plugin_en` `plugin_ja` `plugin_zh` `plugin_zh_TW` + 기본 `plugin.properties`(UTF-8). `language()` 로 고르고 `정확한 언어 → en → 기본 파일 → 앱 번들 → fallback` 순(zh-TW 는 zh 로 떨어지지 않습니다). `args` 가 있으면 `MessageFormat`(`{0}`) |
| `writeAtomic(path, bytes 또는 String, ownerOnly)` | 같은 폴더의 임시 파일에 쓴 뒤 원자적 이동으로 바꿔 넣습니다(원자적 이동이 안 되는 파일 시스템에서는 일반 교체). 쓰기·이동 실패는 `IOException`. `ownerOnly` 면 이 윈도우 계정과 SYSTEM 만 읽게 ACL 을 **임시 파일에 걸고 나서** 이름을 바꿉니다; ACL 을 못 걸면(윈도우 밖·FAT·거부하는 공유) FINE 로그 한 줄만 남기고 파일은 그대로 씁니다 — 암호화가 아닙니다. 경로는 `dataDir` 밖이어도 됩니다. API 키·개인적인 것은 이것으로 |

### 3.8 메뉴와 설정 UI

| 메서드 | 하는 일 |
|---|---|
| `addTrayItem(label, action)` → `PluginUi.MenuHandle` | 트레이 메뉴 줄(앱 줄 아래·「종료」 위). `action` 은 EDT. 돌려주는 **손잡이**로 `setLabel`·`setEnabled`·`setChecked(Boolean)`·`remove()`, 읽기 `label()`·`enabled()`·`checked()`·`removed()` — 줄이 안 바뀌면 무시해도 됩니다. 바뀐 것은 다음에 메뉴가 열릴 때 보입니다(메뉴는 열 때마다 새로 짓습니다). 어느 스레드에서든. **이름은 문자열이라 언어가 바뀌어도 스스로 안 바뀝니다** — `settings` 이벤트의 `phase=language` 를 듣고 `setLabel(text(...))` 로 다시 놓으세요(LUMI Chat·Super Bongo 가 그렇게 합니다) |
| `addTrayToggle(label, initial, onChange)` → `MenuHandle` · 실험 | **토글 줄** — 이름은 그대로, 「켜짐/꺼짐」 상태 칩(앱의 「잠깐 숨기기」 문법). 누르면 스스로 뒤집고 `onChange(새 값)` 을 EDT 에서 부릅니다; `setChecked` 는 칩만 바꾸고 `onChange` 를 부르지 않습니다 |
| `addSettingsButton(label, action)` → `MenuHandle` | 설정 창 정보 탭의 사용법 단추 가까이에 단추 하나. `action` 은 EDT. 앱은 설정 창을 열 때마다 등록 목록을 다시 읽으므로 손잡이의 이름·활성·제거는 다음에 창을 열 때 반영됩니다 |
| `addFeatureToggle(label, description, state, onChange, dependsOnFeature)` → `PluginUi.FeatureToggle` | 루미 탭 「기능 켜고 끄기」 카드의 **맨 아래**에 앱 모양의 스위치 줄. 등록이 0개면 줄·여백도 없습니다. 창을 열 때 `state` 로 체크를 채우고, 체크가 바뀌었을 때만 [적용] 때 `onChange(새 값)` 으로 건넵니다. false 를 돌려주면 `state` 로 되돌립니다. `dependsOnFeature`(`bongo` 등)가 꺼지면 회색이 되지만 체크는 보존됩니다. 하네스 `press:feature:<라벨>` |
| `addCharacterMenuItem(label, forImageSet, action)` → `MenuHandle` | 캐릭터 우클릭 메뉴 줄. `forImageSet` 이 캐릭터마다 보일지 정합니다(null = 모두), `action(imageSet, mascotId)`. 손잡이는 트레이 줄과 같습니다 |
| `addCharacterMenuToggle(label, forImageSet, initial, onChange)` → `MenuHandle` · 실험 | 캐릭터 메뉴의 토글 줄. 토글 상태는 줄 하나에 하나(캐릭터마다가 아님). `onChange(새 값)` 은 EDT |
| `claimClickGreeting()` | 클릭 인사를 이 플러그인이 맡는다고 알립니다(앱의 클릭 인사가 조용해집니다) |

### 3.9 창의 옷

플러그인 창을 앱처럼 보이게 합니다.

| 메서드 | 하는 일 |
|---|---|
| `theme()` → `PluginTheme` | 색(`tone(다크값)`·`inkSoft`·`inkFaint`·`line`·`cardBackground`·`danger`·`success`·`dark()`), CJK 글꼴 `font(text, size)`, 걸어 두는 손질(`colour`·`dress`·`style`·`button(kind)`·`toggle`·`slider`·`describe`), 조각(`sectionTitle`·`hairline`·`row`·`card`·`gapRow`·`hintHtml`·`cardGap`). 색은 **다크 시안 값**으로 말하면 지금 테마로 옮겨 주고, 테마가 갈리면 걸어 둔 손질이 다시 돕니다. 전부 EDT. `style(component, 문자열)` 의 문자열은 룩앤필(FlatLaf)의 것이라 앱의 옷이 바뀌면 이 호출만 빈 일이 될 수 있습니다 — 되도록 뜻이 있는 메서드(`colour`·`button`·`card` …)를 쓰세요 |
| `appIcon()` → Image | 앱 아이콘 — 주인 없는 대화상자의 제목줄에 |
| `manageWindow(window, key)` | 사용자가 마지막에 둔 자리에 창을 놓고(처음엔 활성 모니터 가운데, 화면 밖은 안 됨) 닫을 때 기억합니다. 크기를 정한 뒤, 열 때마다 불러도 됩니다(멱등) |

### 3.10 단축키

| 메서드 | 하는 일 |
|---|---|
| `bindHotkey(name, "Win+Ctrl+Alt+Shift+키", action)` → `HotkeyResult` | **전역** 단축키 — 다른 프로그램이 앞에 있어도 듣습니다. 수식 키 하나 이상 필수. 빈 조합 = 해제. 플러그인 id 는 **등록 이름**에만 붙습니다 — 이름은 서로 안 부딪히지만 실제 키 조합은 다른 프로그램·플러그인과 부딪힐 수 있습니다(그때 `REFUSED`). 답은 **왜** 안 됐는지입니다: `OK`·`NONE`·`BAD_COMBINATION`·`REFUSED`(다른 프로그램이 쥐고 있음)·`UNSUPPORTED` — 설정 창이 사용자의 말로 옮깁니다. `action` 은 EDT |
| `unbindHotkey(name)` | 해제 |
| `normalizeHotkey(s)`, `hotkeyFromKeyEvent(keyCode, modifiersEx)` | 조합 글자의 정본 꼴(`Ctrl+Shift+L`), 아니면 null · 단축키 칸에서 눌린 키를 조합 글자로(맨 키·수식 키만이면 null) |

### 3.11 예약 작업과 바이트코드

| 메서드 | 하는 일 |
|---|---|
| `every(initialMs, periodMs, task)`, `after(ms, task)` → `ScheduledHandle` · 실험 | **스케줄러**: **이 플러그인의 타이머 스레드**(`lumi-plugin-timer-<id>`, 데몬, EDT 아님)에서 주기/한 번 실행. 플러그인마다 이벤트 스레드 하나·타이머 스레드 하나 — 느린 작업은 제 플러그인의 다음 작업만 늦추고 남의 것은 안 밀립니다. 던지는 실행은 로그만 남고 주기는 계속됩니다. `cancel()`·`isCancelled()`; 종료 때 남은 것은 앱이 끊고 스레드도 내립니다. `task == null` 은 둘 다, `periodMs <= 0` 은 `every()` 만 `IllegalArgumentException` — 해제 여부와 무관하게(`after(ms, …)` 의 `ms` 는 0 이하도 받습니다) |
| `onEdt(task)`, `offEdt(task)` · 실험 | Swing 이벤트 스레드에서(창·메뉴를 만질 때 — 절대 오래 잡지 마세요) · 이 플러그인의 이벤트 스레드에서(리스너와 같은 줄, EDT 밖) |
| `onLoad(prefix, LumiTransformer)` → boolean | 아직 안 실린 클래스(엔진·마스코트)를 로드 시점에 자유롭게 고칩니다(§6) |
| `patch(className, ClassFileTransformer)` → boolean | 이미 실린 클래스 하나를 재변환합니다(메서드 본문만 — JVM 규칙)(§6) |

<a id="events"></a>

## 4. 이벤트 — `ctx.on(topic, map -> …)`

값은 따로 적지 않으면 문자열입니다. 리스너는 발행 스레드가 아니라 그 플러그인 전용 데몬 스레드에서 차례로 돕니다 — 발행 쪽(EDT·엔진 잠금 안)을 막을 수 없습니다. 던지면 로그 뒤 건너뜁니다. 「· 실험」 칩은 §3 첫머리와 같은 뜻입니다.

| 토픽 | 언제 | 키 |
|---|---|---|
| `ready` | 엔진이 뜨고 플러그인들이 시작된 뒤 | 없음 |
| `behavior` | 마스코트의 행동이 바뀜 | `imageSet`, `name`, `mascotId`(Integer) |
| `bubble` | 말풍선이 뜸 | `imageSet`, `mascotId`(Integer; 셋으로 지정했으면 0), `text`(`bubble.text` 훅을 지난 뒤 실제로 뜬 글자), `ms`(Long; `bubble.duration` 뒤의 최종 수명), `kind`(`say` = 보통 줄 · `private` = 알람·부탁처럼 방송 화면에서 빼는 줄 · `choice` = 선택지 말풍선이 열림, 그때 `ms` 는 제한 시간). **`kind=choice` 에는 `mascotId` 가 없습니다**(`imageSet`·`text`·`ms`·`kind` 만). 「···」 생각 중 표시는 이벤트를 내지 않습니다 |
| `choice` · 실험 | 선택지 말풍선에 답이 남 | `imageSet`, `mascotId`(Integer), `index`(Integer, 0부터 · `-1` = 떴는데 답 없음(시간 초과·Esc·캐릭터 사라짐) · `-2` = 앱이 치움(같은 플러그인이 대체·`clearQueue`·줄 대기 초과·줄 참)), `option`(고른 글자, 음수면 `""`), `from`(물은 플러그인 id) |
| `click` | 캐릭터 위에서 마우스 단추가 눌림 | `imageSet`, `mascotId`(Integer), `button`(`left`/`right`/`middle`), `x`, `y`(Integer, 화면 좌표) |
| `mascot` | 캐릭터가 나타나거나 사라짐 | `imageSet`, `mascotId`(Integer), `event`(`added`/`removed`) |
| `headpat` | 쓰다듬기 시작 | `behavior` |
| `affection` | 보이는 호감도 단계가 바뀜 | `tier`(Integer), `previous`(Integer), `points`(Integer) |
| `achievement` | 스팀 도전과제 해금 | `id` |
| `focus` | 집중 모드 켜짐/풀림 | `active`(Boolean) |
| `chat` | AI 채팅 오감(LUMI Chat 플러그인이 발행합니다 — 그 플러그인이 없으면 오지 않습니다) | `phase`(`ask`/`reply`), `imageSet`, `text`. 다른 플러그인의 `ctx.publish("chat", …)` 은 `<그 id>.chat` 으로 나가므로 앱 토픽 `chat` 을 가장할 수 없습니다 |
| `tts` | 음성 재생 시작/끝 | `phase`(`start`/`end`) |
| `settings` | 설정 창 [적용], 또는 언어 변경 | `phase`(`apply`), `changed`(지난 [적용] 이후 앱이 `LumiSettings` 로 적은 키를 쉼표로 — 스톡 엔진 키는 안 실립니다) / `phase`(`language`), `language`(BCP-47) |
| `ask` | `speech\ask.txt` 가 쓰임 | `text` |
| `settings_open` | `speech\settings_open.txt` 가 쓰임 | 없음 |
| `say_file` | `speech\say.txt` 줄이 표시됨 | `text` |
| `drag` · 실험 | 사용자가 캐릭터를 집어 올림·놓음(`Dragged` 시작·끝) | `phase`(`start`/`end`), `imageSet`, `mascotId`(Integer), `x`, `y`(Integer, 앵커의 화면 좌표) |
| `throw` · 실험 | 놓으며 던져짐(`Thrown` 시작) · 그 비행이 끝남(다음 행동 시작) | `phase`(`thrown`/`landed`), `imageSet`, `mascotId`(Integer), `x`, `y`(Integer), `next`(항상 실립니다 — `landed` 면 비행 뒤 행동 이름, `thrown` 이면 `""`) |
| `hover` · 실험 | 포인터가 캐릭터 창에 들어옴·나감 | `phase`(`enter`/`exit`), `imageSet`, `mascotId`(Integer) |
| `doubleclick` · 실험 | 같은 캐릭터 위 왼쪽 누름 둘이 600 ms 안에, 가로·세로 각각 90 px 안에서(반지름이 아니라 180×180 상자입니다. AWT clickCount 는 믿을 수 없습니다 — 누르는 순간 들기가 창을 옮깁니다) | `imageSet`, `mascotId`(Integer), `x`, `y`(Integer). 그 둘은 `click` 도 각각 냅니다 |
| `alarm` · 실험 | 알림이 실제로 전달됨 | `kind`(`hourly`/`alarm`), `time`(`HH:mm`), `late`(Boolean — 화면이 바빠 미뤄졌다 나온 알람) |
| `pomodoro` · 실험 | 집중/휴식 주기가 움직임 | `phase`(`focus`/`break`/`stopped`), `minutes`(Integer, 시작하는 국면의 길이; stop 은 0), `round`(Integer, 켠 뒤 시작한 집중 국면 수) |
| `bongo` · 실험 | 봉고 모드가 타건·클릭을 그림 | `kind`(`keyboard`/`left_click`/`right_click`), `frame`(Integer, 그린 손 자리), `look`(`bongo`/`small`). **키 코드·글자는 절대 없습니다**(코어는 키를 읽지 않습니다) |
| `app` · 실험 | 앱이 제 종료 문으로 내려감 | `phase`(`exit`). 플러그인 `stop()` 보다 앞이고, 짧은 상한을 두고 전달을 기다린 뒤 내려갑니다 |
| `island` · 실험 | 정원(섬) 프로세스 시작·정지 요청, 사진 요청 응답 | `phase`(`start`/`stop`/`photo`), `request`(photo 만) |
| `game` · 실험 | 장치 게임 판이 열림·달리기 시작·닫힘 | `phase`(`start`/`run`/`stop`) |

`mascotId` 는 실행 중에만 뜻이 있는 번호입니다(같은 캐릭터 = 같은 번호). `imageSet` 은 `img\<폴더>` 이름입니다. 위치 틱 이벤트는 없습니다 — 위치는 `ctx.mascots()` 를 폴링하세요. 없는 것: 절전·복귀, 모니터 구성 변경(앱이 듣지 않습니다).

페이로드 예(값은 `Map<String,Object>` 의 실제 형):

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
app         {phase=exit}
```

<a id="hooks"></a>

## 5. 훅 — `ctx.hook(id, (hook, current, ctx) -> …)`

코어가 **정하기 전에** 묻습니다. 핸들러는 지금까지의 값 `current` 를 받아 새 값을 돌려주거나 `null`(통과)을 돌려줍니다.

규약:

- **동기** — 부른 스레드에서 그 자리에서 돕니다. EDT·타이머·엔진 틱 위일 수 있으니 값싸게, 절대 블록하지 마세요. 20 ms 를 넘기면 훅+플러그인당 한 번 경고합니다.
- **순서** — 모드 탭 순서로 위→아래, **아래가 이깁니다**. 앞선 답을 존중하고 싶으면 `current` 를 보고 스스로 AND 하세요.
- **형** — 기본값과 같은 형이어야 합니다(숫자는 `Number`). 아니면 통과 + 한 번 경고. 숫자 훅은 코어가 범위를 고정합니다(표의 clamp).
- **예외** — 전부 잡습니다. 그 핸들러만 건너뛰고 체인은 계속됩니다. 아무도 답하지 않으면 기본값입니다. 등록된 핸들러가 없는 훅은 ctx Map 도 만들지 않고 기본값을 즉시 돌려줍니다.
- **빈 문자열** — 뜻은 훅마다 다릅니다(표의 「반환·기본」): `selftalk.line`·`speech.greeting` 은 `""` = 건너뜀, `bubble.text` 는 공백뿐인 답·null = 원문 그대로.
- **사용자 약속** — 사용자가 건 약속(알람, 사용자가 누른 부탁, `say.txt`)은 훅으로는 막을 수 없습니다 — 그런 자리에는 취소 훅이 없고 미루기 훅만 있습니다. 앱이 스스로 고르는 것(혼잣말·인사)은 `""` 로 건너뛸 수 있습니다.
- `ctx` 는 불변 Map 이고 null 은 `""` 입니다. 캐릭터가 있는 자리는 `mascotId`(Integer)·`imageSet`(String). 마스코트 객체는 넘기지 않습니다.

카탈로그 (15):

| 훅 | 묻는 자리 | ctx | 반환·기본 | 주의 |
|---|---|---|---|---|
| `focus.quiet` | 시보·축하·혼잣말·인사·온보딩이 「조용히 할까」를 묻는 한 자리 | `hidden`, `fullscreenAhead`, `focusActive` | boolean, 기본 = 화면이 바쁨 OR 집중 중 | false 고정 = 화면 공유 중 말풍선. 초당 여러 번 → 상수 시간 |
| `focus.screen_busy` | 알람 보류·포모도로·집중 인사가 「화면이 바쁜가」 | `hidden`, `fullscreenAhead` | boolean, 기본 = 숨김 OR 전체 화면 앞 | true 고정 = 보류 알람이 영영 안 나옵니다 |
| `text.localized` | 번들 글자 전부가 지나는 문(인사·시보·알람·단계 이름·메뉴 글자) | `key`, `language`, `fallback` | String, 기본 = 번들 값. `""` 도 그대로 | 자주 불립니다 — 무거우면 UI 가 느려집니다. 인사는 `speech.greeting` |
| `sound.effect` | 효과음 경로, 재생 앞 | `path`, `caller` | String 경로; `""` = 무음. 기본 = `path` | 음량·끈 소리 목록은 훅 뒤에 그대로 적용됩니다. 전체 음소거는 훅보다 **앞**이라 사용자가 소리를 꺼 두었으면 훅으로 되살릴 수 없습니다 |
| `behavior.force` | 감독자·플러그인의 `behave` 가 이름으로 강제하는 문 전부 | `name`, `caller`, `mascotId`(없으면 -1), `imageSet` | String; `""` = 걸지 않음. 기본 = `name` | 엔진의 자율 선택은 묻지 않습니다. 알람·정각 알림의 밀기는 사용자 약속 — `""` 도 원래 이름으로 걸립니다 |
| `bubble.duration` | 말풍선 수명 계산 뒤 | `text`, `requested`(0=자동), `source`(`say`/`private`/`selftalk`), `voiceMs`, `mascotId`, `imageSet` | long ms, 기본 = 계산값. clamp 500..120000 | 즉시 `say` 는 긴 값도 다음 말이 덮습니다(큐 줄은 그 뒤에 뜹니다) |
| `selftalk.line` | 혼잣말 대사가 골라진 뒤 | `candidate`, `onGardenFloor`, `gardenPhase`, `tier`, `offered`, `mascotId`, `imageSet` | String; `""` = 이번 차례 건너뜀 | `offered=true`(해금 알림·친구 소식)를 삼키면 그 알림이 사라집니다 |
| `selftalk.interval` | 다음 혼잣말까지 | `baseMs`, `frequency`(0-3), `minSec`, `maxSec` | long ms, 기본 = 계산값. clamp 3000..3600000 | |
| `speech.greeting` | 클릭·수다 인사 | `pool`(List), `last`, `trigger`(`click`/`chatter`), `imageSet` | String; `""` = 인사 없음. 기본 = 풀에서 무작위 | 풀이 비어도 묻습니다 — 인사말 없는 캐릭터에 인사를 주는 길 |
| `affection.award` | 호감도 적립원 전부가 지나는 한 자리 | `source`, `points`, `dailyCap`, `cooldownSeconds`, `todayPoints` | int; 0 = 이번 건 안 줌(쿨다운도 안 겁니다). 기본 = `points`. clamp 0..1000000 | 음수 → 0(절대 빼지 않습니다). 일일 상한은 그대로 |
| `affection.tier` | 점수 → 단계 | `points` | int 1..6, 기본 = 표 | 내려가는 값은 무시됩니다(올릴 때만) |
| `place.allowed` | 감독자가 캐릭터를 옮겨 놓기 전에 「여기 놓아도 되는가」 | `x`, `y`, `confinement`(int[4] 또는 `""`), `mover` | boolean, 기본 = 울타리 안 | 100 ms 타이머에서도 불립니다 → 값싸야 합니다. false 고정이면 테더가 되돌리지 못합니다 |
| `bongo.key_frame` | 봉고 모드에서 키 그림 자리를 고른 직후 | `frames`, `cycle`(코어가 고른 자리), `look`(`bongo`/`small`) | int 자리, 기본 = 돌아가며. clamp -1..frames-1 | ctx 에 키 코드·글자는 없습니다(코어는 키를 읽지 않습니다). 매 타건, EDT |
| `bongo.key_input` | 봉고가 보이는 동안 40 ms 마다, 마우스 추정과 독립 | `frames`, `look` | 한 번 소비할 그림 자리; -1 = 플러그인이 입력을 맡되 이번엔 없음, -2/null = 코어 추정 사용. 기본 -2, clamp -2..frames-1 | 입력 폴러 스레드: 기다리지 마세요. 확인한 키를 한 번만 소비하세요. 키 내용은 코어에도 공용 이벤트 버스에도 가지 않습니다 |
| `bubble.text` · 실험 | 말풍선 글자가 오르기 직전의 한 문 — `say`·`sayFor`·`sayPrivate`·`say.txt`·인사·다른 플러그인의 `say`, 그리고 혼잣말·선택지 물음 | `text`, `source`(`say`/`private`/`selftalk`/`choice`), `mascotId`, `imageSet` | String, 기본 = 원문. **`""`·공백뿐인 답·null = 원문 그대로(치환만, 삼키기 없음)** | 이 문에는 알람·`say.txt` 같은 사용자 약속이 섞여 지나므로 건너뛰기가 없습니다 — 앱이 고른 줄을 빼려면 `selftalk.line`/`speech.greeting`. `bubble` 이벤트의 `text` 는 훅을 지난 뒤의 글자입니다 |

<a id="bytecode"></a>

## 6. 바이트코드 — 마지막 수단

훅·이벤트로 되는 것은 그쪽으로 하세요. 앱 업데이트로 그 메서드가 바뀌면 깨지는 것은 변환기뿐입니다(§8 약속 밖).

| 길 | 언제 등록 | 할 수 있는 것 |
|---|---|---|
| `LumiTransformer` 서비스 | 게이트 직후, 앱이 실리기 전(`attach`) | 그 뒤 실리는 **모든** 클래스를 통째로(메서드·필드 추가 포함) |
| `ctx.onLoad(prefix, t)` | `start()` 에서 | 아직 안 실린 클래스(엔진·마스코트는 `start` 뒤에 실립니다)를 같은 자유도로 |
| `ctx.patch(className, t)` | 언제든 | 이미 실린 클래스 하나를 재변환 — JVM 규칙상 메서드 본문만 |

- 보호 목록은 아예 건네지지 않습니다: `lumi.SteamGate`, `lumi.OfflineToken`, `lumi.Features`, `lumi.plugin.*`, `lumi.steam.*`, `lumi.AffectionStore`, `lumi.Achievements`(호감도 저장소와 도전과제 — 스팀 도전과제 부정 방지. 점수를 주려면 `awardAffection`).
- 던지는 변환기는 로그 뒤 원본 그대로 실립니다. 여러 플러그인이 같은 클래스를 건드리면 이어서 적용되는데, 플러그인 jar 이 모드 목록의 역순으로 클래스패스에 붙으므로 **아래 모드가 먼저** 등록되고 먼저 돕니다. 다만 이 차례는 `ServiceLoader` 가 정하는 것이라 계약이 아닙니다 — 순서에 기대는 변환기는 쓰지 마세요.
- 꺼진 플러그인의 변환기는 등록되지 않습니다. `-Dlumi.plugins=off`(런처 `--no-plugins`)면 전부 꺼집니다.
- JDK 25 의 `java.lang.classfile` 로 충분합니다 — `examples\plugin-transformer`.

<a id="data-mods"></a>

## 7. 데이터 조각 (코드 없는 모드)

같은 모드 폴더에 통째 파일 대신 **덧붙이는 조각**을 둘 수 있습니다. 메모리에서 합쳐지고 아래 모드가 이깁니다.

| 자리 | 하는 일 |
|---|---|
| `img\<셋>\conf\actions.d\*.xml`, `behaviors.d\*.xml` | 액션·행동 덧붙이기. 같은 `Name` 은 그 자리에서 교체됩니다. 전역 `conf\actions.d\`·`behaviors.d\` 는 원본을 전역 `conf\` 에서 읽는 셋에만 |
| `conf\language.d\<이름>_<locale>.properties` | 그 로케일의 키 덮기. 접미 없는 파일은 빠진 키만 |
| `conf\self_talk.d\`, `conf\meet_dialogues.d\<이름>_<lang>.txt` | 혼잣말·만남 대사를 원본 뒤에 덧붙임 |
| `mod.json` `{"name","version","app_version"}` | 모드 탭 칩(플러그인이 없는 모드용) |

깨진 조각은 경고 한 줄로 건너뛰고, 합친 결과가 엔진 검증에 걸리면 조각 없이 다시 읽습니다. 자세한 규칙은 [첫걸음 안내](../plugin-guide.html)의 「데이터 조각」에 있습니다.

<a id="compatibility"></a>

## 8. 호환 정책

- 플러그인 API 의 공개 표면은 **안정**과 **실험** 둘로 나뉩니다. 표시가 없는 항목은 안정입니다. 실험 항목은 표에 「실험」 칩으로 표시되고 `sdk.json` 의 `preview` 에 같은 목록이 있습니다.
- **안정 항목은 같은 `api_version` 안에서 더해지기만 합니다.** 없어지지 않고 형이 바뀌지 않습니다. 결정 자리가 앱에서 사라진 훅은 기본값만 돌려주는 빈 자리로 남고 표에 폐기로 표시됩니다. 이벤트에는 키가 더해질 수 있고 빠지지 않습니다 — 모르는 키는 무시하세요.
- **실험 항목은 `api_version` 을 올리지 않고 바뀌거나 없어질 수 있습니다.** 그때는 `CHANGELOG.md` 에 적고, 없앤 자리는 한 앱 버전 동안 빈 자리로 남깁니다. 실험 항목이 앱 버전 하나를 바뀌지 않고 넘기면 안정으로 올립니다. 1.3.0 의 구분 기준: 자사 플러그인(LUMI Chat·Super Bongo)이나 앱 자체가 실제로 쓰는 문과 그 짝은 안정, 그 밖은 실험입니다.
- **`api_version` 은 안정 항목을 없애거나 모양을 바꿔야만 할 때 오릅니다.** 그때 이행 목록을 `CHANGELOG.md` 에 적고, **직전 버전(N-1)을 위해 쓰인 플러그인은 한 버전 더 실립니다**(없앤 훅은 빈 자리, 없앤 메서드는 위임 껍데기로). 두 버전 전 플러그인은 실리지 않고 업데이트 안내를 받습니다. 앱보다 새 `api_version` 을 적은 플러그인도 실리지 않습니다.
- 「더해지기만 한다」는 뒤집으면 나중 앱 버전에서 더해진 메서드·훅·토픽이 같은 `api_version` 의 옛 앱에는 없다는 뜻입니다. 검증한 앱 버전을 `plugin.json` 의 `app_version` 에 적으세요. 실행 중인 앱과 다르면 모드 탭에 「N용」 칩이 붙습니다(안내일 뿐 막지 않습니다).
- 약속 밖: 변환기가 건드리는 코어 클래스의 모양, `PluginContext` 와 이 문서의 타입 표 밖의 공개 클래스(클래스패스에 있어 부를 수는 있지만 버전마다 바뀔 수 있습니다), `conf\settings.properties` 의 앱 키.
- (현재 버전 사실) 1.3.0 의 로더는 `api_version` 을 읽기만 하고 판단하지 않습니다. 위의 거부·칩은 다음 앱 버전부터 로더가 집행합니다.
- SDK 버전은 앱 버전과 같습니다(`sdk.json` 의 `sdk_version` = `app_version`). `api_version` 은 별개의 호환 번호로 이 문서 제목에 적혀 있습니다. 문서·예제만 바뀐 개정본은 버전을 유지하고 파일 이름과 `sdk.json` 의 `commit` 으로 구분합니다.

<a id="practices"></a>

## 9. 하지 말 것

- 이벤트 리스너에서 EDT 를 기다리지 마세요(발행 쪽이 EDT 일 수 있습니다 — `SwingUtilities.invokeLater` 나 `onEdt` 로).
- 훅 핸들러에서 파일·네트워크·잠금을 잡지 마세요.
- 사용자 데이터를 `plugindata\<id>\` 밖에 두지 마세요. 개인정보를 다루면 창작마당 설명에 밝히세요.
- `conf\settings.properties` 의 앱 키를 바꾸지 마세요(제 접두 키만).

<a id="testing"></a>

## 10. 테스트 킷 — `PluginTestKit`

`lib\Shimeji-ee.jar` 는 게이트 때문에 앱을 띄울 수 없지만, 플러그인의 **단위 검사**는 앱 없이 됩니다. `PluginTestKit` 이 주는 `PluginContext` 는 문마다 이 킷으로 이어져 있어 — 플러그인이 한 일은 **기록**되고, 읽는 값은 **가짜**에서 오고, 이벤트·훅·시계는 검사가 직접 밉니다. 엔진·창·스팀은 하나도 안 뜹니다. `tools\run_tests.py <플러그인 폴더>` 가 `test\*.java` 를 굽고 돌립니다(예 `examples\hello\test\HelloPluginTest.java`; 프레임워크 없이 `main` 하나).

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

손대지 않은 hello 예제의 사본에 `test\HelloSmokeTest.java` 로 두고 `python tools\run_tests.py <폴더>` 로 돌리세요. 러너는 `test\` 바로 아래의 `*.java` 만 찾고 클래스마다 JVM 을 따로 띄우며(`-Dplugin.dir=<플러그인 폴더>`), **`-ea` 를 주지 않으므로 `assert` 문은 무효입니다** — 실패는 위처럼 `AssertionError` 를 직접 던지세요. 킷은 플러그인을 만들어 주지도, `ready` 를 내 주지도, `stop()` 을 대신 불러 주지도 않습니다 — 셋 다 검사가 합니다.

| 메서드 | 하는 일 |
|---|---|
| `start(pluginId)` · `start(Path pluginJson)` · `start(PluginDescriptor)` | 킷 하나 = 플러그인 하나. `plugin.json` 을 주면 `"settings"` 기본값이 `prefs()` 에 심어집니다 |
| `context()`, `descriptor()`, `dataDir()` | 건넬 컨텍스트 · 매니페스트 · 임시 폴더(`prefs()` 도 여기, `close()` 가 지웁니다) |
| `calls()`, `calls(name)`, `lastCall(name)`, `clearCalls()` | 기록: `say`·`sayQueued`·`ask`·`behave`·`behaveFor`·`summon`·`dismiss`·`moveTo`·`heart`·`playSound`·`awardAffection`·`setCharactersVisible`·`hideBubble`·`publish`·`addTrayItem`·`addTrayToggle`·`addSettingsButton`·`addCharacterMenuItem`·`addCharacterMenuToggle`·`addFeatureToggle`·`bindHotkey`·`every`·`after`… — `Call.name` + `args`. `lastCall` 은 이름이 필수입니다(무인자 꼴은 없습니다) |
| `queued()`, `menuRows()`, `featureToggles()`, `hotkeys()`, `choiceOpen()`, `choiceOptions()` | 큐에 남은 줄 · 붙인 메뉴 줄(손잡이 살아 있음) · 붙인 기능 스위치 · 등록한 단축키 · 열린 선택지 |
| `mascots(…)`, `affection(tier, %, untilNext, days)`, `affectionRemaining(n)`, `focus(active, sources)`, `screenBusy`, `bubbleVisible`, `charactersHidden`, `language(code)`, `behaviorNames(Set<String>)`, `currentBehavior(name)`, `installedImageSets`, `activeImageSets`, `screens`, `setting(key, value)`, `appText(key, value)`, `soundsEnabled` | 플러그인이 **읽는** 값의 가짜. 기본: 캐릭터 없음·1단계·오늘 몫 2점·`ko`·`Lumi` 하나·1920×1080·소리 켜짐 |
| `feature(id, on)` | `featureEnabled(id)` 의 답(기본 전부 꺼짐) |
| `publish(topic, k, v, …)` | 리스너에게 **동기** 배달(던지는 리스너는 건너뜁니다 — 앱 규칙) |
| `ask(hook, default, k, v, …)` | 훅 핸들러를 앱 규칙대로 돌려 답을 줍니다(null 통과·형 불일치 통과·던지면 건너뜀) |
| `answerChoice(index)`, `choicesWaiting()` | 열린 선택지를 그 번호(또는 -1)로 닫고 콜백을 부른 뒤 줄의 다음 것을 엽니다(앱과 같은 줄서기 — 같은 플러그인의 새 `ask` 는 옛 표를 `-2` 로 대체, `tick()` 이 `maxWaitMs` 를 넘긴 것을 `-2` 로 뺍니다) · 기다리는 선택지 수 |
| `tick(ms)`, `now()` | 가짜 시계 — 기한이 온 `every`/`after` 를 차례로 돌립니다(주기는 맞는 횟수만큼) |
| `textLoader(ClassLoader)` | `text()` 가 `plugin_<lang>.properties` 를 찾을 로더(기본 = 검사의 로더) |
| `quiet(b)`, `foregroundScreen(rect)`, `persona(set, text)`, `speaking(b)` | 가짜값 — `quietNow`·`foregroundScreen`·`persona`·`isSpeaking` 의 답 |
| `speechVolume()`, `mouthFlapping()` | 마지막 `setSpeechVolume` · 지금 입을 움직이는 셋(없으면 null). `speak` 는 기록판에서 즉시 끝납니다(`onStart`·`onEnd` 가 그 자리에서 돕니다) |
| `close()` | 컨텍스트로 등록한 것을 거두고 임시 폴더를 지웁니다(두 번 안전; `try-with-resources`). 플러그인의 `stop()` 은 부르지 않습니다 — `prefs().onChange` 리스너도 앱에서처럼 남습니다 |
| `PluginTestKit.realContext(pluginId)` | **제품 백엔드**의 컨텍스트 — 앱 창 없이 앱의 진짜 상태(집중 모드·말풍선·호감도 저장소)에 대고 플러그인 클래스를 돌리는 통합 검사용. 기록도 해제도 없습니다. 단위 검사는 `start()` |
| (알아 둘 것) | 킷은 제 jar 를 모르므로 `plugin_ko.properties` 를 **클래스 폴더**(검사 중인 플러그인)에서 먼저 찾고, 없으면 첫 jar 의 것을 집습니다(제품에서는 jar 이름으로 제 것을 고릅니다). 검사 중인 플러그인도 jar 로 실었다면 `textLoader(...)` 로 로더를 지정하세요 |

킷 안에서는 리스너·훅·예약 작업·`onEdt`/`offEdt` 가 **부른 스레드에서 그 자리**에서 돕니다 — 검사가 결정적입니다. 예외는 `prefs()` 입니다: 진짜 저장소(임시 폴더)라 `onChange` 알림은 앱에서처럼 **비동기**로 옵니다. `patch`/`onLoad` 는 흉내 내지 않습니다(계측 없음 → `false`).

<a id="catalogue"></a>

## 11. 카탈로그 — `CATALOG.md`

행동 이름(한글·빈도·숨김), 이벤트 토픽과 키, 훅마다 묻는 자리·기본값·ctx 키, `MascotState` 필드, 읽어도 되는 앱 설정 키, `settings` 의 `type` 값, 기능 스위치 id — 전부 SDK 를 구울 때 앱 소스에서 자동 생성합니다. 손으로 고치지 않으며, 소스와 어긋난 사본으로는 SDK 가 구워지지 않습니다.

<a id="alternatives"></a>

## 12. 약속 밖의 일을 하는 법

약속(§3~§5)에 없는 것은 **앱 클래스를 부르지 말고** 아래처럼 하세요. 앱 클래스는 클래스패스에 있어 부를 수는 있지만 버전마다 바뀝니다(§8).

| 하고 싶은 것 | 하는 법 |
|---|---|
| JSON 읽고 쓰기 | `Json` (§3 표). 더 필요하면 제 라이브러리를 `plugins\lib\` 에 |
| 앱과 같은 옷의 창 | `ctx.theme()` 만 씁니다 — FlatLaf 스타일 문자열은 `theme().style(component, "background:#131F33; …")` 처럼 다크값으로. 그 밖의 룩(앱의 `settings` 패키지)은 약속 밖 |
| 창 자리 기억 | `ctx.manageWindow(window, "settings")` |
| 비밀을 파일에 | `ctx.writeAtomic(path, bytes, true)` — `prefs()` 는 평범한 설정용(평문), 키는 `ownerOnly` 로. ACL 은 최선의 노력이지 암호화가 아닙니다(§3.7) |
| 더블클릭 감지 | `ctx.on("doubleclick", …)` — 전역 AWT 리스너를 걸지 마세요. 앱이 제 통과 창(하트·음표) 위의 누름까지 `click` 으로 넘겨 줍니다 |
| 캐릭터 옆에 창 띄우기 | `ctx.mascotById(id)` 가 null 이 아닌지 본 뒤 `anchor()`(발끝, 바탕화면 좌표) 아래에; 화면은 `screens()` |
| 지금 화면에 있는 캐릭터의 셋 목록 | `ctx.mascots()` 에서 `imageSet()` 을 순서대로 중복 없이 — 켜진 셋(`activeImageSets`)과 다릅니다(켜졌어도 아직 안 태어났을 수 있습니다) |
| 소리 내기 | 효과음은 `playSound(path)`, 목소리(TTS·미리 구운 파일)는 PCM 으로 풀어 `speak(PcmAudio, onStart, onEnd)` — mp3 는 `javax.sound` 가 못 읽으니 wav/PCM 으로 받으세요 |
| 입 움직이기 | `mouthFlap(set, ctx::isSpeaking)` — `behave("Talk")` 를 손으로 반복하지 마세요 |
| 마우스 커서·다른 프로그램의 창 만지기 | 앱이 하지 않는 일이라 문이 없습니다. JNA 로 직접 합니다(약속 밖, `plugins\lib\` 에 jna 를 넣지 마세요 — 앱의 것을 씁니다) |
| 페르소나 편집 | `personaFile(set)` 에 `writeAtomic` — 파일 규약은 앱 것입니다 |

<a id="recipes"></a>

## 13. 자주 쓰는 코드

### 최소 진입 클래스

`src\myplugin\MyPlugin.java` 로 두고 `src\META-INF\services\com.group_finity.mascot.lumi.plugin.LumiPlugin` 에 `myplugin.MyPlugin` 을 적습니다. 매니페스트는 §1 의 것을 그대로 쓰되 최소 구성에서는 `settings` 배열을 빼도 됩니다. 빌드는 [README 의 명령](../README.md)으로 합니다. hello 예제의 진입 클래스를 이것으로 바꾸면 그 검사도 새 동작에 맞게 바꿔야 합니다.

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

`ready` 로 엔진이 뜨기를 기다리고, 먼저 말을 거는 인사는 `quietNow()` 로 지금 조용히 할 때인지 묻습니다. 번역은 `src\plugin_ko.properties`·`src\plugin_en.properties` 에 `greeting=…` 으로 적습니다 — jar 에서는 뿌리에 놓입니다.

아래 짧은 예들은 `start(PluginContext ctx)` 안에 넣는 코드입니다.

### 클릭한 캐릭터에게 답하기

```java
ctx.on("click", event -> {
    if (!"left".equals(event.get("button"))) return;
    if (!(event.get("mascotId") instanceof Integer id)) return;
    if (!(event.get("imageSet") instanceof String set)) return;
    if (ctx.mascotById(id) == null) return;
    ctx.sayTo(id, set, ctx.text("clicked", "Hello!"), 3000);
});
```

더블클릭도 `click` 을 두 번 냅니다 — 더블클릭에 반응하려면 `doubleclick` 을 들으세요. 앱의 클릭 인사도 함께 나올 수 있습니다; `claimClickGreeting()` 은 앱의 클릭 인사를 통째로 맡을 때만 쓰세요. 조회와 표시 사이에 그 아이가 사라지면 `sayTo` 는 같은 셋의 첫째에게 갑니다.

### 선택지 띄우기

```java
ctx.ask("Take a short break?", java.util.List.of("Yes", "Later"),
        10_000, 30_000, index -> {
            if (index < 0) return;
            if (index == 0) ctx.sayQueued("Enjoy your break!", 3000);
        });
```

`10_000` 은 **뜬 뒤** 답을 기다리는 시간, `30_000` 은 줄에서 기다려 줄 시간입니다. `-1` 은 떴는데 답이 없었다, `-2` 는 앱이 치웠다(이미 뜬 물음도 제 새 `ask` 가 대체합니다)는 뜻이고 — 둘 다 보기 번호가 아닙니다. 정상 콜백은 EDT 밖이니 Swing 을 만지려면 `onEdt` 로 보내세요. 배포할 때는 물음과 보기도 `text()` 로 옮기세요.

### 인사 고치기

```java
ctx.hook("speech.greeting", (hook, current, args) -> {
    if (!"Lumi".equals(args.get("imageSet"))) return null;
    if (!(current instanceof String line) || line.isBlank()) return null;
    return "[my plugin] " + line;
});
```

매니페스트에도 `"hooks": ["speech.greeting"]` 을 적습니다. `null` 은 앞선 핸들러의 답을 그대로 둡니다. `bubble.text` 핸들러 안에서 `ctx.say()` 를, `focus.quiet` 핸들러 안에서 `ctx.quietNow()` 를 부르면 같은 훅에 다시 들어가니 하지 마세요.

### 전역 단축키 걸기

```java
com.group_finity.mascot.lumi.plugin.HotkeyResult result =
        ctx.bindHotkey("greet", "Ctrl+Alt+L", () -> ctx.say("Hello!", 3000));
if (result != com.group_finity.mascot.lumi.plugin.HotkeyResult.OK) {
    ctx.log().warning("Hotkey registration: " + result);
}
```

등록 이름은 플러그인마다 따로지만 실제 키 조합은 다른 프로그램·플러그인과 부딪힐 수 있습니다. 선언형 `hotkey` 설정은 글자만 저장하니 처음 값을 걸고 값이 바뀌면 다시 거세요. 손으로 풀 때는 `unbindHotkey("greet")` 입니다.

### 자주 불리는 훅에서 설정을 캐시하기

별도 진입 클래스 `src\myplugin\QuietPlugin.java` 로 두고, 쓸 때는 서비스 파일에 `myplugin.QuietPlugin` 을 적습니다. 매니페스트에는 `"hooks": ["focus.quiet"]` 을, `settings` 배열에는 `{"key":"quiet","type":"bool","default":false,"label":"Keep quiet"}` 을 더합니다.

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

훅은 원자적 캐시 값만 읽습니다 — 파일도, 설정 저장도, 저장소 잠금도 없습니다. 설정 변경 알림은 비동기라 바꾼 직후 잠깐 옛 값이 쓰일 수 있습니다. 리스너는 정상 종료든 시작 실패든 `stop()` 에서 직접 풉니다(§2).

### 작업 스레드 고르기

| 작업 | 어디서 |
|---|---|
| Swing 창·위젯 | `ctx.onEdt(...)` — 콜백은 짧게 |
| 이벤트 처리·짧은 배경 작업 | `ctx.on(...)` / `ctx.offEdt(...)` — 둘은 이 플러그인의 이벤트 스레드를 나눠 씁니다 |
| 주기·지연 작업 | `ctx.every(initialMs, periodMs, ...)` / `ctx.after(ms, ...)` — 이 플러그인의 타이머 스레드 |
| 훅 판정 | 부른 스레드 — 캐시 값으로 즉시 답합니다 |
| 긴 네트워크 작업 | 시간 제한과 종료 처리를 갖춘 제 작업 스레드 — 훅·EDT·이벤트 줄을 막지 않습니다 |
