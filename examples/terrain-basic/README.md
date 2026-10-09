# 발판 하나 예제

지형 API의 등록·갱신·해제를 보여주는 작은 예제입니다. 트레이 메뉴 세 개로 초록색 발판 하나를 표시하고, 너비를 바꾸고, 제거합니다.

| 메뉴 | API |
|---|---|
| SDK: 발판 표시 | `ctx.registerTerrain("platform", geometry)` |
| SDK: 발판 너비 변경 | `terrain.update(geometry)` |
| SDK: 발판 제거 | `terrain.remove()` |

발판의 초록색 막대는 제목 없는 `JWindow`로 표시합니다. 막대 위치와 지형에는 같은 바탕화면 논리 좌표를 사용합니다. 갱신할 때 조각 ID `bar`를 유지하며, 종료 시에는 지형과 창을 함께 제거합니다. 바탕화면 모드에서 사용합니다.

SDK 폴더에서 빌드하고 테스트합니다.

```powershell
python tools\build_plugin.py examples\terrain-basic
python tools\run_tests.py examples\terrain-basic
```

생성된 `dist\lumi.sdk.example.terrain` 폴더를 앱 설치 폴더의 `mods` 아래에 복사합니다. 설정 → 모드에서 코드 실행에 동의하고 앱을 다시 실행하면 트레이 메뉴가 생깁니다. 이 예제는 `registerTerrain`이 포함된 앱이 필요합니다. 이전 앱은 같은 `api_version`이어도 해당 메서드가 없을 수 있습니다.

꼬미를 막대 위에서 놓아 착지를 확인할 수 있습니다. 너비를 줄이거나 발판을 제거하면 지형도 다음 이동 틱에 바뀝니다. 동봉 테스트는 창을 열기 전의 메뉴 등록과 종료를 확인합니다. 실제 화면의 좌표 정렬과 움직임은 앱에서 확인합니다. 자세한 계약은 [API.md](../../docs/API.md)의 지형 절을 참고합니다.
