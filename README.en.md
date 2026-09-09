<!-- Generated from the SDK's README.en.md by build/make_sdk_public_repo.py. Do not edit by hand. -->
# Little LUMI Model — Plugin SDK

[한국어](README.md) · [Downloads (Releases)](https://github.com/studio-lumi-h/little-lumi-sdk/releases)

> **This repository holds the readable parts only** — docs, examples, tools and the public API sources.
> **To build a plugin, download the ZIP from [Releases](https://github.com/studio-lumi-h/little-lumi-sdk/releases)**: the `lib/` jars you compile
> against and the generated javadoc live there. This is version **1.3.0a**.

---

Welcome. This SDK is the toolkit for adding your own things to Little LUMI: make her react to clicks, head pats and alarms, add lines, menus and hotkeys, or build a mod out of nothing but data — artwork, behaviours, translations.

This distribution is **SDK 1.3.0a / app 1.3.0 / api_version 1**.

Open [index.html](index.html) for the offline document collection. Code examples have copy buttons; the original Markdown and licence notices are preserved.

## What is in here

| Folder or file | What it is for |
|---|---|
| `index.html` | offline API, catalogue, example and licence readers |
| `plugin-guide.en.html` | the walkthrough, start to finish |
| `lib\` | the jars you compile a plugin against |
| `tools\` | scripts that build, test and prepare for the Workshop |
| `examples\` | two samples you can build as they are |
| `docs\` | API reference · identifier catalogue · changelog · javadoc |
| `sdk.json` | this distribution's versions, and the hooks and events this app offers |
| `LICENSE-SDK.txt` | terms of use |

## New here

Open [**plugin-guide.en.html**](plugin-guide.en.html). That is the place to start.

It takes you in order from installing the tools to seeing Lumi say a line you wrote, and it assumes you have never programmed before. The Korean page is [plugin-guide.html](plugin-guide.html).

## In a hurry

With JDK 25 and Python 3 installed, three lines in PowerShell from the unpacked SDK folder:

```powershell
python tools\build_plugin.py examples\hello
python tools\run_tests.py examples\hello
Copy-Item -Recurse dist\hello.lumi.sample "<install folder>\mods\hello.lumi.sample"
```

The first line builds the example into `dist\hello.lumi.sample\`, the second tests it without the app, the third installs that whole folder. Close the app before you copy and start it again afterwards — a row for the example appears under Settings → Mods.

## Where to go next

| Document | What it holds |
|---|---|
| [plugin-guide.en.html](plugin-guide.en.html) | the walkthrough — from knowing nothing |
| [docs/API.en.md](docs/API.en.md) | everything you can do |
| [docs/CATALOG.en.md](docs/CATALOG.en.md) | names of behaviours, events, hooks and settings keys |
| [docs/CHANGELOG.md](docs/CHANGELOG.md) | what changed in each version |
| `examples\` | two samples — a greeting plugin and a bytecode transformer |
| `tools\` | building and testing, and the check before a Workshop upload |

## Worth knowing

- **SDK 1.3.0a is a revision for app 1.3.0.** It updates the example activation instructions and build-tool messages. `sdk_version` is `1.3.0a`, `app_version` is `1.3.0`, and `api_version` remains `1`.
- `lib\Shimeji-ee.jar` is **compile-only**. It is the Steam build as shipped, so the SDK alone cannot start the app — try your work in your purchased install.
- A plugin runs **inside the app with your rights** (no sandbox). Tell the people you share it with.
- Terms of use are in [LICENSE-SDK.txt](LICENSE-SDK.txt).

What stays and what may change as the app moves to a new version is written up in [API.en.md "Compatibility"](docs/API.en.md#compatibility).

## Code mod consent

Installing or subscribing does not authorize execution. Enable the mod's checkbox, review the warning, and agree; code and bundled data are applied on the next app start. Existing last-run hashes are not approval. Changes to any plugin JAR or its `plugins/lib` libraries require approval again. Creator-declared network/behavior fields are informational, not enforced permissions.

Library JARs must belong to the same approved mod as a plugin; unrelated library-only mods are not placed on the class path. Put dependencies in `plugins/lib` instead of a JAR manifest `Class-Path`; manifest class paths and JAR indexes are rejected. The loader executes verified copies, so plugins must not rely on their CodeSource path being their install directory.

Code paths containing symbolic links or junctions are unsupported.

When new or changed code mods need approval, a startup notice opens. Its **OK** button only opens Settings → Mods; it does not grant consent. Review the red **Unapproved** badge and enable the mod checkbox to give explicit consent.

Declining consent sets the toggle to **− (unapproved lock)** and suppresses startup reminders for that code version. **Click − to return to OFF**. Updating a JAR or library clears the lock. Locking never grants consent; code and data remain off. Closing with Escape or X leaves approval and lock state unchanged.
