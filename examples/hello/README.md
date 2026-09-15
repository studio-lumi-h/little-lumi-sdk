# Hello LUMI 예제 / Hello LUMI example

플러그인의 이벤트·훅·설정·트레이 메뉴와 단위 검사를 시작하는 예제입니다. SDK 루트에서 아래 명령을 실행하세요. JDK 25와 Python 3이 필요합니다.

This sample demonstrates events, hooks, settings, tray entries and unit tests. Run these commands from the SDK root with JDK 25 and Python 3 installed.

## 빌드 / Build

```text
python tools\build_plugin.py examples\hello
```

결과는 `dist\hello.lumi.sample\plugins\hello.lumi.sample.jar`입니다. 앱을 종료한 뒤 `dist\hello.lumi.sample` 폴더를 설치본의 `mods\` 아래에 복사합니다. 앱을 켜고 설정 > 모드에서 예제의 토글을 눌러 권한 안내를 확인한 뒤 **활성화**를 선택합니다. 시작 안내의 확인은 설정으로 이동할 뿐 활성화가 아닙니다. 앱을 완전히 종료하고 다시 켜면 인사와 설정 카드·트레이 메뉴가 나타납니다.

Quit the app and copy `dist\hello.lumi.sample` into the installation’s `mods\` folder. Start the app, click the sample’s toggle in Settings > Mods, review the permissions and choose **Enable**. Confirming the startup notice only opens Settings. Quit completely and start again to load the greeting, settings card and tray entries.

문구나 의존 라이브러리를 수정해 JAR 내용이 바뀌면 다시 활성화하고 다음 앱 실행에서 확인합니다. 시작 인사 옵션과 앱 언어를 확인하세요. 트레이의 인사는 별도 문구이며 「인사 말투」 설정을 따릅니다. 자세한 순서는 [첫걸음 안내](../../plugin-guide.html#zero)를 참고하세요.

After changing text or dependency JARs, enable the changed mod again and restart. Check the startup greeting option and app language. The tray greeting uses separate text and follows Greeting mood. See the [walkthrough](../../plugin-guide.en.html#zero).

## 앱 없이 검사 / Test without the app

```text
python tools\run_tests.py examples\hello
```

`PluginTestKit`이 실제 앱·Steam·창 없이 예제의 동작을 검증합니다. 자세한 내용은 [한국어 API](../../docs/API.md) / [English API](../../docs/API.en.md)를 참고하세요.

`PluginTestKit` checks the sample without opening the app, Steam or a window. See the API reference for the test helpers.

## 배포 고지 / Distribution notices

예제를 복사할 때 `src/META-INF/little-lumi/`도 함께 보존하세요. SDK 생성 시 들어가는 기존 이용 조건·저작권·면책 고지는 빌드한 JAR에도 자동으로 포함됩니다. 추가한 코드와 외부 의존성의 고지는 별도로 넣으세요.

Keep `src/META-INF/little-lumi/` when copying this example. The existing terms, copyright and disclaimer supplied by the SDK builder are automatically included in the built JAR. Add notices for your own additions and dependencies separately.

창작마당 패커는 고지를 포함한 모든 파일을 보존합니다. 지원하지 않는 파일이나 경로가 있으면 일부 파일만 버린 패키지를 만들지 않고 실패합니다.

The Workshop packer preserves every file, including notices. Unsupported files or paths fail the operation instead of producing a partial package.
