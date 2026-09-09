# 동봉 고지 / Included notices

`APP-LICENSE.txt`와 `THIRD_PARTY_LICENSES.txt`는 앱의 고지 원문입니다. 앱 전체 고지에는 SDK에 없는 캐릭터 자산·앱 글꼴·JRE·Electron도 설명되어 있습니다. SDK에는 Javadoc용 글꼴·스크립트가 별도로 포함될 수 있습니다. 고지의 존재가 해당 구성요소의 동봉을 뜻하지는 않습니다.

`APP-LICENSE.txt` and `THIRD_PARTY_LICENSES.txt` reproduce the application's notices. They also describe character assets, application fonts, JRE and Electron that are absent from this SDK. Javadoc has its own support assets. A notice does not mean that every component it describes is bundled.

| SDK 구성 / Component | 라이선스와 고지 위치 / Notice |
|---|---|
| `lib/Shimeji-ee.jar` | 스톡 엔진 zlib·BSD-2-Clause 및 Kilkakon 고지: `APP-LICENSE.txt`; 자체 추가분: `LICENSE-SDK.txt`와 `APP-LICENSE.txt`; 내장 hqx: `THIRD_PARTY_LICENSES.txt` |
| `lib/`의 외부 JAR / Third-party JARs | 구성요소별 조건: `THIRD_PARTY_LICENSES.txt` 및 각 JAR의 `META-INF/` 고지 |
| JNA 5.19.1 내장 libffi / libffi inside JNA | MIT: `libffi-LICENSE.txt`. 원문: https://github.com/java-native-access/jna/blob/5.19.1/native/libffi/LICENSE |
| Nashorn / `lib/nashorn-core-15.7.jar` | GPL v2 + Classpath Exception: `NASHORN-LICENSE.txt`. 앱 고지의 `jre/legal/java.base/LICENSE` 대신 SDK에서는 이 파일을 확인합니다. Javadoc 생성 여부와 무관하게 동봉합니다. / This is the SDK path replacing the application's JRE path, even without Javadoc. |
| `docs/javadoc/`의 DejaVu 글꼴 | 생성된 `docs/javadoc/legal/dejavufonts.md`의 조건 / See the generated notice |
| Javadoc jQuery·jQuery UI | MIT: `docs/javadoc/legal/jquery.md`, `jqueryUI.md` |
| Javadoc Oracle JS·CSS·SVG | 각 파일 헤더의 UPL 1.0 고지 / UPL references in the file headers |
| `plugin-guide.html`, `plugin-guide.en.html`의 Google Fonts | 원격 자원 참조이며 글꼴 파일 직접 동봉은 아닙니다. / Remote references, not bundled font files. |

Javadoc 자산과 버전은 생성에 사용한 JDK에 따라 달라집니다. 생성된 파일과 그 고지를 함께 보존하세요. Nashorn 소스 링크는 앱 고지의 Source Code Availability에 있습니다. 대응 소스·빌드 자료·재결합 도구는 SDK의 `docs/sources/third-party-sources.zip`에 함께 동봉합니다.

Javadoc assets depend on the generating JDK; preserve their generated notices. Nashorn source links are in Source Code Availability in the application notices. The corresponding sources, build materials and recombination helper accompany this SDK in `docs/sources/third-party-sources.zip`.

예제의 `src/META-INF/little-lumi/`에는 SDK 생성기가 기존 SDK 이용 조건과 앱 고지를 복사합니다. 예제 빌더가 이를 JAR 내부에 그대로 넣습니다. 이는 원래 예제 부분의 고지 보존용이며 모드 작성자의 새 코드 전체를 STUDIO LUMI 소유로 표시하거나 앱·외부 라이브러리를 예제 JAR에 동봉한다는 뜻이 아닙니다. 새 코드와 추가 의존성의 고지는 작성자가 별도로 추가하세요.

The SDK builder copies the existing SDK terms and application notices to each example's `src/META-INF/little-lumi/`; the plugin builder carries them into its JAR. These preserve notices for the original example, do not claim ownership of your additions, and do not imply that the app or its libraries are embedded in the example JAR. Add notices for your own code and additional dependencies separately.

앱 고지의 `LICENSE.txt`는 SDK에서 같은 폴더의 `APP-LICENSE.txt`에 해당합니다. 앱 고지가 참조하는 크레딧은 같은 폴더에 원문 동봉한 `CREDITS.txt`에서 확인합니다. 과거 Launch4j 문구에 대한 현행 C# 런처 설명은 `APP-LICENSE.txt` 끝의 정정 고지를 확인하세요. SDK에는 런처 EXE가 없습니다.

In the application notices, `LICENSE.txt` maps to `APP-LICENSE.txt` in this folder. The referenced `CREDITS.txt` is also reproduced here. The clarification at the end of `APP-LICENSE.txt` identifies the current C# launcher instead of the historical Launch4j reference; the SDK contains no launcher EXE.

제3자 소스·빌드·재결합 방법은 [대응 소스 안내](../sources/README.md)를 확인하세요. / See the [corresponding-source guide](../sources/README.md).
