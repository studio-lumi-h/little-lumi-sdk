# Single platform example

A small demonstration of terrain registration, updates and removal. Three tray commands show one green platform, change its width and remove it.

| Menu | API |
|---|---|
| SDK: show platform | `ctx.registerTerrain("platform", geometry)` |
| SDK: change platform width | `terrain.update(geometry)` |
| SDK: remove platform | `terrain.remove()` |

An untitled `JWindow` paints the bar. The window and terrain use the same desktop logical coordinates. Updates retain the piece ID `bar`; stopping removes both the terrain and the window. Use desktop mode.

Build and test from the SDK directory:

```powershell
python tools\build_plugin.py examples\terrain-basic
python tools\run_tests.py examples\terrain-basic
```

Copy `dist\lumi.sdk.example.terrain` into the app installation's `mods` directory. Approve code execution under Settings → Mods and restart the app to enable the tray commands. This example requires an app containing `registerTerrain`; earlier apps with the same `api_version` may lack the method.

Drop LUMI above the bar to check landing. Resizing or removing it changes the terrain on the next movement tick. The included test checks menu registration and stopping before a window is opened. Check visual alignment and movement in the app. See the terrain section of [API.en.md](../../docs/API.en.md).
