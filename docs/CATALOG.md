<!-- 자동 생성 — 손대지 말 것 · python build/gen_plugin_catalog.py -->
# Little LUMI Model — 플러그인 카탈로그

**SDK/앱 1.4.0, API 1** 의 이름·값 참조표입니다. 생성기가 소스에서 만듭니다 — 손으로 고치지 않습니다(SDK 빌드가 낡은 사본을 거부합니다). 규약과 예시는 [API.md](API.md), 여기는 이름과 값만 있습니다. [English](CATALOG.en.md) · [시작 안내](../README.md).

[행동](#behaviours) · [이벤트](#events) · [훅](#hooks) · [캐릭터 상태](#mascot-state) · [앱 설정](#app-settings) · [설정 타입](#setting-types) · [기능 ID](#feature-ids)

실행 중인 캐릭터가 할 수 있는 행동은 `ctx.behaviorNames(imageSet)` 으로 확인하세요. 플러그인의 옵션은 `ctx.prefs()` 에 두고, 앱 설정 키보다 공개 조회 메서드를 먼저 쓰세요.

<a id="behaviours"></a>

## 1. 행동 이름 — `ctx.behave(name)` · `ctx.behaviorNames(imageSet)`

`img\Lumi\conf\behaviors.xml` 의 `<Behavior Name>` 전부와 `행동목록.txt` 의 한글 이름입니다. 다른 이미지셋은 제 `behaviors.xml` 을 따르므로 실행 중에는 `ctx.behaviorNames(imageSet)` 이 정답입니다. 기부 점프 `JumpDono_x<0..9>_y<0..14>` 150개는 좌표별로 생성된 것이라 한 줄로 접었습니다.

**숨김**(`Hidden="true"`)은 캐릭터 메뉴에서 감출 뿐입니다. 저절로 골라지는지는 **빈도**와 `Condition` 이 정합니다 — 빈도는 선택 가중치이지 시간이나 고정 확률이 아니고, 0 이면 자동 선택에서 빠지지만 이름으로 부르거나 다른 행동에서 이어질 수는 있습니다. 숨김이면서 빈도 > 0 인 행동도 있습니다. **토글**(`Toggleable="true"`)은 숨김과 같이 붙을 수 있습니다. `Dragged`·`Thrown` 은 엔진이 마우스 조작으로 거는 행동입니다.

| 이름 | 한글 | 빈도 | 숨김 |
|---|---|---|---|
| `ChaseMouse` | 마우스 쫓아가기 | 0 | 숨김 |
| `SitAndFaceMouse` | 앉아서 마우스 향하기 | 0 | 토글 |
| `SitAndSpinHead` |  | 0 | 숨김 |
| `Fall` | 추락 | 0 | 숨김 |
| `Dragged` | 들려 있음(엔진) | 0 | 숨김 |
| `Thrown` | 던져짐(엔진) | 0 | 숨김 |
| `Talk` | 입 뻐끔(말하는 동안 — ctx.mouthFlap 이 반복해 건다) | 0 | 숨김 |
| `Flying` | 자리로 날아가기(자바만 부른다) | 0 | 숨김 |
| `AlarmNotice` | 알람 「어!」(자바만 부른다) | 0 | 숨김 |
| `HeadPat` | 쓰다듬기 | 0 | 토글 |
| `HeadPatTilt2` |  | 0 | 숨김 |
| `HeadPatTilt3` |  | 0 | 숨김 |
| `HeadPatSit` |  | 0 | 숨김 |
| `HeadPatSitTilt2` |  | 0 | 숨김 |
| `HeadPatSitTilt3` |  | 0 | 숨김 |
| `HeadPatSitFacing` |  | 0 | 숨김 |
| `HeadPatSitFacingTilt2` |  | 0 | 숨김 |
| `HeadPatSitFacingTilt3` |  | 0 | 숨김 |
| `HeadPatSitLedge` |  | 0 | 숨김 |
| `SelfTalk` | 혼잣말 | 0 | 토글 |
| `ChatHost` |  | 0 | 숨김 |
| `ChatGuest` |  | 0 | 숨김 |
| `HiFiveHost` |  | 0 | 숨김 |
| `HiFiveGuest` |  | 0 | 숨김 |
| `GiftGiver` |  | 0 | 숨김 |
| `GiftReceiver` |  | 0 | 숨김 |
| `GiftDing` |  | 0 | 숨김 |
| `GiftEnd` |  | 0 | 숨김 |
| `GiftCatch` |  | 0 | 숨김 |
| `GiftHopLoop` |  | 0 | 숨김 |
| `CreditSpot` |  | 0 | 숨김 |
| `ParadeBump` |  | 0 | 숨김 |
| `GatherSit` |  | 0 | 숨김 |
| `GatherCallAgain` |  | 0 | 숨김 |
| `TagWin` |  | 0 | 숨김 |
| `TagCaught` |  | 0 | 숨김 |
| `CreditGet` |  | 0 | 숨김 |
| `SkyReturn` |  | 900 | 숨김 |
| `VoidRescue` |  | 50 | 숨김 |
| `EventDrop` |  | 0 | 숨김 |
| `PullUp` |  | 0 | 숨김 |
| `Divided` |  | 0 | 숨김 |
| `StandUp` | 일어서기 | 60 | 숨김 토글 |
| `LookAround` | 도리도리 | 120 | 토글 |
| `TiltHead` | 흥얼흥얼 | 120 | 토글 |
| `Chatter` | 인사하기 | 90 | 토글 |
| `CreditEvent` | 크레딧 획득 | 150 | 토글 |
| `CreditRetry` | 남은 크레딧 주우러 가기 | 60 | 숨김 토글 |
| `ChatInvite` | 말 걸기 | 20 | 토글 |
| `ChatApproach` |  | 70 | 숨김 토글 |
| `LeadParade` | 줄줄이 행진 | 20 | 토글 |
| `JoinParade` |  | 70 | 숨김 토글 |
| `GatherCall` | 모여 모여 | 20 | 토글 |
| `GatherJoin` |  | 70 | 숨김 토글 |
| `TagRun` | 술래잡기 | 20 | 토글 |
| `TagChase` |  | 70 | 숨김 토글 |
| `HiFiveInvite` | 하이파이브 | 20 | 토글 |
| `HiFiveApproach` |  | 70 | 숨김 토글 |
| `GiftInvite` | 크레딧 나눠주기 | 20 | 토글 |
| `GiftApproach` |  | 70 | 숨김 토글 |
| `SprintAndBrake` | 전력질주 | 130 | 토글 |
| `ClimbIEFace` | 창 타고 오르기 | 90 | 토글 |
| `ClimbIEFaceDrop` | 창 타다 미끄러지기 | 50 | 숨김 토글 |
| `FloorGrabIEWall` | 창 옆면 매달리기 | 70 | 토글 |
| `WindowDive` | 창에서 뛰어내리기 | 0 | 숨김 |
| `HappyHop` | 폴짝폴짝 | 150 | 토글 |
| `SitDown` | 앉기 | 80 | 토글 |
| `SitWhileDanglingLegs` | 걸터앉기 | 50 | 토글 |
| `LieDown` |  | 0 | 숨김 |
| `SplitIntoTwo` |  | 0 | 숨김 |
| `HoldOntoWall` | 벽 붙잡기 | 100 | 숨김 토글 |
| `FallFromWall` | 벽에서 떨어지기 | 50 | 숨김 토글 |
| `FocusHoldOntoIEWall` | 벽 조용히 붙잡기 | 100 | 숨김 |
| `HoldOntoCeiling` | 천장 붙잡기 | 0 | 숨김 |
| `FallFromCeiling` | 천장에서 떨어지기 | 50 | 숨김 토글 |
| `FocusHoldOntoCeiling` | 천장 조용히 붙잡기 | 100 | 숨김 |
| `WalkAlongWorkAreaFloor` | 작업 공간 바닥 걷기 | 100 | 숨김 토글 |
| `RunAlongWorkAreaFloor` | 작업 공간 바닥 달리기 | 100 | 숨김 토글 |
| `CrawlAlongWorkAreaFloor` | 바닥 기어가기 | 0 | 숨김 |
| `WalkLeftAlongFloorAndSit` | 왼쪽으로 걷다가 앉기 | 100 | 숨김 토글 |
| `WalkRightAlongFloorAndSit` | 오른쪽으로 걷다가 앉기 | 100 | 숨김 토글 |
| `GrabWorkAreaBottomLeftWall` |  | 0 | 숨김 |
| `GrabWorkAreaBottomRightWall` |  | 0 | 숨김 |
| `WalkLeftAndSit` | 왼쪽으로 달려가서 앉기 | 100 | 숨김 토글 |
| `WalkRightAndSit` | 오른쪽으로 달려가서 앉기 | 100 | 숨김 토글 |
| `RunAndGrabBottomLeftWall` |  | 0 | 숨김 |
| `RunAndGrabBottomRightWall` |  | 0 | 숨김 |
| `JumpFromBottomOfIE` | 창 아래에서 뛰어오르기 | 40 | 숨김 토글 |
| `PullUpShimeji` |  | 0 | 숨김 |
| `ClimbHalfwayAlongWall` | 벽 따라 일부 오르기 | 50 | 숨김 토글 |
| `ClimbAlongWall` | 벽 따라 오르기 | 100 | 숨김 토글 |
| `ClimbAlongCeiling` | 천장 따라 오르기 | 100 | 숨김 토글 |
| `WalkAlongIECeiling` | 창 천장 따라 걷기 | 100 | 숨김 토글 |
| `FaceFromTop` |  | 80 | 숨김 토글 |
| `RunAlongIECeiling` | 창 천장 따라 달리기 | 100 | 숨김 토글 |
| `CrawlAlongIECeiling` |  | 0 | 숨김 |
| `SitOnTheLeftEdgeOfIE` | 창 왼쪽 가장자리에 앉기 | 100 | 숨김 토글 |
| `SitOnTheRightEdgeOfIE` | 창 오른쪽 가장자리에 앉기 | 100 | 숨김 토글 |
| `JumpFromLeftEdgeOfIE` | 창 왼쪽 가장자리에서 뛰기 | 100 | 숨김 토글 |
| `JumpFromRightEdgeOfIE` | 창 오른쪽 가장자리에서 뛰기 | 100 | 숨김 토글 |
| `WalkLeftAlongIEAndSit` | 창 따라 왼쪽으로 걷다가 앉기 | 100 | 숨김 토글 |
| `WalkRightAlongIEAndSit` | 창 따라 오른쪽으로 걷다가 앉기 | 100 | 숨김 토글 |
| `WalkLeftAlongIEAndJump` | 창 따라 왼쪽으로 걷다가 뛰기 | 100 | 숨김 토글 |
| `WalkRightAlongIEAndJump` | 창 따라 오른쪽으로 걷다가 뛰기 | 100 | 숨김 토글 |
| `HoldOntoIEWall` | 창 벽 붙잡기 | 100 | 숨김 토글 |
| `ClimbIEWall` | 창 벽 오르기 | 100 | 숨김 토글 |
| `ClimbGardenLadder` | 섬 사다리 오르기 | 120 | 숨김 토글 |
| `ClimbIEBottom` | 창 바닥 오르기 | 100 | 숨김 토글 |
| `GrabIEBottomLeftWall` | 창 왼쪽 아래 벽 붙잡기 | 100 | 숨김 토글 |
| `GrabIEBottomRightWall` | 창 오른쪽 아래 벽 붙잡기 | 100 | 숨김 토글 |
| `JumpFromLeftWall` | 창 왼쪽 벽에서 뛰기 | 0 | 숨김 |
| `JumpFromRightWall` | 창 오른쪽 벽에서 뛰기 | 0 | 숨김 |
| `JumpOnIELeftWall` | 창 왼쪽 벽으로 뛰기 | 50 | 숨김 토글 |
| `JumpOnIERightWall` | 창 오른쪽 벽으로 뛰기 | 50 | 숨김 토글 |
| `ThrowIEFromLeft` |  | 120 | 토글 |
| `ThrowIEFromRight` |  | 120 | 토글 |
| `RunAndThrowIEFromLeft` |  | 120 | 토글 |
| `RunAndThrowIEFromRight` |  | 120 | 토글 |
| `JumpDono_x<n>_y<m>` ×150 | | 0 | 숨김 |

<a id="events"></a>

## 2. 이벤트 토픽과 페이로드 — `ctx.on(topic, event -> …)`

「실험」 칩이 붙은 항목은 `api_version` 을 올리지 않고 바뀌거나 없어질 수 있습니다(같은 목록이 `sdk.json` 의 `preview` 에 있습니다). 표시가 없는 항목은 안정입니다 — [API.md §8](API.md#compatibility).

따로 형을 적지 않은 값은 문자열입니다. `mascotId` 는 그 개체가 사는 동안만 유효하고(사라지면 재사용될 수 있습니다), 좌표는 바탕화면 좌표입니다. 앱에서는 플러그인마다 하나인 이벤트 스레드에서, 테스트 킷에서는 부른 스레드에서 리스너가 돕니다. 페이로드 예시와 연동 주의점은 [API.md 의 이벤트 절](API.md#events) 을 보세요.

| 토픽 | 구분 | 의미와 필드 |
|---|---|---|
| `ready` |  | 엔진이 뜨고 플러그인들이 시작된 뒤 — 없음 |
| `behavior` |  | 마스코트의 행동이 바뀜 — `imageSet`, `name`, `mascotId`(Integer) |
| `bubble` |  | 말풍선이 뜸 — `imageSet`, `mascotId`(Integer, 0 = 이미지셋 지정; `kind=choice` 에는 없음), `text`(보이는 글자 — `bubble.text` 훅 뒤), `ms`(Long, `bubble.duration` 뒤 최종 수명; `choice` 는 제한 시간), `kind`(`say` = 보통 줄(큐에 넣은 줄·`say.txt` 포함), `private` = 방송 무대가 건너뛰는 알람·부탁 줄, `choice` = 단추 달린 말풍선이 열림). 「···」 busy 표시는 이벤트를 내지 않습니다 |
| `choice` | 실험 | 선택지 말풍선에 답이 남 — `imageSet`, `mascotId`(Integer), `index`(Integer, 0부터 · `-1` = 떴는데 답 없음(시간 초과·Esc·캐릭터 사라짐) · `-2` = 앱이 치움(같은 플러그인이 대체·`clearQueue`·줄 대기 초과·줄 참)), `option`(고른 글자, 음수면 `""`), `from`(물은 플러그인 id) |
| `click` |  | 캐릭터 위에서 마우스 단추가 눌림 — `imageSet`, `mascotId`(Integer), `button`(`left`/`right`/`middle`), `x`, `y`(Integer, 화면 좌표) |
| `mascot` |  | 캐릭터가 나타나거나 사라짐 — `imageSet`, `mascotId`(Integer), `event`(`added`/`removed`) |
| `headpat` |  | 쓰다듬기 시작 — `behavior` |
| `affection` |  | 보이는 호감도 단계가 바뀜 — `tier`(Integer), `previous`(Integer), `points`(Integer) |
| `achievement` |  | 스팀 도전과제 해금 — `id` |
| `focus` |  | 집중 모드 켜짐/풀림 — `active`(Boolean) |
| `chat` |  | 앱 API에 등재된 채팅 토픽 — `phase`(`ask`/`reply`), `imageSet`, `text`. 발생 여부는 설치한 채팅 연동에 달려 있습니다. `ctx.publish("chat", ...)`는 앱 토픽이 아닌 `<내-id>.chat`을 발행합니다. |
| `tts` |  | 음성 재생 시작/끝 — `phase`(`start`/`end`) |
| `settings` |  | 설정 창 [적용], 또는 언어 변경 — `phase`(`apply`), `changed`(지난 [적용] 이후 앱이 `LumiSettings` 로 적은 키를 쉼표로 — 스톡 엔진 키는 안 실림) / `phase`(`language`), `language`(BCP-47) |
| `ask` |  | `speech\ask.txt` 가 쓰임 — `text` |
| `settings_open` |  | `speech\settings_open.txt` 가 쓰임 — 없음 |
| `say_file` |  | `speech\say.txt` 줄이 표시됨 — `text` |
| `drag` | 실험 | 사용자가 캐릭터를 집어 올림·놓음(`Dragged` 시작·끝) — `phase`(`start`/`end`), `imageSet`, `mascotId`(Integer), `x`, `y`(Integer, 앵커의 화면 좌표) |
| `throw` | 실험 | 놓으며 던져짐(`Thrown` 시작) · 그 비행이 끝남(다음 행동 시작) — `phase`(`thrown`/`landed`), `imageSet`, `mascotId`(Integer), `x`, `y`(Integer), `next`(항상 실립니다 — `landed` 면 비행 뒤 행동 이름, `thrown` 이면 `""`) |
| `hover` | 실험 | 포인터가 캐릭터 창에 들어옴·나감 — `phase`(`enter`/`exit`), `imageSet`, `mascotId`(Integer) |
| `doubleclick` | 실험 | 같은 캐릭터 위 왼쪽 누름 둘이 600 ms·90 px 안(AWT clickCount 는 믿을 수 없음 — 누르는 순간 들기가 창을 옮김) — `imageSet`, `mascotId`(Integer), `x`, `y`(Integer). 그 둘은 `click` 도 각각 냅니다 |
| `alarm` | 실험 | 알림이 실제로 전달됨 — `kind`(`hourly`/`alarm`), `time`(`HH:mm`), `late`(Boolean — 화면이 바빠 미뤄졌다 나온 알람) |
| `pomodoro` | 실험 | 집중/휴식 주기가 움직임 — `phase`(`focus`/`break`/`stopped`), `minutes`(Integer, 시작하는 국면의 길이; stop 은 0), `round`(Integer, 켠 뒤 시작한 집중 국면 수) |
| `bongo` | 실험 | 봉고 모드가 타건·클릭을 그림 — `kind`(`keyboard`/`left_click`/`right_click`), `frame`(Integer, 그린 손 자리), `look`(`bongo`/`small`). **키 코드·글자는 절대 없음**(코어는 키를 읽지 않음) |
| `app` | 실험 | 앱이 제 종료 문으로 내려감 — `phase`(`exit`). 플러그인 `stop()` 보다 앞, 짧은 상한을 두고 전달을 기다린 뒤 내려감 |
| `island` | 실험 | 정원(섬) 프로세스 시작·정지 요청, 사진 요청 응답 — `phase`(`start`/`stop`/`photo`), `request`(photo 만) |
| `game` | 실험 | 장치 게임 판이 열림·달리기 시작·닫힘 — `phase`(`start`/`run`/`stop`) |

<a id="hooks"></a>

## 3. 훅과 ctx 키 — `ctx.hook(id, (hook, current, ctx) -> …)`

「실험」 칩이 붙은 항목은 `api_version` 을 올리지 않고 바뀌거나 없어질 수 있습니다(같은 목록이 `sdk.json` 의 `preview` 에 있습니다). 표시가 없는 항목은 안정입니다 — [API.md §8](API.md#compatibility).

`PluginHooks.KNOWN` 의 id 마다 코어가 묻는 자리(앱 소스의 파일 — 여럿이면 ` · ` 로 잇습니다), 묻는 메서드, 기본값 식, ctx Map 의 키입니다. 소스 경로는 어디서 묻는지 알려 주는 참고이고 그 파일이 SDK 에 들어 있지는 않습니다. 반환형·범위·특수값은 [API.md 의 훅 표](API.md#hooks) 를 먼저 보세요. `null` 은 통과(앞선 `current` 유지)이고, 뒤의 핸들러가 null 아닌 값을 돌려주면 앞선 답을 바꿉니다. 훅 안에서는 파일·네트워크·잠금을 잡지 마세요.

| 훅 | 구분 | 묻는 자리 | 메서드 | 기본값 | ctx 키 |
|---|---|---|---|---|---|
| `affection.award` |  | `com/group_finity/mascot/lumi/AffectionDirector.java` | `decideInt` | `points (clamp 0..1_000_000)` | `source`, `points`, `dailyCap`, `cooldownSeconds`, `todayPoints` |
| `affection.tier` |  | `com/group_finity/mascot/lumi/AffectionDirector.java` | `decideInt` | `tier (clamp 1..MAX_TIER)` | `points` |
| `behavior.force` |  | `com/group_finity/mascot/LumiBridge.java` | `decideString` | `name == null ? "" : name` | `name`, `caller`, `mascotId`, `imageSet` |
| `bongo.key_frame` |  | `com/group_finity/mascot/lumi/BongoWindow.java` | `decideInt` | `chosen (clamp -1..keyFrames() - 1)` | `frames`, `cycle`, `look` |
| `bongo.key_input` |  | `com/group_finity/mascot/lumi/BongoWindow.java` | `decideInt` | `-2 (clamp -2..bongo.keyFrames() - 1)` | `frames`, `look` |
| `bubble.duration` |  | `com/group_finity/mascot/lumi/SelfTalkDirector.java` · `com/group_finity/mascot/lumi/SpeechDirector.java` | `decideLong` | `life (clamp 500..120_000)` · `computed (clamp 500..120_000)` | `text`, `requested`, `source`, `voiceMs`, `mascotId`, `imageSet` |
| `bubble.text` | 실험 | `com/group_finity/mascot/lumi/SpeechDirector.java` | `decideString` | `text` | `text`, `source`, `mascotId`, `imageSet` |
| `focus.quiet` |  | `com/group_finity/mascot/lumi/FocusDirector.java` | `decideBoolean` | `quiet` | `hidden`, `fullscreenAhead`, `focusActive` |
| `focus.screen_busy` |  | `com/group_finity/mascot/lumi/FocusDirector.java` | `decideBoolean` | `busy` | `hidden`, `fullscreenAhead` |
| `place.allowed` |  | `com/group_finity/mascot/lumi/TetherDirector.java` | `decideBoolean` | `allowed` | `x`, `y`, `confinement`, `mover` |
| `selftalk.interval` |  | `com/group_finity/mascot/lumi/SelfTalkDirector.java` | `decideLong` | `interval (clamp 3_000..3_600_000)` | `baseMs`, `frequency`, `minSec`, `maxSec` |
| `selftalk.line` |  | `com/group_finity/mascot/lumi/SelfTalkDirector.java` | `decideString` | `line` | `candidate`, `onGardenFloor`, `gardenPhase`, `tier`, `offered`, `mascotId`, `imageSet` |
| `sound.effect` |  | `com/group_finity/mascot/Mascot.java` | `decideString` | `soundPath` | `path`, `caller` |
| `speech.greeting` |  | `com/group_finity/mascot/lumi/SpeechDirector.java` | `decideString` | `picked == null ? "" : picked` | `pool`, `last`, `trigger`, `imageSet` |
| `text.localized` |  | `com/group_finity/mascot/lumi/SpeechDirector.java` | `decideString` | `value` | `key`, `language`, `fallback` |

<a id="mascot-state"></a>

## 4. `MascotState` — `ctx.mascots()` 의 원소

자바 레코드라 `state.id()`, `state.imageSet()`, `state.anchor()` 처럼 메서드로 읽습니다. 값은 그 순간의 사본이고, `ctx.mascotById(id)` 는 그 캐릭터가 없으면 `null` 입니다.

| 필드 | 형 |
|---|---|
| `id` | `int` |
| `imageSet` | `String` |
| `bounds` | `Rectangle` |
| `anchor` | `Point` |
| `lookRight` | `boolean` |
| `behavior` | `String` |

<a id="app-settings"></a>

## 5. 앱 설정 키 — `ctx.setting(key, fallback)` (읽기만)

● = `Preferences.KEYS`(스팀 클라우드에 오르는 취향 값, `conf\preferences.properties` 에도 사본). 나머지는 앱이 `conf\settings.properties` 에 실제로 읽고 쓰는 키입니다 — 앱 소스에서 설정 저장소(`LumiSettings`)를 지나는 자리에서만 모았습니다. 값은 전부 문자열이고 앱 버전마다 늘 수 있으며, 표에 실렸다고 고정 계약이 되지는 않습니다.

대응하는 공개 조회 메서드가 없을 때만 `ctx.setting(key, fallback)` 으로 읽고 없는 경우를 처리하세요 — 언어는 `language()`, 활성 셋은 `activeImageSets()`, 기능은 `featureEnabled(id)` 입니다. **플러그인은 앱 키를 쓰지 않습니다** — 제 것은 `ctx.prefs()` 에 둡니다.

| 키 | 취향(클라우드) |
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
| `lumi.characters.layerOrder` | |
| `lumi.characters.startup` | |
| `lumi.confguard.all` | |
| `lumi.debug.enabled` | |
| `lumi.island.tutorialShown` | |
| `lumi.onboarding.step` | |
| `lumi.shake.credit.state` | |
| `lumi.steam.enabled` | |
| `lumi.transform.art.enabled` | |

<a id="setting-types"></a>

## 6. `plugin.json` `"settings"` 의 `type`

값: `bool`, `int`, `string`, `choice`, `hotkey`. 항목 꼴과 언어별 이름은 [API.md 의 매니페스트 절](API.md#manifest) 을 보세요.

<a id="feature-ids"></a>

## 7. 기능 스위치 id — `ctx.featureEnabled(id)`

설정 창의 스위치입니다. 읽기만 합니다 — 플러그인은 켜고 끄지 않습니다. 안 켰거나 모르는 id 는 거짓입니다.

| id | 앱 설정 키 |
|---|---|
| `bongo` | `lumi.bongo.enabled` |
| `broadcast` | `lumi.broadcast.enabled` |
| `island` | `lumi.island.enabled` |
| `game` | `lumi.game.enabled` |
| `focus.auto` | `lumi.focus.auto` |
