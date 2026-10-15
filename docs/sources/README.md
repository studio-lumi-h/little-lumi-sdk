# hqx·Nashorn 대응 소스와 hqx 재결합 / Corresponding sources and hqx recombination

이 묶음은 앱 및 SDK에 실제 포함된 hqx와 Nashorn의 대응 소스·빌드 자료입니다. 제3자 소스는 원래 라이선스에 따라 사용·수정·재배포할 수 있으며 SDK의 목적·유료 배포·재배포 제한이 덮어쓰지 않습니다. `sources.json`은 고정 upstream 리비전과 파일·바이너리 해시를 기록합니다. 자체 앱 코드나 캐릭터 원본은 포함하지 않습니다.

This bundle contains the corresponding hqx and Nashorn sources and build materials for the shipped binaries. Their original licences govern use, modification and redistribution; the SDK's purpose, paid-distribution and redistribution restrictions do not override those rights. `sources.json` records fixed upstream revisions and source/binary hashes. It contains no proprietary app source or character artwork.

## 제공 방식 / Distribution method

SDK에는 `docs/sources/third-party-sources.zip`, 앱 배포에는 최상위 `third-party-sources.zip`으로 바이너리와 함께 제공합니다. 앱의 나머지 부분은 전달된 `Shimeji-ee.jar`의 객체 코드로 재결합할 수 있습니다. 이 파일을 삭제하지 말고 hqx 또는 Nashorn 바이너리를 재배포할 때 적용 라이선스에 필요한 소스·고지·재결합 자료도 함께 제공하세요. 별도 요청이나 다운로드 승인 없이 압축을 풀어 사용할 수 있습니다.

The source bundle accompanies the binaries as `docs/sources/third-party-sources.zip` in the SDK and `third-party-sources.zip` at the application distribution root. The supplied app JAR provides the remaining application object code needed for recombination. Preserve the applicable source, notices and recombination materials with redistributed hqx/Nashorn binaries. Extraction requires no separate request or download approval.

## hqx

- 출처 / Source: https://github.com/DalekCraft2/Shimeji-Desktop/tree/dea89528c10c066626a09609f0e742cbe6405a8d/src/main/java/hqx
- 수정하지 않은 네 소스의 저작자: Maxim Stepin, Cameron Zemek, Tamme Schichler, A. Eduardo García. 각 파일의 원래 헤더를 보존했습니다.
- LGPL-3.0-or-later. `hqx/COPYING.LESSER.txt`와 `hqx/COPYING.txt`를 함께 읽으세요. 원래 Maven 설정은 `hqx/upstream-pom.xml`에 보존했습니다. hqx 자체는 Java 외의 의존성이 없습니다.
- JDK 25.0.4에서 아래 명령으로 컴파일한 네 클래스가 스톡 1.0.22 및 앱/SDK hqx 클래스와 모두 바이트 단위로 일치합니다. 다른 JDK는 디버그 정보 등으로 바이트가 달라질 수 있습니다.

The four unmodified source files retain their original author and LGPL-3.0-or-later headers. The original Maven configuration is preserved separately. hqx needs no dependency beyond Java. Compiling with JDK 25.0.4 and the command below reproduces all four shipped class files byte for byte; other compilers can differ in debug information.

압축을 푼 폴더에서 실행합니다. `javac`는 JDK 25의 도구이며 경로에 공백이 있으면 따옴표로 묶습니다. Windows·Linux에서 같은 인자를 사용합니다.

Run from the extracted directory using JDK 25's javac (quote paths with spaces):

```text
javac --release 25 -g -encoding UTF-8 -d classes hqx/src/main/java/hqx/Hq2x.java hqx/src/main/java/hqx/Hq3x.java hqx/src/main/java/hqx/Hq4x.java hqx/src/main/java/hqx/Util.java
python relink_hqx.py --input PATH/TO/Shimeji-ee.jar --classes classes --out Shimeji-ee.modified.jar
```

1. 변경할 hqx 소스를 편집하고 위 명령으로 네 파일을 함께 컴파일합니다. 공개 클래스·메서드의 인터페이스를 호환되게 유지하세요.
2. 앱 설치본의 `app/Shimeji-ee.jar` 또는 SDK의 `lib/Shimeji-ee.jar`를 `--input`으로 지정합니다. 도구는 새 출력만 만들고 입력·기존 출력은 덮어쓰지 않습니다. `hqx/` 클래스만 교체하며 같은 이름의 중복 클래스를 남기지 않습니다. 나머지 엔트리·Steam 게이트·매니페스트의 바이트는 그대로입니다.
3. 실제 앱에서 사용하려면 앱을 완전히 종료한 뒤 원래 JAR를 별도 위치에 백업하고, 출력 파일을 설치본의 `app/Shimeji-ee.jar`로 복사합니다. 파일을 잠그는 앱이 실행 중이면 먼저 종료하세요. 모드의 `plugins/`에 넣는 방법이 아닙니다. 기존 Steam 소유권 조건과 실행 경로는 그대로입니다.
4. 앱의 HQX 확대 경로에서 수정 동작을 확인합니다. 복구는 앱 종료 후 백업 JAR를 원위치에 복사하거나 Steam 파일 검사를 사용합니다. Steam 업데이트·파일 검사는 수정 JAR를 원본으로 되돌릴 수 있습니다. 개인 설정·콘텐츠를 삭제할 필요는 없습니다.

