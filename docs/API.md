# Little LUMI Model — 플러그인 API 참조 (api_version 1)

**SDK/앱 1.5.1, `api_version` 1** 기준 참조입니다. 이 문서는 플러그인이 기대도 되는 표면 전부이고, 여기 적힌 것만이 앱 업데이트를 건너 살아남는다고 약속합니다([호환 정책](#compatibility)). 같은 폴더의 `javadoc\index.html` 은 아래 타입의 시그니처·주석을 그대로 옮긴 것이고, `api-src\` 는 그 소스 파일들입니다. 처음이면 [첫걸음 안내](../plugin-guide.html)부터 따라 하고, 이름을 찾을 때는 [카탈로그](CATALOG.md)를 쓰세요. [English](API.en.md).

[매니페스트](#manifest) · [생명주기](#lifecycle) · [메서드](#context) · [이벤트](#events) · [훅](#hooks) · [바이트코드](#bytecode) · [데이터 조각](#data-mods) · [호환 정책](#compatibility) · [하지 말 것](#practices) · [테스트](#testing) · [카탈로그](#catalogue) · [약속 밖](#alternatives) · [자주 쓰는 코드](#recipes) · [캐릭터 제작](#character-authoring) · [미리보기](#authoring-preview) · [연계 활동](#animation-activities) · [잡힘 회전](#held-motion) · [창작마당 게시](#workshop-publishing)

| 타입 (`com.group_finity.mascot.lumi.plugin`) | 역할 |
|---|---|
| `LumiPlugin` | 진입점 인터페이스 — `start(PluginContext)` / `stop()` |
| `PluginContext` | 앱이 건네는 문 — 말하기·행동·상태 조회·제어·이벤트·훅·설정·UI 자리·단축키·바이트코드 |
| `MascotState` | 캐릭터 하나의 지금 상태(id·이미지셋·사각형·발끝·방향·행동, 바탕화면 좌표) — `ctx.mascots()` 가 돌려주는 레코드 |
| `PcmAudio` | 재생할 소리 한 토막(형식·PCM 바이트·길이 ms) — `ctx.speak()` 가 받는 레코드 |
| `MicCaptureLease` | 마이크 녹음 동안 앱 음성을 억제하는 손잡이 — `ctx.beginMicCapture()`로 얻고 `close()`로 반환 |
| `HotkeyResult` | `ctx.bindHotkey()` 의 답 — `OK` `NONE` `BAD_COMBINATION` `REFUSED` `UNSUPPORTED` |
| `PluginTheme` | 앱의 옷 — 색·CJK 글꼴·단추/토글/슬라이더 손질·카드 조각 (`ctx.theme()`) |
| `Json` | 의존성 없는 JSON 읽기·쓰기(`parse` `get` `getString` `write`) — LLM 답장·설정 파일에 |
| `PluginEvents` | 「일어난 뒤 알림」 버스(토픽·페이로드 표는 §4) |
| `PluginHooks` | 「정하기 전 질문」(훅 규약·카탈로그는 §5), `PluginHooks.Handler` |
| `LumiTransformer` | 로드 전 클래스 변환기 서비스 |
| `PluginDescriptor` | 읽힌 `plugin.json` |
| `PluginPrefs`, `PluginText` | 제 설정 저장소(`ctx.prefs()`)와 제 5개 언어 문구(`ctx.text()`) |
| `PluginUi.MenuHandle`, `PluginUi.FeatureToggle` | 메뉴 줄·단추의 손잡이와 즉시 반영되는 기능 스위치 |
| `ScheduledHandle` · 실험 | 예약 작업의 취소·취소 여부 |
| `PluginSettingsSchema` | 선언형 설정 파서 — 보통은 `plugin.json` 에 선언만 하면 됩니다 |
| `PluginTestKit` · 실험 | 가짜 컨텍스트·호출 기록·직접 미는 시계(§10) |
| `WorkshopContent`, `WorkshopContent.PromptSource` | 창작마당 제작자 파일과 구독 식별자 읽기 전용 조회 |

| `WorkshopPublisher` | 사용자 확인 후 창작마당 게시·갱신 및 업로드 파일 검증 |
| `VisualLogic`, `CharacterDefinition` · 실험 | 시각 규칙과 자체 캐릭터 구성 생성 |
| `CharacterTemplates` · 실험 | 설치된 행동·액션·모션 템플릿 조회 |
| `AuthoringPreview`, `AnimationActivities` · 실험 | 실제 엔진 미리보기와 연계 활동 조회·시험 |
| `TerrainRect`, `TerrainHandle` · 실험 | 플러그인 발판의 영역과 등록·갱신·해제 |


### 창작마당 제작자 데이터

`WorkshopContent.promptSources(imageSet)`는 모드 순서대로 정렬한 변경 불가능한 `PromptSource` 목록을 반환합니다(아래 모드가 충돌 시 우선). 각 레코드는 `promptsFile()`·`personaFile()`(`Path`, 읽기 전용이며 없을 수 있음), `workshopId()`, `subscriptionId()`, `modStyleName()`(Workshop ID가 포함된 페르소나 표시 이름 유지)을 제공합니다. 파일이 없거나 읽을 수 없는 경우를 처리하세요. 비활성·구독 해지·미승인 공급자는 제외하며 시작 후에는 준비된 목록만 제공합니다. 잘못된 캐릭터 이름이나 Workshop 미지원 구성에는 빈 목록을 반환합니다.

사용자가 명시적으로 선택한 제작자 프리셋을 두 식별자에 묶으세요. 업데이트·OFF/ON·일시적인 다운로드 대기는 구독 식별자를 유지하며, 구독 해지 후 재구독은 식별자가 바뀌어 새 선택이 필요합니다. 조회는 프롬프트 선택이나 코드 실행 동의가 아닙니다. `WorkshopContent.hasDirectSource(id)`는 구독 해지 기록까지 포함해 이전 사본의 중복 조회를 막는 용도이며 활성·승인 여부를 뜻하지 않습니다. 두 메서드는 컨텍스트 없이 본체 상태를 조회합니다.

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
  "app_version": "1.5.1",
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

모드에 실행 코드와 데이터를 함께 넣을 수 있습니다. 동의는 출처와 실행 파일의 목록·내용을 기준으로 하며, 이미지·음성 등 일반 데이터만 바뀌면 재동의를 요구하지 않습니다. `plugins/runtime/`의 동반 파일은 모두 실행 입력으로 검사합니다. Windows에서는 실행 파일 내용을 복사하지 않고, 하드링크와 쓰기·삭제 방지 핸들로 보호한 별도 실행 경로를 사용합니다. 일반 데이터는 원본에서 읽고 실제 사용 시 검증합니다. 비활성·미승인 코드 패키지는 데이터도 적용하지 않습니다. 모드 파일은 앱 종료 후 변경하세요.

Windows x64 DLL은 모드 루트의 `native/` 또는 `audio-native/`에 둘 수 있습니다. 보조 인터프리터·실행 도구는 `plugins/runtime/`에 동반 파일과 함께 넣습니다. 이 디렉터리 전체는 실행 코드 묶음이며 EXE·DLL·PYD의 PE 형식을 검사합니다. 검증된 실행 경로에서 상대 폴더 구성을 유지하고, 클래스패스 JAR 옆에도 `native/`·`audio-native/`·`runtime/` 경로를 보존합니다. 앱이 이 도구를 자동 실행하지는 않습니다. 코드 변경은 다시 동의를 받아야 하며, 설치 원본 경로나 원본 JAR 이름이 실행 경로라고 가정하지 마세요.

- `start(ctx)` — 내장 기능이 뜬 뒤, 엔진(마스코트)이 실리기 **전**에 한 번 불립니다. 여기서 `on`/`hook`/`addTrayItem`/`onLoad` 를 겁니다. 이때 캐릭터 상태는 비어 있을 수 있으니 엔진이 필요한 일은 `ready` 에서 하세요. 던지면 그 플러그인만 실패로 표시됩니다.
- `ready` 이벤트 — 엔진이 뜨고 모든 플러그인의 `start` 가 끝난 뒤 한 번, 앱이 냅니다. `PluginTestKit` 은 대신 내 주지 않습니다(§10).
- `stop()` — 앱 종료 때 불립니다(기본 구현은 빈 것). `start()` 가 던진 경우에도 부분 시작을 정리하려고 곧바로 불리므로, 아직 초기화하지 못한 필드가 있어도 견디게 작성하세요. 플러그인과 한 jar 안의 서비스 구현들은 실제 시작의 역순으로 멈춥니다.
- `stop()` 뒤 **앱이 거둡니다**: 이 컨텍스트로 건 리스너·훅·전역 단축키·메뉴 줄·기능 스위치·예약 작업은 플러그인이 정리하지 않았어도 로더가 전부 해제합니다. 시작 실패 때도 곧바로 해제하며, 실패한 플러그인의 의존자는 시작하지 않습니다. 실행 중 켜기·끄기는 없습니다(모드 탭의 스위치는 다음 실행부터).
- **직접 만든 것은 직접 닫습니다**: 창·클라이언트·스트림·작업 스레드는 `stop()` 에서 닫으세요. **`prefs().onChange(...)` 리스너도 컨텍스트가 거두지 않습니다** — 등록 장부에 없으니 리스너를 들고 있다가 `stop()` 에서 `removeOnChange(listener)` 로 푸세요(§13 `QuietPlugin`). 이미 변환한 클래스는 되돌리지 않습니다.
- 바꾼 **상태도 되돌립니다**: `claimClickGreeting()` 의 점유는 이 플러그인이 아직 쥔 경우에만 풀리고, `summon()` 으로 부르고 아직 `dismiss()` 하지 않은 캐릭터는 보내며, `setCharactersVisible(false)` 로 숨겨 둔 채였으면 다시 보입니다. 뒤의 둘은 **앱이 계속 도는 동안**(시작 실패)에만 하고, 앱 종료 때는 캐릭터에 손대지 않습니다.
- 그 뒤로 **컨텍스트는 죽은 것**입니다: 늦게 오는 등록(`on`·`hook`·`every`·`after`·`onEdt`·`offEdt`·메뉴 줄·`addSettingsButton`·`addFeatureToggle`·`bindHotkey`·`claimClickGreeting`·`onLoad`·`patch`·`ask`·`summon`·`dismiss`·`moveTo`·`setCharactersVisible`)과 행동 문 전부(`say`·`sayQueued`·`showBusy`·`behave`·`speak`·`mouthFlap`·`heart`·`playSound`·`awardAffection`·`publish`·`setSetting`·`writeAtomic`·`manageWindow`)는 **던지지 않고** 무시됩니다 — 로그에 플러그인마다 한 줄 `<id>: registration after stop() ignored: <what>` 이 남고, 돌아오는 것은 무해한 값입니다(이미 취소된 `ScheduledHandle`, `removed()` 가 참인 `MenuHandle`/`FeatureToggle`, `HotkeyResult.REFUSED`, `summon` 은 -1, `dismiss`/`moveTo`/`onLoad`/`patch` 는 false). `ask` 의 콜백에는 `-2` 가 가는데, 이때만은 정상 경로의 전용 스레드가 아니라 **부른 스레드에서 그 자리에서** 전달됩니다. `mascots()`·`prefs()`·`text()` 같은 읽기는 계속 답합니다. 예외: `beginMicCapture()`는 종료된 컨텍스트에서 `IllegalStateException`을 던집니다. `every()`/`after()` 는 `task == null`(그리고 `every` 의 `periodMs <= 0`)을 해제 여부보다 먼저 검사하므로 죽은 컨텍스트에서도 `IllegalArgumentException` 을 던집니다. `stop()` 안에서 제 스레드를 멈추면 이 줄을 볼 일이 없습니다.
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
| `beginMicCapture()` → `MicCaptureLease` | **EDT 밖에서** 호출. 현재 음성 출력 라인이 실제 닫힐 때까지 기다린 뒤 반환. 보유 중 본체 보이스팩·TTS·미리듣기·다른 플러그인의 새 음성을 버린다. 녹음 장치를 닫은 뒤 lease를 `close()`한다(checked exception 없음). 중첩/여러 플러그인의 lease가 모두 닫혀야 새 음성이 가능하고, 버렸던 음성은 재생되지 않는다. 컨텍스트 종료 때 자기 lease를 자동 반환하며 진행 중 획득도 취소한다. EDT/종료 후/중단된 획득/출력 닫기 실패는 `IllegalStateException`. 다른 앱이나 효과음을 음소거하지 않고 마이크 권한을 부여하지도 않는다 |

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
| `addTrayItem(label, action)` → `PluginUi.MenuHandle` | 트레이 메뉴 줄(앱 줄 아래·「종료」 위). `action` 은 EDT. 돌려주는 **손잡이**로 `setLabel`·`setEnabled`·`setChecked(Boolean)`·`remove()`, 읽기 `label()`·`enabled()`·`checked()`·`removed()` — 줄이 안 바뀌면 무시해도 됩니다. 바뀐 것은 다음에 메뉴가 열릴 때 보입니다(메뉴는 열 때마다 새로 짓습니다). 다만 `setLabel` 의 이름은 이미 떠 있는 트레이·캐릭터 메뉴 줄과 설정 창 단추에도 곧 EDT 에서 반영됩니다 — 언어 변경 이벤트가 창을 다시 지은 뒤에 도착해도 됩니다. 어느 스레드에서든. **이름은 문자열이라 언어가 바뀌어도 스스로 안 바뀝니다** — `settings` 이벤트의 `phase=language` 를 듣고 `setLabel(text(...))` 로 다시 놓으세요(LUMI Chat·Super Bongo 가 그렇게 합니다) |
| `addTrayToggle(label, initial, onChange)` → `MenuHandle` · 실험 | **토글 줄** — 이름은 그대로, 「켜짐/꺼짐」 상태 칩(앱의 「잠깐 숨기기」 문법). 누르면 스스로 뒤집고 `onChange(새 값)` 을 EDT 에서 부릅니다; `setChecked` 는 칩만 바꾸고 `onChange` 를 부르지 않습니다 |
| `addSettingsButton(label, action)` → `MenuHandle` | 설정 창 정보 탭의 사용법 단추 가까이에 단추 하나. `action` 은 EDT. 이름 변경은 이미 열린 단추에도 EDT에서 반영됩니다. 등록·활성·제거는 다음에 창을 열 때 반영됩니다 |
| `addFeatureToggle(label, description, state, onChange, dependsOnFeature)` → `PluginUi.FeatureToggle` | 루미 탭 「기능 켜고 끄기」 카드의 **맨 아래**에 앱 모양의 스위치 줄. 등록이 0개면 줄·여백도 없습니다. 창을 열 때 `state` 로 체크를 채우고, 체크가 바뀌는 즉시 `onChange(새 값)` 으로 건넵니다. false 를 돌려주면 `state` 로 되돌립니다. `dependsOnFeature`(`bongo` 등)가 꺼지면 회색이 되지만 체크는 보존됩니다. 하네스 `press:feature:<라벨>` |
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

### 3.12 플러그인 지형 · 실험

| API | 동작 |
|---|---|
| `registerTerrain(name, List<TerrainRect>)` → `TerrainHandle` · 실험 | 이 플러그인의 바탕화면 지형을 등록합니다. 플러그인이 다르면 같은 이름을 쓸 수 있습니다. 같은 플러그인의 등록 중인 이름을 다시 쓰면 `IllegalArgumentException`입니다. |
| `TerrainRect(id, left, top, right, bottom)` · 실험 | 바탕화면 논리 좌표의 사각형입니다. `MascotState.anchor()` 및 Swing 화면 좌표와 같은 공간이며 음수 좌표도 허용합니다. 너비·높이가 아니라 네 변의 좌표를 받습니다. |
| `TerrainHandle.update(rects)` → boolean · 실험 | 해당 등록의 지형 전체를 교체합니다. 빈 목록은 모든 조각을 지우고 등록 이름을 유지합니다. 해제 후에는 `false`입니다. |
| `TerrainHandle.remove()`, `close()`, `isRemoved()` · 실험 | 등록을 해제합니다. 중복 해제는 안전하며 이름을 다시 등록할 수 있습니다. 옛 핸들은 새 등록을 바꾸지 못합니다. |

```java
TerrainHandle platform = ctx.registerTerrain("platform", List.of(
    new TerrainRect("bar", 100, 400, 500, 408)
));
platform.update(List.of(new TerrainRect("bar", 100, 420, 450, 428)));
platform.update(List.of()); // 조각만 지웁니다.
platform.remove();          // 이름까지 해제합니다.
```

등록 이름과 조각 ID는 공백뿐인 값을 제외한 1~128자입니다. 한 등록 안의 조각 ID는 중복될 수 없고 최대 조각 수는 `TerrainRect.MAX_PIECES`(2048)입니다. `right > left`, `bottom > top`이어야 하며 너비·높이는 양의 int 범위여야 합니다. 잘못된 값은 이전 지형을 바꾸기 전에 예외를 발생시킵니다. 목록과 조각은 사본으로 전달하므로 호출 후 입력 목록을 수정해도 반영되지 않습니다.

모든 호출은 어느 스레드에서나 가능합니다. 엔진은 꼬미의 다음 이동 틱 전에 최신 목록을 반영합니다. 반환 성공은 데이터 접수를 뜻하며 착지 완료를 뜻하지 않습니다. 같은 틱 전에 여러 번 갱신하면 마지막 목록을 사용합니다. 조각 ID를 유지하면 기존 바닥 참조를 유지하고, 목록에서 빠진 조각은 바닥 참조도 무효화합니다. 이동하는 발판에 꼬미를 실어 나르는 기능은 제공하지 않습니다. 플러그인 종료·시작 실패 후 컨텍스트가 남은 등록을 자동 해제하며, 종료 후 등록은 이미 해제된 핸들을 반환합니다.

정원·미니게임·다른 플러그인의 지형은 각각 유지됩니다. 후보 순서는 미니게임 → 정원 → 플러그인 → 일반 창입니다. 플러그인 사이의 겹침은 등록 순서, 한 등록 안의 겹침은 조각 목록 순서로 먼저 나온 후보가 우선합니다. 화면과 바닥의 기존 엔진 규칙은 계속 적용됩니다. 현재 엔진은 작업 영역 상단에서 140 논리 픽셀 이상의 여유가 있는 사각형 윗변을 바닥으로 사용합니다.

이 API는 사각형 테두리를 창 지형처럼 제공합니다. 윗변 착지·걷기·발판 끝에서 지지면 상실은 기존 행동이 처리합니다. 그림의 불투명 영역 추출, 몸 전체의 장애물 충돌, 곡선 오르막 보정, 목표 점프, 접촉·파기 이벤트는 제공하지 않습니다. 바탕화면 지형은 방송 모드에서 사용하지 않고 바탕화면으로 돌아오면 다시 사용합니다. 플러그인이 그림 표시와 좌표 변환을 담당하며, 그림이 숨거나 창 뒤로 가리는 것은 자동 감지하지 않으므로 해당 지형을 갱신하거나 지워야 합니다.

`PluginTestKit`은 `registerTerrain(name, rects)`, `updateTerrain(name, rects)`, `removeTerrain(name)` 호출을 기록하고 이름 중복 및 종료 시 회수를 재현합니다. 충돌·그리기·엔진 틱은 재현하지 않습니다. 실행 예제는 [발판 하나](../examples/terrain-basic/README.md)입니다.

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
| `app` · 실험 | 앱 종료·재시작 | `phase`(`exit`/`restart`). 플러그인 `stop()` 전에 짧은 상한으로 전달합니다. `restart`에서는 작별 연출을 생략하고, 두 경우 모두 필수 저장·해제는 `stop()`에서 수행합니다 |
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
app         {phase=exit} / {phase=restart}
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

### 조건식 실행과 호스트 접근

XML의 조건식과 스크립트 속성은 호스트 접근을 제한한 Nashorn에서 평가합니다. 산술·비교·논리식, 일반 JavaScript 함수와 `eval`, 기존 `mascot`·`lumi` 바인딩은 유지합니다. 조건식에서 엔진·컨텍스트 객체 접근, `load`·`loadWithNewGlobal`을 통한 추가 스크립트 로딩, `exit`·`quit` 및 scripting 모드 기능에 의존하지 마세요. 이러한 호스트 기능을 사용하던 모드는 수정이 필요합니다.

동봉 `nashorn-core-15.7.jar`는 보안 수정을 적용한 `15.7-lumi.2`입니다. 파일명은 같지만 Maven Central의 원본 JAR와 다르며, 원본으로 교체하면 본체의 보호 검사에 의해 조건식 엔진 초기화가 거부됩니다. 수정 패치·빌드 방법·해시는 [대응 소스 안내](sources/README.md)에 있습니다. 이 변경은 본체의 조건식 평가 경로에 적용되며, 승인된 Java 플러그인의 실행 권한과 공개 API 호환 번호는 그대로입니다.

<a id="compatibility"></a>

## 8. 호환 정책

- 플러그인 API 의 공개 표면은 **안정**과 **실험** 둘로 나뉩니다. 표시가 없는 항목은 안정입니다. 실험 항목은 표에 「실험」 칩으로 표시되고 `sdk.json` 의 `preview` 에 같은 목록이 있습니다.
- **안정 항목은 같은 `api_version` 안에서 더해지기만 합니다.** 없어지지 않고 형이 바뀌지 않습니다. 결정 자리가 앱에서 사라진 훅은 기본값만 돌려주는 빈 자리로 남고 표에 폐기로 표시됩니다. 이벤트에는 키가 더해질 수 있고 빠지지 않습니다 — 모르는 키는 무시하세요.
- **실험 항목은 `api_version` 을 올리지 않고 바뀌거나 없어질 수 있습니다.** 그때는 `CHANGELOG.md` 에 적고, 없앤 자리는 한 앱 버전 동안 빈 자리로 남깁니다. 실험 항목이 앱 버전 하나를 바뀌지 않고 넘기면 안정으로 올립니다. 1.5.1 의 구분 기준: 자사 플러그인(LUMI Chat·Super Bongo)이나 앱 자체가 실제로 쓰는 문과 그 짝은 안정, 그 밖은 실험입니다.
- **`api_version` 은 안정 항목을 없애거나 모양을 바꿔야만 할 때 오릅니다.** 그때 이행 목록을 `CHANGELOG.md` 에 적고, **직전 버전(N-1)을 위해 쓰인 플러그인은 한 버전 더 실립니다**(없앤 훅은 빈 자리, 없앤 메서드는 위임 껍데기로). 두 버전 전 플러그인은 실리지 않고 업데이트 안내를 받습니다. 앱보다 새 `api_version` 을 적은 플러그인도 실리지 않습니다.
- 「더해지기만 한다」는 뒤집으면 나중 앱 버전에서 더해진 메서드·훅·토픽이 같은 `api_version` 의 옛 앱에는 없다는 뜻입니다. 검증한 앱 버전을 `plugin.json` 의 `app_version` 에 적으세요. 실행 중인 앱과 다르면 모드 탭에 「N용」 칩이 붙습니다(안내일 뿐 막지 않습니다).
- 약속 밖: 변환기가 건드리는 코어 클래스의 모양, `PluginContext` 와 이 문서의 타입 표 밖의 공개 클래스(클래스패스에 있어 부를 수는 있지만 버전마다 바뀔 수 있습니다), `conf\settings.properties` 의 앱 키.
- (현재 버전 사실) 1.5.1 의 로더는 `api_version` 을 읽기만 하고 판단하지 않습니다. 위의 거부·칩은 다음 앱 버전부터 로더가 집행합니다.
- SDK 버전은 앱 버전과 같습니다(`sdk.json` 의 `sdk_version` = `app_version`). `api_version` 은 별개의 호환 번호로 이 문서 제목에 적혀 있습니다. 문서·예제 개정 내용은 배포 안내에서 확인하세요.

<a id="practices"></a>

## 9. 하지 말 것

- 이벤트 리스너에서 EDT 를 기다리지 마세요(발행 쪽이 EDT 일 수 있습니다 — `SwingUtilities.invokeLater` 나 `onEdt` 로).
- 훅 핸들러에서 파일·네트워크·잠금을 잡지 마세요.
- 사용자 데이터를 `plugindata\<id>\` 밖에 두지 마세요. 개인정보를 다루면 창작마당 설명에 밝히세요.
- `conf\settings.properties` 의 앱 키를 바꾸지 마세요(제 접두 키만).

<a id="testing"></a>

## 10. 테스트 킷 — `PluginTestKit`

SDK 1.5.1의 `beginMicCapture()`도 가짜 백엔드에서 검사할 수 있습니다. `beginMicCapture`/`endMicCapture` 호출이 소유자 id와 함께 기록되고, lease 보유 중 `speak`는 `speakSuppressed`를 기록하며 재생 콜백을 호출하지 않습니다. `kit.close()`는 중첩 lease를 모두 반환합니다. 실제 오디오 장치는 열지 않습니다.

`api_version`은 호환 가능한 추가이므로 1을 유지합니다. 이 메서드가 없는 이전 앱도 지원하는 플러그인은 시작 시 메서드 존재를 확인하고 음성 입력만 비활성화해야 합니다. `app_version`은 표시용이며 최소 앱 버전을 강제하지 않습니다. SDK 1.5.1으로 컴파일했다는 사실만으로 이전 앱에 새 메서드가 생기지는 않습니다.

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
| 소리 내기 | 효과음은 `playSound(path)`, 목소리(TTS·미리 구운 파일)는 PCM 으로 풀어 `speak(PcmAudio, onStart, onEnd)` — 앱 JavaSound 디코더는 WAV·OGG(Vorbis)·MP3·FLAC·AIFF·AU를 지원합니다. `speak`에는 디코딩한 PCM을 전달하세요 |
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

<a id="character-authoring"></a>

## 시각 규칙 제작 — `VisualLogic` 실험 · `CharacterDefinition` 실험

공개 순수 엔진으로 GUI 미리보기와 본체 실행이 같은 규칙을 사용합니다. `VisualLogic.read(Reader)`는 UTF-8로 읽은 Properties 문자 스트림을 받아 `Document`를 반환합니다. `Document(id, imageSet, variables, dialogues, rules)`는 변경 불가능하며 생성 시 검증합니다. `Dialogue(id, language, text, audio)`, `Rule(id, event, variable, increment, comparison, threshold, dialogue, behavior)`와 `Effect(ruleId, dialogueId, text, audio, behavior)`는 공개 record입니다. `variables`는 `Map<String,Double>`, `dialogues`는 ID별 `Map<String,Dialogue>`, `rules`는 순서가 있는 `List<Rule>`입니다.

```properties
schema=1
id=maker.example
imageSet=MM_example
variable.clicks=0
dialogue.hello.language=ko
dialogue.hello.text=안녕하세요
dialogue.hello.audio=sound/maker.example/hello.wav
rule.count=1
rule.0.id=greet
rule.0.event=click
rule.0.variable=clicks
rule.0.increment=1
rule.0.comparison=ge
rule.0.threshold=2
rule.0.dialogue=hello
rule.0.behavior=ModMakerIdle
```

`new VisualLogic.Session(document)` 뒤 `dispatch(event)`가 `List<Effect>`를 반환합니다. 이벤트는 `ready`, `click`, `menu:<식별자>`입니다. 매칭 규칙을 문서 순서대로 실행하며 변수 증가 **후** `always/eq/ne/lt/le/gt/ge`로 비교합니다. 변수·대사·행동은 빈 문자열로 생략할 수 있습니다. 변수를 생략하면 증가값은 0, 조건은 always여야 합니다. 다른 이벤트는 빈 결과입니다. `variables()`는 복사본, `restore(Map<String,Double>)`는 알려진 변수만 복원하며 잘못된 값은 전체 복원을 거부합니다. null은 허용하지 않습니다. `close()` 후 변경·실행은 거부합니다. 증가 연산 넘침은 변수 변경과 효과를 모두 취소합니다.

식별자는 영숫자로 시작하는 영숫자·점·밑줄·하이픈 1~64자입니다. 입력 100만 문자, 변수 128개, 대사 256개, 규칙 128개, 대사 4096자, 숫자 유한값 ±1e12 이내입니다. 대사 language는 32자 이내 메타데이터이며 명시적으로 선택한 대사를 번역하지 않습니다. audio는 `sound/` 아래 상대 WAV 경로이며 상위 이동을 허용하지 않습니다. 스크립트·반복·재귀·파일/네트워크 동작은 없습니다.

패키지의 **`plugins/runtime/visual-logic.properties`**는 JAR 없이도 본체가 발견합니다. 실행 코드로 분류되어 승인·변경 시 재승인·다음 실행 적용과 보호된 실행 사본을 따릅니다. 미승인 모드는 PNG/WAV까지 적용하지 않습니다. 본체 어댑터는 지정 imageSet의 click만 받으며 클릭 간격은 100ms 이상입니다. 메뉴 이벤트마다 트레이 항목을 등록합니다. 대사는 5초 말풍선, WAV는 본체 효과음 설정, 행동은 해당 캐릭터가 제공하는 이름만 사용합니다. 변수는 모드 설치 출처별 설정에 분리되어 정상 종료 때 저장되고 재시작 때 복원됩니다. 비정상 종료 후에는 마지막 정상 종료 값입니다. 모드 ID 문자열만으로 다른 모드 상태를 공유하지 않습니다.

`CharacterDefinition.generate(String imageSet, List<Frame>)`는 `Map<String,String>`으로 `img/<imageSet>/conf/actions.xml`, `behaviors.xml`, `info.xml`을 반환합니다. `Frame(String image,int durationMs,int anchorX,int anchorY)`의 image는 이미지셋 안의 PNG 상대 경로입니다. 1~4096개 프레임, 16~60000ms, 앵커 ±8192를 허용하며 내장 이미지셋 이름은 거부합니다. 지속시간은 60Hz 엔진 tick으로 반올림합니다. PNG는 호출자가 내보냅니다. 자체 프레임으로 ModMakerIdle/Stand/Walk/Fall/Dragged/Thrown/ChaseMouse 기본 행동을 생성합니다. 이 API는 파일을 설치하거나 덮어쓰지 않습니다. Steam 게시 API는 포함하지 않습니다.

`CharacterDefinition.behaviorNames()`는 생성 행동 이름의 변경 불가능한 `Set<String>`을 반환합니다. 편집기는 이 기본 집합과 선언한 커스텀 행동으로 선택을 검증합니다. 알 수 없거나 중복된 Properties 필드, 불완전한 대사 레코드는 거부합니다.


<a id="authoring-preview"></a>

## 실제 본체 제작 시험 — `AuthoringPreview` 실험

`PluginContext.openAuthoringPreview(AuthoringPreview.Snapshot)` · 실험은 실행 중인 데스크톱 본체에 제작 시험 전용 캐릭터 한 개를 만들고 `AuthoringPreview.Session`을 반환합니다. 플러그인 컨텍스트마다 열린 세션 하나만 허용합니다. 제작기에서 사용자가 시험 시작을 선택한 뒤 호출하며, 모드 설치·코드 승인·일반 소환 목록·영구 변수 저장은 변경하지 않습니다. `PluginTestKit`에서는 `UnsupportedOperationException`을 던집니다. 해제된 컨텍스트 또는 이미 열린 세션에서는 `IllegalStateException`을 던집니다.

- `Frame(byte[] png,int durationMs,int anchorX,int anchorY)`는 PNG 바이트, 16~60000ms와 ±8192 앵커를 받습니다. 입력·반환 바이트 배열을 복사합니다.
- `Snapshot(List<Frame>,VisualLogic.Document,Map<String,byte[]> audio)`는 규칙과 필요한 이미지를 함께 전달합니다. 음성 키는 규칙 대사의 `sound/...wav` 상대 식별자입니다. 실제 파일 경로를 읽는 요청이 아니며, 경로·XML·스크립트·JAR를 받지 않습니다. 모든 음성 참조가 audio에 있어야 하고 행동은 `CharacterDefinition.behaviorNames()` 안에 있어야 합니다.
- 한 스냅샷은 1~16384개 PNG 프레임, 최대 256개 WAV, 파일당 4MiB, 전체 24MiB를 허용합니다. PNG는 가로·세로 각각 최대 2048, 전체 1600만 픽셀입니다. 음성은 PCM WAV, 1~2채널, 최대 96kHz·30초입니다. 바이트·형식 검증 실패는 예외로 반환됩니다.
- `Session.replace(Snapshot)`는 수정본 전체를 검증한 뒤 시험 캐릭터를 교체합니다. 위치는 유지하고 변수는 초기값으로 돌아가며 `ready`를 실행합니다. 거부된 수정본은 기존 시험 상태를 보존합니다.
- `Session.dispatch(String)`는 `ready`, `click` 또는 현재 문서에 선언된 `menu:<식별자>`를 실행합니다. 실제 시험 캐릭터 클릭도 규칙에 전달됩니다. 닫힌 세션이나 선언하지 않은 이벤트는 거부합니다.
- `Session.state()`는 `State(boolean closed,int mascotId,long revision,Map<String,Double> variables,List<String> trace)` 복사본입니다. 변수와 최근 실행 기록을 읽습니다. 기록은 최대 80줄·줄당 512자로 제한하며 어느 스레드에서나 조회할 수 있습니다.
- 열기·교체·이벤트 실행은 Swing EDT 밖에서 호출합니다. `close()`는 반복 호출할 수 있으며, EDT에서는 정리를 예약하고 다른 스레드에서는 정리가 끝날 때까지 기다립니다. 제작기 종료 시 호출합니다. 플러그인 컨텍스트 해제도 세션을 정리합니다. 시험 캐릭터·말풍선·음성·자체 임시 파일은 세션 소유입니다.

시험 상태는 저장 프로젝트나 내보낸 모드에 기록되지 않습니다. 영구 설치·승인·본체 재시작 시험은 내보내기 흐름에서 별도로 수행합니다.

### 행동별 프레임 (실험)
`CharacterDefinition.Behavior(String name,String kind,List<Frame> frames)`와 `generateBehaviors(String imageSet,List<Behavior>)`는 행동마다 별도 PNG·지속시간·앵커를 제공합니다. 이름은 `[A-Za-z][A-Za-z0-9_-]{0,63}`, kind는 `behaviorNames()`의 기본 동작 이름입니다. 커스텀 포즈에는 `ModMakerIdle` kind를 사용합니다. 기본 이름은 같은 kind만 허용하고 중복 이름은 거부합니다. `ModMakerIdle`이 필수이며 빠진 기본 엔진 행동은 대기 프레임을 사용합니다. 최대 512개 행동·행동당 4096개 프레임·총 16384개 프레임입니다. 기존 `generate`는 동일하게 동작합니다. `behaviorNames()`는 기본 이름만 반환하므로 편집기는 자체 행동 이름을 추가하여 검증합니다.

`AuthoringPreview.Behavior(String name,String kind,List<Frame> frames)`와 `Snapshot(frames,document,audio,behaviors)`는 같은 계약으로 실제 엔진에 행동별 이미지를 전달합니다. behaviors가 비어 있으면 기존 frames를 공통 사용하고, 있으면 behaviors가 실제 이미지 원본입니다(frames에는 기본 대기 목록을 전달). 전체 행동 합계 16384개 프레임·24 MiB·디코딩 1,600만 픽셀 예산을 공유합니다. 기존 3인자 Snapshot도 유지됩니다. 선언된 커스텀 행동을 규칙에서 참조할 수 있고 잘못된 스냅샷은 기존 세션을 보존합니다. 동의·수명·파일 접근 계약은 변하지 않습니다.

`generateBehaviors(String imageSet,List<Behavior>,String previewImage)`는 선택적인 대표 PNG 경로를 받습니다. `null`은 첫 대기 프레임을 그대로 사용합니다. 경로는 이미지셋 내부 PNG 상대 경로이며 절대 경로·상위 경로·XML 문자는 거부합니다. 호출자가 해당 PNG를 함께 내보내야 합니다. 이 값은 `info.xml`의 `PreviewImage`에만 반영되어 본체 캐릭터 선택창에서 사용되며 행동 프레임을 바꾸지 않습니다. 모드 목록이나 Steam 썸네일 설정은 아닙니다.

### 설치된 LUMI 행동 템플릿

`CharacterTemplates.behaviors()`는 설치된 Lumi XML의 행동 그래프에서 `Template(id,displayName,frames)`를 읽습니다. `TemplateFrame(slot,image,durationMs,anchorX,anchorY)`의 slot은 불투명 포즈 식별자, image는 `lumi:shime1.png` 형태의 본체 참조입니다. `CharacterTemplates.image(reference)`는 최대 4 MiB PNG 원본을 반환합니다. 경로를 외부로 노출하지 않으며 잘못된 참조는 IllegalArgumentException, 본체 콘텐츠 누락은 IllegalStateException입니다. 리소스 풀이나 배포 PNG에 복제하지 마세요.

`CharacterDefinition.Frame`의 마지막 `templatePose`, `Behavior`의 마지막 `templateId`는 이전 생성자에서는 빈 문자열입니다. 템플릿 내보내기는 원본 조건·복합 액션·속도·핫스팟·다음 행동을 보존하고 포즈만 교체합니다. 원본 포즈 키가 빈 새 프레임은 앞 프레임의 애니메이션(맨 앞이면 첫 애니메이션)에 추가됩니다. 각 애니메이션 안의 순서는 입력 순서이며 조건 애니메이션 사이의 이동은 하지 않습니다. 변경하지 않은 Duration 표현식은 그대로 보존합니다. 출력 `@lumi/` 이미지는 본체가 설치된 Lumi 파일로 해석하며 PNG를 패키지에 복사하지 않습니다. 최대 512행동, 16384프레임입니다.

`AuthoringPreview.Frame(png,durationMs,anchorX,anchorY,hostImage,templatePose)`와 `Behavior(name,kind,frames,templateId)`는 같은 계약으로 실제 실행기에서 시험합니다. hostImage가 있으면 PNG 바이트 대신 본체 참조를 사용합니다. 이전 생성자는 유지됩니다. 설치·승인·게시 동작은 없습니다.

`CharacterTemplates.reservedIds()`는 원본 행동 및 보조 액션 ID 집합입니다. 자신의 templateId와 일치하는 ID 및 동일 kind의 기존 7개 기본 역할을 제외한 이름 충돌은 내보내기에서 거부합니다. 표시 이름과 별개인 실행 ID 편집 시 이 집합을 예약하세요.

같은 `templateId`의 여러 실행용 별칭은 kind와 전체 프레임 정의가 동일할 때 허용합니다. 충돌하는 덮어쓰기는 거부합니다. 예를 들어 사용자 ID `Idle`과 엔진 역할 `ModMakerIdle`은 같은 `StandUp` 템플릿을 공유할 수 있습니다. 실시간 미리보기에서도 PNG 내용을 공유하며 원본 복합 동작을 유지합니다.

### 필수 행동과 행동 효과음

`CharacterTemplates.requiredBehaviorIds()`는 엔진 필수 실행 ID `ChaseMouse`, `Fall`, `Dragged`, `Thrown`을 반환합니다. `Template.required()`도 같은 분류를 제공합니다. 제작기의 기본 대기 역할은 이 집합과 별개이며 이전 생성자는 유지됩니다.

`CharacterDefinition.Behavior(name,kind,frames,templateId,sound)` 및 `AuthoringPreview.Behavior`의 같은 생성자에서 sound는 `sound/<modid>/<file>.wav` 패키지 상대 경로입니다. null 또는 빈 문자열은 기본 템플릿 효과음을 유지합니다. 지정한 WAV는 원본 포즈 소리보다 우선하여 행동에 진입한 뒤 첫 재생 틱에 한 번 울리며 같은 행동의 애니메이션 반복으로 다시 울리지 않습니다. 행동을 새로 시작하면 다시 재생할 수 있습니다. 소리 끄기·개별 음소거·전체 음량 설정은 기존 효과음과 같습니다.

생성 결과에 `img/<imageSet>/conf/behavior-sounds.properties`가 추가될 수 있습니다. 이 파일도 반환된 다른 파일과 함께 내보내야 합니다. 원본 XML·이동·조건·포즈 Sound는 변경하지 않으므로 override를 제거하면 원래 효과음으로 돌아갑니다. 별칭은 효과음까지 동일해야 같은 템플릿을 공유할 수 있습니다. 실시간 미리보기의 WAV 바이트는 기존 `Snapshot.audio` 맵에 같은 키로 제공하며 참조 누락은 거부합니다. 편집기에서 삭제된 선택적 WAV는 override를 전달하지 않아 기본 효과음을 유지할 수 있습니다. 패키지에 WAV가 실제로 없으면 소리 재생만 실패하며 캐릭터 동작은 계속됩니다.

`Template.hasDefaultSound()`는 해당 설치본 행동의 도달 가능한 액션 그래프 전체에서 비어 있지 않은 `Pose.Sound`가 있는지 나타냅니다. 조건별 애니메이션도 포함하며, 값이 false인 행동은 기본 소리가 없는 것으로 표시할 수 있습니다. 파일의 존재·디코딩 성공이나 현재 음소거 설정을 뜻하지 않습니다. 이전 수동 생성자는 소리 메타데이터를 모르는 경우 false로 유지하고, `behaviors()`의 결과가 설치본의 정본입니다.

### 안내 에셋이 없는 최종 내보내기

`generateStandaloneBehaviors(imageSet, behaviors, previewImage)`는 최종 패키지용입니다. 작성한 행동의 액션·조건 그래프와 `behaviorNames()`의 7개 실행 역할만 유지합니다. 작성하지 않은 역할은 자신의 기본 대기 프레임으로 보완하며 Stand/Walk를 임의의 자율 행동으로 추가하지 않습니다. 작성하지 않은 다음 행동 참조는 기본 대기 또는 작성한 템플릿의 실행 ID로 연결하고, 연결할 대상이 없는 핫스팟은 제거합니다.

작성 프레임·썸네일의 `lumi:` 이미지 참조와 활성 그래프에 남은 기본 Lumi 소리는 최종 내보내기에서 거부합니다. 기본 소리가 있으면 사용자 WAV를 연결하거나 명시적으로 무음으로 설정해야 합니다. 기존 `generateBehaviors`는 편집·가이드 미리보기 호환을 위해 그대로 유지됩니다.

`CharacterDefinition.Behavior` 및 `AuthoringPreview.Behavior`의 여섯 번째 인자는 `boolean muteDefaultSound`이며 기존 생성자는 false입니다. 빈 sound와 true의 조합은 기본 소리를 끕니다. 이때 매핑 파일의 빈 값은 명시적 무음을 뜻하고, 키가 없으면 기본 소리를 유지합니다. 최종 생성 결과는 꼬미 원음 참조를 제거하지만 해당 행동의 이동·조건·프레임 설정은 보존합니다. 삭제된 선택적 사용자 WAV를 무음으로 내보내려면 이 플래그를 사용하세요.

### 공유 기본 액션 제작 — 새 25Hz API

`CharacterTemplates.actions()`는 `ActionTemplate(id,displayName,frames,hasDefaultSound)` 목록입니다. named Action이 직접 소유한 Animation만 읽으며 Sequence의 하위 시나리오를 펼치지 않습니다. 따라서 Walk·Run·Stand·Sit 등의 프레임 정의는 한 곳입니다. 프레임 슬롯은 `Action/animationIndex/poseIndex`, 시간은 실제 엔진의 25Hz(40ms/틱)입니다. `CharacterDefinition.actionTickDurationMs(ms)`로 표시·미리보기 시간을 같은 틱으로 맞출 수 있습니다. 구 Behavior API의 60 기준 변환은 호환을 위해 그대로 유지되며 새 제작기는 이 액션 API를 사용합니다.

`CharacterDefinition.Action(name,templateId,frames,sound,muteDefaultSound)`와 `generateActions(imageSet,actions,previewImage)`는 공유 액션을 편집합니다. 기본 작성 액션은 name `Idle`이며 꼬미 원본을 쓰면 templateId `Stand`입니다. templateId가 빈 커스텀 액션은 독자적인 정지 포즈로 생성합니다. 모든 액션이 커스텀인 프로젝트는 설치된 Lumi XML 없이 생성할 수 있습니다. `generateStandaloneActions`는 작성한 액션의 안내 이미지·소리를 거부하고, 작성하지 않은 원본 액션/인라인 포즈는 자체 Idle 이미지와 무음으로 보완하여 기존 시나리오의 이동·조건을 유지합니다. 빠진 7개 실행 역할도 자체 대기로 보완합니다.

원본 Action ID는 바꾸지 않으므로 Walk를 한 번 편집하면 WalkLeftAlongFloorAndSit·WalkAlongWorkAreaFloor 등의 모든 참조에 반영됩니다. 새 액션 ID로 호출할 수 있는 Behavior도 만듭니다. 새 호출 진입점은 별도 wrapper에만 기본 이동 목표와 최대 100틱을 제공하고 끝나면 Idle로 돌아갑니다. 같은 이름의 기존 기본 Behavior는 원래 문맥을 유지합니다. 원본 시나리오의 ActionReference 파라미터는 변경하지 않습니다. 바닥·벽·천장 등 원본 액션의 실행 환경 조건도 유지됩니다.

효과음은 `conf/action-sounds.properties`로 물리 액션 ID에 연결하며 동일 Behavior 안에서 같은 Action이 재진입해도 한 번씩 재생합니다. 같은 클립이 아직 재생 중이면 새 진입 시 처음부터 재생합니다. 빈 값은 명시적 무음, 없는 키는 원래 포즈 소리입니다. `AuthoringPreview.Action`은 같은 필드를 사용하며 `Snapshot.forActions(actions,document,audio)`로 실시간 시험합니다. 기존 Frame 필드와 audio 맵 키는 그대로입니다. 레거시 behaviors와 새 actions를 한 Snapshot에 동시에 넣을 수 없습니다.

새 프레임의 slot이 비어 있으면 직전 원본 Animation(첫 프레임이면 첫 Animation)에 넣고, 해당 Animation의 원본 포즈를 바탕으로 Velocity 등 이동 파라미터를 유지합니다. 기존 슬롯을 다른 조건 Animation으로 옮기는 순서는 거부합니다.

### 프레임 효과음 — 공유 액션 API

`TemplateFrame`은 기존 5개 필드 뒤에 `sound`와 `volumeDb`를 제공합니다. 원음 참조는 `lumi:<상대경로>.wav`, 없으면 빈 문자열입니다. `CharacterTemplates.sound(reference)`는 설치본 `img/Lumi/sound` 아래 WAV를 4 MiB까지 읽습니다. 임의 경로·상위 이동은 거부하며 잘못된 참조는 IllegalArgumentException, 파일 읽기 실패는 IllegalStateException입니다. 안내 원음은 사용자 리소스에 복사하지 마세요.

`CharacterDefinition.Frame`의 기존 5개 필드 뒤, `AuthoringPreview.Frame`의 기존 6개 필드 뒤에 `sound`, `muteDefaultSound`, `loopSound`가 추가됩니다. 이전 생성자는 빈 sound/false/false입니다. 사용자 sound는 `sound/...wav` 패키지 경로이며 빈 값은 원음을 상속합니다. mute는 소리를 제거합니다. 기본 once는 프레임 진입마다 한 번 재생하며 애니메이션이 다시 돌아오면 다시 재생합니다. loop는 프레임 동안 반복하고 프레임·애니메이션·행동 이탈 또는 캐릭터 제거 때 중지합니다. once의 잔향은 다음 프레임에서도 끝까지 재생하되 캐릭터별 최대 16개로 제한합니다. 캐릭터마다 Clip을 소유해 서로의 소리를 끊지 않습니다.

`frame-sounds.properties`의 `<cue>.path`·`<cue>.loop`가 XML의 `Sound="@frame/<cue>.wav"`와 연결됩니다. 이 파일도 함께 내보내세요. 원음의 Volume은 유지하고 사용자 WAV 교체는 0 dB를 사용합니다. 기존 액션 단위 효과음/무음이 지정되면 그 설정이 우선합니다. 새 제작기는 프레임 설정을 사용합니다.

직접 Animation 안의 이미지·반전 이미지·기준점·이동 등 속성이 같은 양의 정수 tick 구간은, 뒤 Pose에 새 Sound 선언이 없을 때 논리 프레임 하나로 표시합니다. 범위 slot은 `Action/animationIndex/start-end`입니다. Walk는 4개×240ms, Run은 4개×120ms이며 소리 시작 위치는 유지합니다. 새 XML은 이 논리 프레임당 Pose 하나를 내보내며 별도 진입 정책으로 기존 1 tick 요청+무음 hold 분할 없이 재트리거를 막습니다. 기존 단일 slot도 읽습니다. 최종 내보내기의 안내 PNG/WAV 금지 원칙은 동일합니다.

프레임의 마지막 선택 인자 `Double soundVolumeDb`는 내장 WAV를 사용자 리소스로 포함할 때 원래 음량을 보존합니다. 유한한 -80..24 dB만 허용합니다. `null`은 기존 정책(원음 음량 상속, 사용자 WAV는 0 dB)을 유지하며 기존 생성자는 호환됩니다.

## 공통 모션 제작
`CharacterTemplates.motions()`는 실제 Behavior 진입점에서 도달 가능한 실행 액션을 SDK의 의미별 바인딩 정본으로 묶습니다. `MotionTemplate(id,displayName,frames,bindings,hasDefaultSound)`의 첫 binding이 대표 애니메이션입니다. 미사용 액션은 제외되며 나머지 사용 액션도 단일 binding 모션으로 제공합니다. Jump는 Jumping, FaceGlide는 FaceGlideMid, Land는 SoftLandSilent가 대표입니다.
`CharacterDefinition.Motion(name,templateId,frames)`를 `generateMotions(imageSet,motions,previewImage)` 또는 `generateStandaloneMotions(...)`에 전달합니다. templateId는 모션 ID이며 빈 값은 커스텀, name=Idle은 필수입니다. 프레임 slot은 대표 native 액션의 slot을 유지합니다. 편집 이미지·시간·기준점·효과음·음량은 모든 binding에 동일하게 적용되고, 원본 액션의 클래스·목표·중력·참조 Duration·조건·프레임 이동값은 각 실행 액션에 남습니다. 역할별 원래 효과음 차이도 대표 효과음으로 통일합니다. 프레임을 추가하면 해당 애니메이션의 첫 물리 pose를 사용합니다. 조건부 애니메이션을 가로지른 순서 변경은 거부합니다.
`AuthoringPreview.Motion(name,templateId,frames)`와 `Snapshot.forMotions(motions,document,audio)`는 같은 설정으로 실시간 시험합니다. `Snapshot.motions()`로 읽으며 actions/behaviors와 혼합할 수 없습니다. 모션 name으로 callable Behavior를 생성하므로 이벤트가 모션을 직접 선택할 수 있습니다. 기존 Action/Behavior API와 생성자는 유지됩니다. 이미지·음성 가이드 참조는 미리보기에서만 허용하고 최종 내보내기에서는 교체·포함 또는 음소거해야 합니다.

Walk는 Walk/ChatApproach/LeadParade/JoinParade/GatherJoin/HiFiveApproach/GiftApproach, Dash는 Dash/CreditDash/TagDashLeg/TagChase에 공통 적용합니다. 대표 Walk/Dash의 효과음도 공유하므로 원래 무음인 실행 장면에 발소리가 추가되는 것은 의도된 정책입니다.

### 효과음 재생 범위
`CharacterTemplates.SoundPlayback`은 `FRAME_ENTRY`(프레임/애니메이션 주기 진입마다)와 `BEHAVIOR_ENTRY`(한 행동 실행 중 같은 논리 음원은 한 번)를 제공합니다. `TemplateFrame` 마지막 인자 `soundPlayback`은 원본 SoundGate에서 발소리 등 반복 허용 음원은 FRAME_ENTRY, pinch 등 비반복 음원은 BEHAVIOR_ENTRY입니다. 슬롯 `원본액션/애니메이션번호/자세번호`의 애니메이션 번호가 다르면 순차 프레임이 아닌 조건부 분기일 수 있습니다. Pinched의 29개 분기는 하나의 행동에서 선택되는 대안입니다.
`CharacterDefinition.Frame`과 `AuthoringPreview.Frame`에 마지막 선택 인자 `CharacterTemplates.SoundPlayback soundPlayback`을 추가했습니다. null은 대표 원본 정책을 상속하고 커스텀 프레임은 FRAME_ENTRY를 사용합니다. 명시 값은 상속을 덮어씁니다. loopSound=true는 이 범위보다 우선합니다. 내장 WAV를 리소스로 포함할 때 음량과 함께 이 값을 프로젝트에 보존하고 전달해야 합니다.
프레임 sidecar의 `.scope`가 이 값을 보존합니다. 과거 sidecar에 scope가 없으면 FRAME_ENTRY입니다. BEHAVIOR_ENTRY는 캐릭터별 실제 행동 episode와 `.path` 논리 음원을 기준으로 하므로 분기·1프레임 반복으로 재생하지 않고, 놓았다 다시 잡으면 재생합니다. 종료된 세션과 다른 캐릭터의 gate는 공유하지 않습니다. 스톡 캐릭터의 기존 SoundGate 정책은 수정하지 않습니다.

## 최소 캐릭터 실행 프로필
`CharacterTemplates.limitedMotions()`는 Stand(Idle), Walk, Run, Jump, Falling, Land, Pinched, Sit, Sprawl만 제공합니다. Pinched는 조건부 29분기가 아닌 중립 `Pinched/14/0` 한 자세입니다. `generateLimitedMotions`/`generateStandaloneLimitedMotions(imageSet,motions,previewImage)`와 `AuthoringPreview.Snapshot.forLimitedMotions(motions,document,audio)`를 사용합니다. `Snapshot.limitedRuntime()`으로 프로필을 확인합니다. 기존 모든 생성 경로는 변경하지 않습니다.
|모션|자동 사용|작성하지 않았을 때|
|---|---|---|
|Idle|필수 바닥 대기|생성 거부|
|Walk/Run/Sit/Sprawl|작성된 항목만 바닥 조건 자율 후보와 명시 호출|후보 제외|
|Jump|바닥에서 명시 이벤트 호출만|제외|
|Falling|추락/던져짐|Idle 그림의 무음 낙하|
|Land|추락/던져짐 종료 후 착지|바로 Idle|
|Pinched|실제 집기; 저항으로 자동 탈출하지 않음|Idle 그림의 무음 집기|
|커스텀|ready/click/menu 명시 호출; 프레임 총 길이 1회 후 Idle|무작위 실행하지 않음|
내보내기는 원본 꼬미 시나리오 그래프를 포함하지 않습니다. 미작성 선택 기능이 Idle 그림으로 몰래 실행되지 않으며 필수 Fall/Thrown/Dragged/ChaseMouse와 이전 역할 이름은 안전한 자체 그림 별칭으로 충족합니다. `character-profile.properties`를 다른 생성 파일과 함께 패키지에 포함해야 합니다. 동일 모션 실행 중 재요청은 버리고, 집기·추락 중 모션 이벤트는 대기열 없이 거부합니다. 집기·추락이 시작되면 대기 중 사용자 요청도 취소합니다. 별도의 idle-random 이벤트는 제공하지 않습니다.
실제 배포의 ready는 각 캐릭터가 처음 정상 착지한 뒤 한 번 발행합니다. 모드 종료·비활성화 시 관찰 작업을 해제하고 제거된 캐릭터 상태를 정리합니다. 제한 프로필 미리보기는 바닥 Idle에서 시작하므로 ready를 바로 시험할 수 있습니다. 이 생명주기 준비 처리는 모션 요청 대기열이 아닙니다.

명시 이벤트로 Pinched를 선택하면 그림을 한 번 재생하는 유한 모션입니다. 실제 잡기 물리는 별도 내부 액션이 동일 애니메이션을 사용하며, 이벤트 호출이 마우스를 붙잡지 않습니다. 요청한 내장 템플릿이 설치본에 없으면 커스텀으로 조용히 대체하지 않고 오류를 반환합니다.

## 종료 이벤트 (`exit`)
시각 규칙의 event는 `ready`, `click`, `exit`, `menu:<id>`를 지원합니다. exit는 캐릭터 제거 및 정상 앱 종료 **전** 해당 캐릭터에게 한 번 발생합니다. 조건 불충족·빈 반응은 지연 없이 제거합니다. 모션·대사·음성이 있으면 모든 캐릭터가 동시에 최대 2초의 반응 단계를 거친 뒤 정리합니다. 명시 모션은 최소 캐릭터 프로필이 생성한 위치 고정 시각 액션으로 재생하므로 공중·집기 중에도 안전합니다. 긴 모션/음성은 2초에서 잘립니다. behavior가 빈 값이면 현재 모션의 신원·시간·일시정지 상태를 유지하며 새 모션 명령을 내리지 않습니다.
`AuthoringPreview.Session.dispatch("exit")`는 즉시 반환하며 실제 반응 후 `state().closed()`가 true가 됩니다. 종료 중 replace/추가 dispatch는 무시합니다. Session.close·미리보기 교체·모드 비활성화는 exit를 발생시키지 않고 즉시 취소/정리합니다. 중복 제거/종료 요청은 동일한 반응 Future를 공유합니다. 준비·UI가 막혀도 별도 deadline이 제거 상태를 확정하며 EDT에서 기다리지 않습니다. 물리 스피커 장치 실패 시 음성만 생략하고 유효한 모션/대사는 유지합니다. 참조 WAV가 없고 다른 효과도 없으면 지연하지 않습니다.
|종료 경로|동작|
|---|---|
|캐릭터 제거|해당 캐릭터 exit 반응 후 제거|
|Main.exit 직접 종료/재시작|플러그인·ticker 정지 전 사용자 exit 반응 단계|
|일반 메뉴 종료(exitWithFarewell)|사용자 exit 대상만 기존 작별에서 제외; 사용자 반응과 나머지 꼬미 작별이 병렬 실행|
|꼬미만 실행 중|기존 작별 동작·음성·시간 그대로 유지|
혼합 실행에서 사용자 반응은 2초 상한이고 앱 전체 종료는 기존 꼬미 작별 시간과 OS 정리 시간도 포함합니다. 음성은 기존 마이크 차단·공유 음성 채널 정책을 유지하므로 여러 캐릭터의 음성 동시 출력은 보장하지 않습니다. 모션과 말풍선은 캐릭터별로 표시합니다. 강제 프로세스 종료·충돌·OS 강제 종료의 반응은 보장하지 않습니다.

### 기본 자동 애니메이션 제작

`CharacterTemplates.automaticAnimations()`는 Stand(Idle), Walk, Run, Jump, Falling, Land, Pinched, Sit, Sprawl, GrabWall, ClimbWall, GrabCeiling, ClimbCeiling을 제공합니다. 사용자는 프레임 이미지·시간·기준점·효과음만 작성하고 실행 조건과 이동은 엔진이 관리합니다. 벽·천장은 창과 작업영역 지형을 사용합니다. 잡힘은 중립 자세, 벽 애니메이션은 한 가지 기본 자세로 편집합니다. 벽 잡기·오르기의 기준점은 화면 끝 벽을 기준으로 편집하며, 창 옆면에서는 원본 템플릿의 창/화면 기준점 차이를 추가합니다. 작성한 기준점 수정량은 두 경우에 동일하게 유지됩니다. 왼쪽·오른쪽 방향의 반전과 현재 접촉 중인 창 판정은 엔진이 처리합니다.

`CharacterTemplates.automaticAnimationIds()`는 설치 에셋을 읽지 않고 지원 ID 목록을 반환합니다. 기본 13종에 HardLand, Stunned, Bouncing, GetUpHop, LookAround, TiltHead, SitAndDangleLegs, AlarmNotice, Farewell, HappyHop, HeadPat을 더해 24종입니다. 프레임 슬롯과 bindings는 SDK 소유 식별자이며 호출자는 해석하거나 네이티브 XML 구조에 대입하지 않습니다.

- 낙하·던져짐: 바닥에 닿은 낙하 높이가 550을 넘으면 등록된 HardLand → Stunned → GetUpHop을 순서대로 실행합니다. 이 세 항목 중 하나도 없으면 다음 갈래를 검사합니다. 높이가 320을 넘고 Bouncing이 있으면 같은 애니메이션으로 두 번 튑니다. 나머지는 Land, 미등록이면 Idle입니다. 높이는 엔진 좌표를 사용하며 사용자가 임계값을 설정하지 않습니다. 벽에 붙은 상태는 착지로 처리하지 않습니다.
- LookAround, TiltHead, SitAndDangleLegs, HappyHop은 바닥의 자동 선택 후보입니다. HappyHop은 같은 시각 타임라인으로 두 번 뛰며 앱의 호감도 상승·바로가기 동작 요청도 받습니다. GetUpHop/Bouncing은 프레임 수나 재생 길이에 관계없이 엔진의 도약·착지 물리를 따릅니다.
- AlarmNotice는 알람·정각 알림 요청에만 연결됩니다. 등록된 캐릭터에만 유지 시간을 부여하고, 벽·천장에서는 먼저 낙하한 뒤 재생합니다. 짧은 프레임 열도 알림 유지 중 계속 순환하며 사용자 조작과 앱의 종료 규칙을 따릅니다.
- Farewell은 기존 정상 종료 인사 과정에 연결되며 일반 Chatter 요청과 분리됩니다. 집중·화면 점유 등 기존 앱의 인사 생략 규칙과 종료 시한을 유지합니다. 공중에서도 사용자 그림으로 재생하며 진행 중인 알람에 가려지지 않습니다. 미등록이면 종료 애니메이션을 추가하지 않습니다.
- HeadPat은 그림 상단 중앙(가로 1/6–5/6, 세로 위 절반)을 자동 입력 영역으로 사용합니다. 문지르기·길게 누르기는 기존 앱이 판정하며 방향별 파라미터를 작성하지 않습니다. 등록한 한 타임라인을 유지하고 손을 뗀 뒤 HappyHop이 있으면 사용하고, 없으면 Idle로 돌아갑니다. 몸통 클릭은 기존 들기 동작을 유지합니다.

조건식·이벤트 편집이나 런타임 플러그인을 내보내지 않습니다. 미등록 선택 애니메이션을 꼬미 가이드로 보완하지 않으며 기존 일반/limited 캐릭터와 꼬미의 동작 규칙은 유지합니다. 다른 캐릭터, 정원·봉고·게임·변신, 마우스 추적 전용 경로, 창 던지기 같은 특수 조건 동작은 목록에 포함하지 않습니다.


`CharacterDefinition.generateAutomaticAnimations(imageSet,animations,previewImage)`와 `generateStandaloneAutomaticAnimations(...)`는 기존 `Motion`/`Frame` DTO를 받습니다. Idle은 필수이며 templateId는 비어 있거나 Stand입니다. 나머지는 제공 카탈로그의 ID와 name/templateId가 같아야 합니다. 커스텀 시나리오는 거부합니다. 미작성 선택 항목은 실행하지 않고, 낙하·잡힘 누락은 무음 기본 대기로 보완합니다. 프레임 개수/그림 교체가 이동을 막지 않도록 걷기·달리기·벽/천장 이동 속도는 엔진이 설정합니다. 바닥 후보는 지상에서, 벽·천장 후보는 해당 경계에서만 선택하며 공중에서는 낙하로 복귀합니다.

결과는 actions.xml/behaviors.xml/info.xml과 character-profile.properties, 필요한 프레임 효과음 메타데이터입니다. 이벤트·대사 로직과 종료 애니메이션 별칭을 만들지 않습니다. standalone은 꼬미 가이드 이미지를 거부합니다. `AuthoringPreview.Snapshot.forAutomaticAnimations(animations,audio)`는 동일 프로필을 실제 엔진에서 시험하며 이벤트 문서를 받지 않습니다. `Snapshot.automaticRuntime()`으로 구분합니다. 기존 생성기와 플러그인 이벤트/대사 API는 유지됩니다.

자동 생성기의 `imageSet`은 프로젝트 폴더명으로 사용할 수 있습니다. `CharacterDefinition.validAutomaticImageSetName(name)`은 1~64 UTF-16 코드 단위의 유효한 Windows 단일 폴더명을 검사합니다. 한글·공백·유효한 유니코드·허용된 문장부호는 원문 그대로 유지하며 trim, 대소문자 변경, 유니코드 정규화를 하지 않습니다. 빈 이름, 경로 구분자, `<>:"/\\|?*`, 제어문자, 잘못된 서로게이트, XML에서 허용하지 않는 U+FFFE/U+FFFF, 끝 공백/점, 캐릭터 선택기가 숨기는 선행점 이름, Windows 장치 예약명(확장자와 COM/LPT의 상첨자 숫자 변형 포함), Lumi/Credit/Unused는 거부합니다. 생성 함수는 동일 규칙으로 검증하며 info.xml의 표시명은 XML 텍스트로 이스케이프합니다. 이 이름은 `img/<imageSet>/` 디렉터리명이며 플러그인·모드 식별자의 ASCII 계약은 바뀌지 않습니다. 기존 일반/limited 생성기도 기존 이름 계약을 유지합니다.


### 프로젝트 이름을 사용하는 효과음 경로

`CharacterDefinition.validSoundPath(path)`는 `sound/` 아래의 상대 WAV 경로를 검사합니다. `sound/고양이 & 친구/effects/step.wav`처럼 한글·다른 Unicode 문자·공백을 포함한 폴더와 파일 이름을 허용합니다. 기존 영문 경로와 프레임의 재생/반복/음량 정책은 유지됩니다. `CharacterDefinition` 및 `AuthoringPreview`의 프레임·액션·행동 입력과 본체의 프레임/액션/행동 효과음 표는 같은 검증 규칙을 사용합니다.

절대 경로, 빈 경로 조각, `.`/`..` 조각, 역슬래시, 제어 문자, 잘못된 UTF-16, Windows 금지 문자·장치 예약 이름·끝의 점/공백은 거부합니다. 문법 검사 통과는 파일 접근 승인이 아니며 기존 모드 리소스 해석·검증을 그대로 거칩니다. 실시간 시험의 audio 맵 키도 프레임의 전체 상대 경로와 일치해야 합니다.


### 기본 활동·상호작용 확장 (45종)

기존 24종 외에 Dash, SprintBrake, FallingThrown, FaceGlide, WindowCarry, WindowThrow, Parade, ParadeBump, HiFivePrepare, HiFive, Talk, Humming, Backstep, GatherCall, GatherSit, Tag, GiftHold, GiftThrow, GiftAwait, GiftCatch, Flying을 제공합니다. 등록한 슬롯에 따라 SDK가 앱의 고정 활동과 역할을 생성하며 제작자가 조건식·상대·이벤트를 작성하지 않습니다.

- Dash/SprintBrake: 전력질주와 급정지. Run이 있고 SprintBrake를 등록하면 달리기 종료에도 연결합니다. FallingThrown은 실제 Thrown 물리의 그림을 교체하며 마우스 놓기의 이동량을 유지합니다.
- FaceGlide: 창 앞면 오르기/미끄러지기와 창 위로 올라서기. WindowCarry/WindowThrow는 앱의 창 던지기 설정과 유효한 창 조건을 그대로 사용합니다. 들고 있는 창의 기본 오프셋은 작성 프레임 기준점 크기에 맞춰 계산합니다.
- Parade/ParadeBump, HiFive/HiFivePrepare, Talk/Humming/Backstep, GatherCall/GatherSit, Tag, GiftHold/GiftThrow/GiftAwait/GiftCatch는 각각 행렬·하이파이브·대화 몸짓·모여 앉기·술래잡기·선물의 고정 실행 흐름을 켭니다. 하나의 관련 슬롯만 등록해도 실행 구성이 완성되며 미작성 준비/이동 자세는 등록된 공통 그림 또는 무음 Idle로 처리합니다. 기본 Lumi 이미지·소리를 출력에 끼워 넣지 않습니다. 새 자동 활동은 집중 모드를 따릅니다.
- 같은 이미지셋의 캐릭터만 서로 연결합니다. 엔진의 접근/접촉 역할과 선물의 주는 쪽/받는 쪽을 구분하며 다른 세트의 소품/받기 요청과 섞이지 않습니다. 발견·줍기용 크레딧 행동과 변신 분기는 생성하지 않습니다. 선물용 소품만 앱의 기본 Credit 자원을 런타임에 사용하며 모드에 복사하지 않습니다.
- 선물 수신 후에는 반복 점프 대신 소리 없는 기본 대기로 다음 전달을 기다리고, 후속 선물이 없으면 종료 반응으로 넘어갑니다. 공중 상태에서 받기·종료 요청이 거절되면 실행 가능한 시점까지 보관합니다. 이 규칙은 제작한 자동 애니메이션 캐릭터에 적용하며 기본 Lumi의 선물 동작은 바꾸지 않습니다.
- Talk는 앱의 말하기 요청도 받습니다. Flying은 앱의 위치 이동 중에만 재생하고 이동이 끝나면 다음 동작으로 바뀝니다. 물리 상태·상호작용 중 입 움직임 요청이 활동을 중단하지 않습니다.
- 프레임 수·길이가 급정지 거리나 하이파이브의 상승/착지 경로를 바꾸지 않습니다. 상호작용 준비 시간, 접촉 점프, 창 던지기 등 실행 시간은 엔진의 상태에 따르며 그 안에서 작성한 프레임 시간으로 재생합니다. 실제 상태가 끝나면 긴 프레임 열도 종료될 수 있습니다.
- `forAutomaticAnimations` 실제 앱 미리보기는 행렬/모여 앉기에 3마리, 그 밖의 상호작용에 2마리, 일반 프로젝트에 1마리의 임시 캐릭터를 소유합니다. 상태의 mascotId는 대표 캐릭터입니다. 교체/닫기/소유자 종료에서 동료와 그 선물 소품을 정리하고, 동료만 닫아도 세션 편집은 계속 가능합니다. 일반 앱 캐릭터 조회/도전과제에는 미리보기 캐릭터를 포함하지 않습니다.

슬롯의 그림·효과음·시간·기준점만 편집합니다. 생성된 내부 행동 이름/연결·호출 메타데이터를 사용자 입력이나 별도 스크립트로 관리하지 마세요. 잡힘 회전/강도별 슬롯과 크레딧 발견·줍기는 이 확장에 포함하지 않습니다.

<a id="animation-activities"></a>

## 연계 애니메이션 조회와 전체 활동 시험

`AnimationActivities.forAnimation(animationId, registered)`는 선택한 애니메이션이 쓰이는 활동 목록을 반환합니다. `registered`에는 현재 작성한 애니메이션 ID를 넣습니다. `Idle`과 기본 템플릿의 `Stand`는 같은 기본 대기로 취급합니다. 이 조회는 에셋을 가져오거나 프로젝트에 애니메이션을 등록하지 않습니다.

- `Activity(id, roles, related, participantCount, available)`는 활동 ID, 역할별 단계, 함께 쓰는 자세, 필요한 전체 캐릭터 수를 제공합니다. `available`은 등록 조합으로 활동을 만들 수 있는지를 뜻하며 실제 지형이나 상대 상태를 보증하지 않습니다.
- `Role(id, steps)`는 한 역할의 논리적 단계 순서입니다. 이동·접촉·조건 분기·반복은 본체가 처리하므로 이 목록을 고정 시간표나 임의의 행동 호출 스크립트로 해석하지 않습니다.
- `Step(id, animationId, effectiveAnimationId, muted, skipped)`는 편집 슬롯과 실제 사용할 그림의 슬롯을 구분합니다. 대체 우선순위는 본체 생성기와 공유합니다. `skipped`이면 단계가 생략되고 `effectiveAnimationId`는 비어 있습니다. `muted`는 본체가 강제로 무음 처리하는 경우이며 작성자가 설정한 음소거와 별개입니다.

자동 애니메이션 시험 세션의 `startActivity(activityId)`는 그 세션 소유 캐릭터와 동료를 이용해 실제 엔진의 활동을 시작합니다. `pauseActivity(true/false)`로 일시정지/계속, `stopActivity()`로 중지합니다. 모두 EDT 밖에서 호출합니다. `activityState()`의 `ActivityState(activityId, phase, actors, reason)`을 읽어 현재 상태를 표시합니다. `ActivityActor(mascotId, roleId, animationId, stepId)`는 실제 실행 중인 역할과 단계를 알려주며, 알려진 단계 밖이면 빈 ID가 올 수 있습니다. 시간 경과만으로 다음 단계가 성공했다고 추정하지 않습니다.

상태는 `IDLE`, `RUNNING`, `PAUSED`, `COMPLETED`, `CANCELLED`, `FAILED`입니다. `COMPLETED`만 정상 완료이며 조건 부족·상대 부재·중단·시간 초과는 구분합니다. 세션 교체·닫기·플러그인 종료에서는 활동과 세션 소유 소품/효과음을 정리합니다. 일반 캐릭터나 다른 세션의 행동, 사용자의 기능 설정은 바꾸지 않습니다. 이 API는 임의의 본체 행동 이름이나 코드 실행을 받지 않습니다.

```java
var activities = AnimationActivities.forAnimation("HiFive", registeredIds);
var activity = activities.stream().filter(AnimationActivities.Activity::available).findFirst();
if (activity.isPresent()) session.startActivity(activity.get().id());
var state = session.activityState();
```

`AnimationActivities`와 활동 시험 API는 실험 API입니다. 기존 `Session` 구현에는 기본 메서드가 제공되며 지원하지 않는 구현의 제어 호출은 `UnsupportedOperationException`으로 거부됩니다. 실제 기능의 존재를 확인하거나 대응하는 본체와 함께 사용합니다.

활동 시험은 명시적으로 시작할 때만 별도의 제어된 캐릭터 구성을 준비합니다. 일반 앱 미리보기의 자동 활동은 그대로 유지합니다. 시험 중에는 임시 캐릭터가 다른 자동 모집에 끼어들지 않습니다. 중지·완료 후에는 세션 캐릭터를 기본 대기에서 멈추며, 다음 활동 시작이나 스냅샷 교체로 다시 진행합니다. 선물 받기 후 대기·종료는 일반 자동 애니메이션 캐릭터와 동일한 유한 흐름을 사용하고, 쓰다듬기는 작성한 프레임 열을 한 번 실행한 뒤 손을 뗀 후속 동작으로 넘어갑니다. 일반 캐릭터의 반복/입력 규칙은 변경하지 않습니다. 시험이 90초 안에 끝나지 않으면 `FAILED`로 보고합니다.

<a id="held-motion"></a>

## 잡힘 물리 회전 — `CharacterDefinition.HeldMotion` 실험

`generateAutomaticAnimations(imageSet,animations,previewImage,HeldMotion)`와 `generateStandaloneAutomaticAnimations(imageSet,animations,previewImage,HeldMotion)`에서 `PHYSICS`를 명시하면 해당 캐릭터만 연속 회전 잡힘을 사용합니다. `LEGACY`와 기존 3인자 호출은 기존 스프라이트 실행과 생성 파일 바이트를 유지합니다. null은 거부합니다. 본체 기본 Lumi, 다른 이미지셋과 다른 개체의 흔들림 상태는 변경하지 않습니다. 출력의 물리 실행 클래스와 `character-profile.properties`의 `heldMotion=PHYSICS`가 선택을 보존하므로 제작기를 종료해도 동작합니다. 이 계약을 구현한 본체가 필요하며 같은 제품 버전 문자열만으로 지원을 판단하지 마세요. 이전 본체에서 물리 출력을 레거시로 조용히 대체하지 않습니다.

피봇은 기존 잡힘 `Offset=0,120`과 프레임 ImageAnchor를 사용하여 이미지 좌표 `ImageAnchor − (0,120)`로 계산합니다. Lumi 기준점 `(96,192)`에서는 `(96,72)`입니다. 클릭한 위치 또는 그림의 불투명 영역을 새로 추정하지 않습니다. 피봇이 커서를 따라가고 기존 가상 발의 관성·감쇠 모델이 연속 회전각을 결정합니다. 빠른 마우스 이동일수록 크게 기울며 정지하면 감쇠합니다. 좌우 반전·배율도 같은 피봇을 유지합니다. 프레임 시간·순서·기준점·FRAME_ENTRY/BEHAVIOR_ENTRY·loop·음량은 그대로이며 마우스 이동이 재생 시간을 초기화하지 않습니다. 놓기·상태 이탈·제거에서 회전을 정리합니다. 공개 `MascotState.bounds`는 회전 후 표시 영역이고 `anchor`는 기존 논리 위치입니다. 표시 영역을 물리 충돌 영역으로 간주하지 마세요.

`AuthoringPreview.Snapshot.forAutomaticAnimations(animations,audio,HeldMotion)`는 동일 옵션을 실제 엔진에 전달하고 `Snapshot.heldMotion()`으로 조회합니다. 9인자 Snapshot의 마지막 인자이며 기존 8인자와 다른 생성자/팩토리는 `LEGACY`입니다. 자동 프로필 이외의 `PHYSICS`는 거부합니다. `AnimationActivities.forAnimation("Pinched",registered)`의 `held`는 Pinched 등록 시 실행할 수 있는 1인 활동입니다. `Session.startActivity("held")`는 대기 중인 시험 캐릭터 하나만 준비하며 사용자가 데스크톱에서 직접 집어 움직여야 합니다. 가짜 커서나 시간표를 실행하지 않습니다. 실제 잡힘 중 `Pinched` 단계를 보고하고 놓은 뒤 대기로 돌아오면 완료, 90초 경과 시 timed-out입니다. 일시정지·재개, 중지·교체·종료의 기존 세션 수명과 소리 정리 계약을 따릅니다. UI는 완료·실패·중지 뒤 세션을 닫아 시험 캐릭터를 제거해야 합니다.

### 이전 자동 출력의 읽기 전용 재구성

새 자동 캐릭터와 실제 시험은 기존 `generateAutomaticAnimations(..., HeldMotion)` / `generateStandaloneAutomaticAnimations(..., HeldMotion)` 및 `AuthoringPreview.Snapshot.forAutomaticAnimations(..., HeldMotion)`를 사용합니다. 미등록 활동은 자동으로 추가하지 않습니다.

`CharacterDefinition.HelperVisuals`와 자동 생성 함수의 **5인자 오버로드**는 이전 출력과의 바이트 비교·복원을 위한 호환 API입니다. `LEGACY`는 원래 출력, `SHARED`는 짧고 무음인 첫 Idle 보조 자세, `SHARED_MOVEMENT`는 이전 출력의 술래잡기·행렬과 그림/효과음 대체 규칙을 재현합니다. 원래 입력 프레임·순서·시간·기준점·효과음과 같은 정책을 전달해야 동일한 파일을 재구성할 수 있습니다. 반환된 파일을 별도 임시 위치에서 비교하며, 이 호출 자체가 설치된 모드나 사용자 프로젝트를 변경하지 않습니다.

이 정책은 새 제작 기능이 아니며 `AnimationActivities`와 `AuthoringPreview.Snapshot`에는 helper 정책 인수·필드를 추가하지 않습니다. 활동 조회·시험은 기존 API와 기본 실행 규칙을 유지합니다. 생성 함수의 기존 오버로드와 기본 Lumi·기존 모드의 동작은 바뀌지 않습니다.

<a id="workshop-publishing"></a>

## 창작마당 직접 게시

`PluginContext.workshopPublisher()`는 명시적으로 확인한 사용자의 현재 Steam 계정에 내보낸 모드를 게시합니다. `WorkshopPublisher.account()`는 EDT 밖에서 호출하며 사용 가능 여부, Steam ID, 표시 이름을 반환합니다. `publish(Request, Listener)`는 비동기이고 콜백도 EDT 밖입니다. UI는 계정·제목·신규/업데이트 대상·신규 공개 범위를 마지막으로 확인한 뒤 호출해야 합니다.

`Request(contentDirectory, previewFile, title, description, existingItemId, expectedSteamId, clientProjectId, visibility)`에서 신규 ID는 빈 문자열이고 `Visibility.PUBLIC/PRIVATE`를 명시합니다. 업데이트는 기존 공개 범위를 보존합니다. `clientProjectId`는 저장·이름 변경 뒤에도 동일해야 합니다. 본체는 파일을 검사·복사한 독립 스냅숏만 전송하고, 현재 계정을 재확인하며 업데이트 항목의 소유권을 Steam에서 확인합니다. 제목은 128 UTF-8 바이트 이하, 설명은 8000 바이트 미만, 미리보기 PNG/JPEG는 1 MiB 미만입니다.

`created(itemId, ownerSteamId)`는 항목 생성만 의미하며 업로드 성공이 아닙니다. 본체는 플러그인·프로젝트·계정별 저널에 ID를 전송 전에 저장하여 실패 후 재시도에서 같은 항목을 갱신합니다. 호출자도 ID를 프로젝트에 보존합니다. `completed(Result)`에 ID·소유자·웹 URL·약관 동의 필요 여부가 들어갑니다. `failed(Failure)`의 ID는 생성 후 전송 실패에서 비어 있지 않을 수 있습니다. Steam 생성 응답과 로컬 저널 기록 사이의 프로세스 강제 종료는 원자화할 수 없으므로 이 극히 짧은 구간에서 생긴 빈 항목은 Steam 페이지에서 확인해야 합니다.

`close()`는 이후 요청과 UI 콜백을 차단하고, 아직 제출하지 않은 작업을 중단합니다. 이미 Steam에 제출한 작업은 되돌리지 않으며 본체가 종료 결과까지 스냅숏을 유지합니다. 플러그인 컨텍스트 해제도 열린 서비스를 닫습니다. `PluginTestKit`에서는 Steam에 연결하지 않고 unavailable 계정을 반환합니다. 별도 창작마당 업로드나 공개는 테스트에서 자동 실행하지 않습니다.

### 업로드 파일 목록과 원본 수명

기존 `WorkshopPublisher.Request` 8인자 생성자는 일반 모드 검사 계약을 유지합니다. 파일 목록과 내용을 고정하여 게시하려면 마지막 두 인자 `Map<String,String> expectedFiles`, `String expectedPreviewSha256`를 받는 10인자 생성자를 사용합니다. `expectedFiles != null`이면 strict 모드이며, 사용자 확인 전에 확정한 **정확한 상대 경로→소문자 SHA-256** 목록과 별도 PNG/JPEG 원본 미리보기의 SHA-256이 모두 필요합니다. 상대 경로는 `/` 구분자를 사용하며 상위 이동·절대 경로·대소문자 충돌을 허용하지 않습니다. Map은 생성 시 방어 복사합니다.

strict 검사는 원본·SDK 사본의 실제 파일 전체를 비교합니다. 숨김 파일이나 `workshop-upload.txt` 같은 로컬 표식도 조용히 무시하지 않습니다. 누락·추가·내용 변경이 있으면 게시를 중단하며 변경된 파일로 확인 기준을 갱신하지 않습니다. 별도 미리보기는 1MiB 미만으로 한정해 읽고 원본 SHA-256을 확인한 뒤 PNG로 다시 인코딩합니다. 준비된 콘텐츠와 재인코딩된 미리보기는 새 항목 생성 응답 뒤를 포함해 Steam 콘텐츠 지정 직전과 제출 직전에 다시 검사합니다. 기존 일반 모드 입력의 표식 제외는 유지하되, 전송 사본에 나중에 생긴 추가 표식은 거부합니다.

`Listener.sourceReleased()`는 SDK가 `contentDirectory`·`previewFile`을 더 읽지 않는 시점에 정확히 한 번 EDT 밖에서 호출됩니다. 성공적인 독립 복사가 끝나면 즉시 전달되며, 준비 실패·거절·취소에도 전달됩니다. `close()` 뒤에도 이 자원 수명 알림은 억제하지 않으므로 호출자는 자신의 임시 배포 폴더를 이때 삭제할 수 있습니다. 이것은 업로드 성공이나 UI 갱신 알림이 아닙니다. `progress/created/completed/failed`의 기존 종료 후 UI 콜백 억제 규칙은 유지합니다. SDK 사본은 Steam 완료·실패까지 SDK가 소유·정리하며 로컬 모드 원본은 변경하지 않습니다. 일시적인 파일 잠금에는 한정된 삭제 재시도를 하고, 최종 정리 실패는 로컬 경고로 기록하며 원격 성공 결과를 실패로 바꾸지 않습니다.

strict SDK 사본은 소유자 전용 POSIX 권한 또는 Windows의 현재 사용자·SYSTEM ACL로 생성합니다. 파일은 기본 바이트 스트림만 복사하여 원본 ACL·속성·NTFS 대체 스트림을 전파하지 않습니다. 이는 같은 JVM에서 실행하는 승인 코드나 같은 계정의 악성 프로세스를 격리하는 샌드박스가 아니며, Steam이 이미 수락한 제출을 취소·되돌린다고 보장하지 않습니다.
