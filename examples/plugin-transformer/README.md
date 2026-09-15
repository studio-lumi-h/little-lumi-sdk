# 예제 — 플러그인 변환기: 본체 클래스의 메서드 하나만 로드 직전에 바꾸기

본체 jar 의 `FocusDirector.setManual(boolean)` 을 **로드 직전에** 고쳐 쓴다.
JDK 25 표준 `java.lang.classfile` API 만 쓰므로 라이브러리가 필요 없다.

무엇이 바뀌나:
1. 메서드 맨 앞에 `FocusPatch.onSetManual(enabled)` 호출이 끼어든다 — 로그에
   `MOD TRANSFORMER HIT: FocusDirector.setManual(true) rewritten by …` 가 남는다.
2. 집중 모드를 켤 때 루미가 하는 말이 `FocusPatch.ENGAGED_LINE` 으로 바뀐다
   (번들 키 `FocusModeEngaged` → 없는 키, 대체 문구 상수 → 우리 문구; 둘 다 `ldc` 상수 교체).

## 굽기

```
python tools\build_plugin.py examples\plugin-transformer
```
→ `dist\lumi.sdk.example.transformer\plugins\lumi.sdk.example.transformer.jar`

## 설치·확인

1. `dist\lumi.sdk.example.transformer\` 폴더를 게임의 `mods\sdk-example\` 로 복사하고 앱을 다시 켠다.
2. 설정 → 모드 탭에 플러그인 칩이 붙은 줄이 생긴다. 로그(`LittleLumiLog0.log.0`)에
   `Plugin loaded: lumi.sdk.example.transformer 1.0.0 (lumi.sdk.example.transformer.jar) sha256=…` 와
   `focus transformer applied=true`.
3. 트레이 메뉴의 「SDK example: toggle focus (patched greeting)」를 누르면 루미가 바뀐 문구를 말하고
   로그에 `MOD TRANSFORMER HIT` 가 찍힌다.
4. 손 안 대고 확인하려면(검증 하네스): 앱을 켜기 전에 빈 파일
   `plugindata\lumi.sdk.example.transformer\selftest.txt` 를 만들어 둔다. 준비되면 플러그인이
   집중 모드를 스스로 켰다 끄고 `selftest done: patched method ran 2 time(s)` 를 남긴다.

## 어디까지 되나

- 대상 클래스가 **아직 로드되지 않았으면** 무엇이든 바꿀 수 있다(메서드·필드 추가 포함). 이미
  로드된 뒤라면 `PluginContext.patch()` 로 다시 변환하되 JVM 제한(메서드 본문만) 안에서다.
- 보호 목록 일곱(`SteamGate`·`OfflineToken`·`Features`·`lumi.plugin.*`·`lumi.steam.*`·`AffectionStore`·
  `Achievements`)은 건네지지 않는다 — 그 클래스에는 `wants()` 조차 불리지 않는다. 그 밖의 본체
  클래스는 전부 열려 있다.
- 던지는 변환기는 로그만 남기고 클래스는 원본 그대로 로드된다.
- 같은 클래스를 여러 플러그인이 건드리면 이어 적용된다 — **겹쳐 쓸 수 있다**. 모드 목록에서 **아래 모드가
  먼저** 등록된다(로더가 플러그인 jar 를 역순으로 클래스패스에 붙인다). 다만 `ServiceLoader` 순서는 계약이
  아니니 **적용 순서에 기대지 말 것**.
- 그래도 **마지막 수단**이다. 앱 업데이트로 그 메서드가 바뀌면 깨진다. 먼저 이벤트·공개 API와
  `docs/API.md`의 판정 훅을 사용하고, 필요한 기능이 없는 경우에만 변환기를 사용한다.

## 직접 만들 때

- `wants()` 는 모든 클래스마다 불리니 문자열 비교 하나로 끝내라.
- 바꿀 메서드의 서술자는 `javap -p -cp lib\Shimeji-ee.jar <클래스>` 로 확인한다. 이 예제의 훅은
  `public static` 이어야 한다 — 다른 패키지에서 `invokestatic` 으로 부르므로.
- `transform()` 은 클래스 로딩 락을 쥔 채 돈다. 그 안에서 앱을 건드리지 말 것.

## 배포 고지 / Distribution notices

예제를 복사할 때 `src/META-INF/little-lumi/`도 함께 보존하세요. SDK 생성 시 들어가는 기존 이용 조건·저작권·면책 고지는 빌드한 JAR에도 자동으로 포함됩니다. 추가한 코드와 외부 의존성의 고지는 별도로 넣으세요.

Keep `src/META-INF/little-lumi/` when copying this example. The existing terms, copyright and disclaimer supplied by the SDK builder are automatically included in the built JAR. Add notices for your own additions and dependencies separately.

창작마당 패커는 고지를 포함한 모든 파일을 보존합니다. 지원하지 않는 파일이나 경로가 있으면 일부 파일만 버린 패키지를 만들지 않고 실패합니다.

The Workshop packer preserves every file, including notices. Unsupported files or paths fail the operation instead of producing a partial package.