Modify the sources, compile all four compatible classes, and supply the installed app JAR or SDK app JAR as input. The helper creates a separate output, replaces only hqx classes without duplicates, and preserves every other entry including the manifest and Steam gate. To use it in the installed app, quit the app, back up `app/Shimeji-ee.jar`, and copy the output there under the original name. Do not put it in `plugins/`. Verify modified behaviour with HQX scaling. Restore the backup while the app is closed, or use Steam verification; updates/verification may restore the original. No user settings or content need deletion.

이 재결합·해당 라이브러리 부분의 수정 및 수정 디버깅 목적 역공학은 허용됩니다. 배포 JAR는 서명되지 않았으므로 이 경로에 별도 서명 키가 필요하지 않습니다. 별도 서명된 JAR에는 도구가 명확히 실패합니다.

Recombination, modification of the library portion and reverse engineering for debugging those modifications are permitted. The shipped app JAR is unsigned, so no private signing key is required. The helper refuses separately signed JARs.

## Nashorn 15.7 + Little LUMI host-access patch

- 완전한 고정 원본 소스 / Complete pinned upstream source: `nashorn/nashorn-15.7-source.zip`
- 출처 / Source: https://github.com/openjdk/nashorn/tree/4923f1e70e460d87c0eeb1f5232344487d46108d (`release-15.7`)
- 수정 내역 / Local changes: `nashorn/classfilter-host-access.patch` (2026-10-12).
- GPL v2 + Classpath Exception. 스냅숏의 `LICENSE`, `ADDITIONAL_LICENSE_INFO`, 파일 헤더를 보존합니다. 원본 소스·리소스·Ant 빌드 파일·nasgen·테스트 전체와 수정 패치를 함께 제공합니다.
- 원본 Java 589개는 Maven Central 15.7 sources.jar와 일치합니다. 동봉 바이너리는 원본에 패치를 적용해 재빌드한 `15.7-lumi.2`이며 Maven 원본 바이너리와 다릅니다. 파일명은 기존 로더 호환성을 위해 `nashorn-core-15.7.jar`를 유지합니다. 원본/수정본 해시와 패치 해시는 `sources.json`에 있습니다.

패치는 ClassFilter가 있는 엔진에서 engine/context 자동 노출, 호스트 스크립트 로딩, 프로세스 종료, scripting 모드 및 JSR-223 호스트 객체 접근을 제한합니다. ClassFilter가 없는 신뢰된 엔진은 기존 동작을 유지합니다. 이 변경은 임의의 Java 호스트 객체나 CPU/메모리 사용량을 격리하는 완전한 샌드박스를 제공하지 않습니다.

The shipped binary is locally modified (`15.7-lumi.2`), not the Maven Central binary. The patch restricts implicit engine/context access, host loading, process exit, scripting mode and JSR-223 host capabilities when a ClassFilter is present. Unfiltered engines retain their original behavior. Arbitrary bound Java objects and CPU/memory consumption are not isolated by this patch.

Python 3, Git, JDK 11 이상과 앱/SDK의 ASM 7.3.1 JAR 다섯 개로 오프라인 재빌드합니다. 확인한 환경은 JDK 25.0.4.1입니다. 압축 해제한 이 소스 묶음의 루트에서 다음 명령을 실행합니다. `--lib`는 앱 또는 SDK의 lib 폴더입니다.

Rebuild offline with Python 3, Git, JDK 11+ and the five ASM 7.3.1 JARs from the app/SDK. Verified with JDK 25.0.4.1. From this extracted source bundle's root:

```text
python nashorn/rebuild.py --lib <app-or-sdk-lib> --output <output>/nashorn-core-15.7.jar
```

도구는 원본 및 패치 해시를 검증하고 임시 폴더에서 패치 적용 → 전체 Java 컴파일 → upstream nasgen → 리소스/서비스 등록 → JAR 생성을 수행합니다. 중간 파일은 자동 정리합니다. 같은 JDK의 반복 빌드를 검사했으며 다른 JDK의 바이트 단위 동일성은 보장하지 않습니다. 패치를 바꾸면 회귀 검사 후 `sources.json`의 패치·바이너리 해시를 함께 갱신해야 합니다. 원본 ZIP의 Ant 절차만 실행하면 보안 패치가 없는 원본 엔진이 만들어집니다.

The helper checks pinned source/patch hashes, compiles all engine sources, runs upstream nasgen, and packages resources/services in an OS temporary directory. Different JDKs need not produce byte-identical output. Updating the patch requires regression checks and updating both patch/binary hashes in `sources.json`. Running only the upstream Ant build produces the unpatched engine.

`relink_hqx.py`와 `nashorn/rebuild.py`는 STUDIO LUMI가 작성한 빌드 도구이며 `HELPER-LICENSE.txt`의 BSD-3-Clause 조건으로 별도 제공됩니다. 이 파일과 hqx·Nashorn의 라이선스를 서로 혼동하지 마세요.

The STUDIO LUMI build helpers are separately provided under BSD-3-Clause in `HELPER-LICENSE.txt`; it does not replace the hqx or Nashorn licences.
