# Hello LUMI 예제 / Hello LUMI example

플러그인의 이벤트·훅·설정·트레이 메뉴와 단위 검사를 시작하는 예제입니다. SDK 루트에서 아래 명령을 실행하세요. JDK 25와 Python 3이 필요합니다.

This sample demonstrates events, hooks, settings, tray entries and unit tests. Run these commands from the SDK root with JDK 25 and Python 3 installed.

## 빌드 / Build

```text
python tools\build_plugin.py examples\hello
```

결과는 `dist\hello.lumi.sample\plugins\hello.lumi.sample.jar`입니다. 설치본의 `mods\<이름>\`에 결과 모드 폴더를 복사하고 앱을 다시 시작합니다.

Copy the resulting mod folder into `mods\<name>\` in your purchased app installation, then restart the app.

## 앱 없이 검사 / Test without the app

```text
python tools\run_tests.py examples\hello
```

`PluginTestKit`이 실제 앱·Steam·창 없이 예제의 동작을 검증합니다. 자세한 내용은 [한국어 API](../../docs/API.md) / [English API](../../docs/API.en.md)를 참고하세요.

`PluginTestKit` checks the sample without opening the app, Steam or a window. See the API reference for the test helpers.
