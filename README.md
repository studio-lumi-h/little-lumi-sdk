<!-- 이 파일은 build/make_sdk_public_repo.py 가 SDK 의 README.md 에서 만듭니다. 손으로 고치지 마세요. -->
# Little LUMI Model — 플러그인 SDK

[English](README.en.md) · [다운로드(Releases)](https://github.com/studio-lumi-h/little-lumi-sdk/releases)

> **이 저장소는 읽을 것만 담고 있습니다.** 문서·예제·도구·공개 API 소스입니다.
> **플러그인을 만들려면 [Releases](https://github.com/studio-lumi-h/little-lumi-sdk/releases)에서 ZIP 을 받으세요** — 컴파일에 필요한
> `lib/` 의 jar 와 생성된 javadoc 이 그 안에 들어 있습니다. 지금 버전은 **1.5.1** 입니다.

---

반갑습니다. 이 SDK 는 루미에 내 것을 더하는 도구 모음입니다. 클릭·쓰다듬기·알람 같은 일에 반응시키고, 대사·메뉴·단축키를 더하고, 그림이나 행동·번역 같은 데이터만으로 된 모드도 만들 수 있습니다.

이 배포본은 **SDK 1.5.1 / 앱 1.5.1 / api_version 1** 입니다.

문서 모음은 [index.html](index.html)에서 브라우저로 읽을 수 있습니다. 코드 예제에는 복사 버튼이 있고, 원본 Markdown과 라이선스도 그대로 들어 있습니다.

## 무엇이 들어 있나

| 폴더·파일 | 무엇에 쓰나 |
|---|---|
| `index.html` | API·카탈로그·예제·라이선스 읽기본 모음 |
| `plugin-guide.html` | 처음부터 따라 하는 첫걸음 안내 |
| `lib\` | 플러그인을 컴파일할 때 쓰는 jar |
| `tools\` | 굽기·검사·창작마당 준비 스크립트 |
| `examples\` | 대사·변환기·발판 하나 본보기 |
| `docs\` | API 참조 · 이름 카탈로그 · 변경 이력 · javadoc |
| `sdk.json` | 이 배포본의 버전 번호와, 이 앱이 주는 훅·이벤트 목록 |
| `LICENSE-SDK.txt` | 이용 조건 |

## 처음이라면

[**plugin-guide.html**](plugin-guide.html) 을 여세요. 여기서 시작하시면 됩니다.

도구를 까는 것부터 루미가 내가 쓴 대사를 말하는 것까지 순서대로 짚어 주고, 프로그래밍을 해 본 적이 없어도 따라올 수 있게 쓰여 있습니다. 영어로 보시려면 [plugin-guide.en.html](plugin-guide.en.html) 입니다.

## 빨리 해 보고 싶다면

JDK 25 와 파이썬 3 이 있으면, SDK 를 푼 폴더에서 PowerShell 로 세 줄이면 됩니다.

```powershell
python tools\build_plugin.py examples\hello
python tools\run_tests.py examples\hello
Copy-Item -Recurse dist\hello.lumi.sample "<설치 폴더>\mods\hello.lumi.sample"
```

첫 줄이 예제를 구워 `dist\hello.lumi.sample\` 을 만들고, 둘째 줄이 앱 없이 검사하고, 셋째 줄이 그 폴더째 설치본에 넣습니다. 앱은 끄고 복사한 뒤 다시 켜세요 — 설정 → 모드 탭에 예제 줄이 생깁니다. 예제 체크박스를 켜고 코드 실행 안내에 동의한 뒤, 트레이에서 앱을 종료하고 다시 켜야 인사와 플러그인 기능이 실행됩니다. 시작 안내의 확인은 모드 탭으로 이동할 뿐 동의가 아닙니다. 새 JAR로 바꾸면 다시 동의하고 재실행하세요.

## 그다음에 볼 것

| 문서 | 무엇 |
|---|---|
| [plugin-guide.html](plugin-guide.html) | 첫걸음 — 아무것도 모르는 상태에서 시작 |
| [docs/API.md](docs/API.md) | 할 수 있는 것 전부 |
| [docs/CATALOG.md](docs/CATALOG.md) | 행동·이벤트·훅·설정 키의 이름 찾기 |
| [docs/CHANGELOG.md](docs/CHANGELOG.md) | 버전마다 바뀐 것 |
| `examples\` | 본보기 셋 — 인사 플러그인·바이트코드 변환기·발판 하나 |
| `tools\` | 굽기·검사, 그리고 창작마당에 올리기 전 확인 |

## 알아 둘 것

- **SDK 버전은 앱 버전과 같습니다.** 쓰시는 루미와 같은 버전의 SDK 를 고르세요. `api_version`은 별도의 API 호환 번호입니다. 문서·예제 개정 내용은 배포 안내에서 확인하세요.
- `lib\Shimeji-ee.jar` 는 **컴파일 전용**입니다. 스팀 배포판 그대로라 SDK 만으로는 앱이 뜨지 않습니다 — 확인은 구매한 설치본에서 하세요.
- 플러그인은 앱과 **같은 권한으로 앱 안에서** 돕니다(샌드박스 없음). 나눠 줄 때 받는 분께 그렇게 알려 주세요.
- 이용 조건은 [LICENSE-SDK.txt](LICENSE-SDK.txt) 에 있습니다.

앱이 새 버전으로 올라갈 때 무엇이 그대로 남고 무엇이 바뀔 수 있는지는 [API.md 「호환 약속」](docs/API.md#compatibility)에 적혀 있습니다.

## 코드 모드 활성화

설치·구독만으로 모드 활성화를 허용하지 않습니다. 모드 체크박스를 켜고 안내에 동의하면 코드와 함께 들어 있는 데이터가 **다음 앱 실행부터** 적용됩니다. 기존 실행 해시는 동의로 간주하지 않습니다. 플러그인 JAR 또는 `plugins/lib` 라이브러리가 바뀌면 다시 동의해야 합니다. 네트워크·행동 변경 표시는 제작자 선언이며 강제 권한 제한이 아닙니다.

라이브러리는 플러그인과 같은 동의된 모드 안에 포함하세요. 별도 라이브러리 전용 모드는 클래스 경로에 추가하지 않습니다. 의존 파일은 JAR 매니페스트 `Class-Path` 대신 `plugins/lib`에 배치하세요. 매니페스트 클래스 경로와 JAR 인덱스는 거부합니다. 검증·보호된 별도 경로에서 실행하므로 CodeSource 경로가 설치 폴더라고 가정하면 안 됩니다.

심볼릭 링크·정션을 포함한 코드 경로는 지원하지 않습니다.

시작 시 신규·변경 코드 모드의 동의가 필요하면 안내 창이 뜹니다. 이 창의 **확인**은 설정 → 모드로 이동할 뿐 모드를 활성화하지 않습니다. 이름 앞의 빨간 **비활성화** 칩을 확인하고 모드 체크박스에서 별도로 동의하세요.

동의창에서 취소 버튼을 누르면 토글이 **−(미동의 잠금)**으로 바뀌어 같은 코드 버전의 시작 알림에서 제외됩니다. **−를 클릭하면 OFF**로 돌아가며, JAR·라이브러리 업데이트 시 잠금이 풀립니다. 잠금 상태에서는 모드가 활성화되지 않으므로 코드와 데이터는 계속 꺼져 있습니다. ESC나 X로 창을 닫으면 동의·잠금 상태를 바꾸지 않습니다.
