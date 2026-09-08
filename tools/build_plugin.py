#!/usr/bin/env python3
r"""Little LUMI mod SDK — build a plugin jar from a plugin folder.

    python tools\build_plugin.py examples\plugin-transformer            → dist\<id>\plugins\<id>.jar
    python tools\build_plugin.py my-plugin --out C:\path\to\mods\my-plugin\plugins

A plugin folder holds:
    plugin.json                 the manifest (id, name, version, dependencies, api_version, ...)
    src\**\*.java               sources, compiled against lib\Shimeji-ee.jar + lib\*.jar
    src\**\(anything else)      copied into the jar as-is — put the
                                META-INF\services\com.group_finity.mascot.lumi.plugin.LumiPlugin
                                (and ...LumiTransformer) files here, naming your classes

The jar is named after the manifest's "id". The mod is the folder that contains plugins\<id>.jar:
copy it to the game's mods\<name>\ and restart — Settings → Mods lists it with a plugin chip.
Several plugins can be active at once (mod order = apply order).
Needs a JDK 25: --javac PATH, $JAVAC, $JAVA_HOME, or javac on PATH.
"""
import argparse
import glob
import json
import os
import shutil
import subprocess
import sys
import tempfile
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
SDK = os.path.dirname(HERE)
LIB = os.path.join(SDK, 'lib')
MAIN_JAR = os.path.join(LIB, 'Shimeji-ee.jar')


def find_javac(cli):
    candidates = [cli, os.environ.get('JAVAC')]
    home = os.environ.get('JAVA_HOME')
    if home:
        candidates.append(os.path.join(home, 'bin', 'javac.exe' if os.name == 'nt' else 'javac'))
    for c in candidates:
        if c and os.path.isfile(c):
            return c
    found = shutil.which('javac')
    if found:
        return found
    raise SystemExit('javac not found — install a JDK 25 and pass --javac, or set JAVA_HOME')


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('plugin_dir')
    ap.add_argument('--out', default=None,
                    help='folder for the jar (default: <SDK folder>\\dist\\<id>\\plugins — resolved '
                         'against the SDK folder, not the current one)')
    ap.add_argument('--no-install-hint', action='store_true',
                    help='skip the closing "copy this into the game" line (used by tools\\run_tests.py)')
    ap.add_argument('--javac', default=None)
    args = ap.parse_args()

    plugin_dir = os.path.abspath(args.plugin_dir)
    manifest_path = os.path.join(plugin_dir, 'plugin.json')
    if not os.path.isfile(manifest_path):
        raise SystemExit(f'plugin.json missing: {manifest_path}')
    with open(manifest_path, encoding='utf-8') as fh:
        manifest = json.load(fh)
    plugin_id = manifest.get('id', '')
    if not plugin_id:
        raise SystemExit('plugin.json has no "id"')
    if not os.path.isfile(MAIN_JAR):
        raise SystemExit(f'{MAIN_JAR} missing — this SDK folder is incomplete')
    javac = find_javac(args.javac)
    out_dir = args.out or os.path.join(SDK, 'dist', plugin_id, 'plugins')

    src = os.path.join(plugin_dir, 'src')
    sources, resources = [], []
    for dirpath, _, files in os.walk(src):
        for f in files:
            full = os.path.join(dirpath, f)
            (sources if f.endswith('.java') else resources).append(full)

    work = tempfile.mkdtemp(prefix='lumi-plugin-')
    try:
        classes = os.path.join(work, 'classes')
        os.makedirs(classes)
        if sources:
            cp = os.pathsep.join(sorted(glob.glob(os.path.join(LIB, '*.jar'))))
            cmd = [javac, '--release', '25', '-encoding', 'UTF-8', '-cp', cp, '-d', classes] + sources
            subprocess.run(cmd, check=True)
        os.makedirs(out_dir, exist_ok=True)
        out = os.path.join(out_dir, plugin_id + '.jar')
        with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as jar:
            jar.write(manifest_path, 'plugin.json')
            for dirpath, _, files in os.walk(classes):
                for f in files:
                    full = os.path.join(dirpath, f)
                    jar.write(full, os.path.relpath(full, classes).replace(os.sep, '/'))
            for full in resources:
                jar.write(full, os.path.relpath(full, src).replace(os.sep, '/'))
        print(f'built: {out}  ({plugin_id}, {len(sources)} source(s), {len(resources)} resource(s))')
        # The loader only ever reads mods\<name>\plugins\*.jar, so what gets copied into the game is
        # the folder ABOVE plugins\. Decide that on the normalised path, never on the raw string: a
        # trailing separator or a folder by another name used to send this line somewhere wrong.
        if not args.no_install_hint:
            out_norm = os.path.normpath(os.path.abspath(out_dir))
            if os.path.basename(out_norm) == 'plugins':
                print(f'done: copy {os.path.dirname(out_norm)} into the game as mods\\<name>\\ and restart')
            else:
                print(f'done: {out_norm} is not a plugins\\ folder — the loader only reads '
                      f'mods\\<name>\\plugins\\*.jar, so move {plugin_id}.jar there (or rebuild with '
                      f'--out <mod folder>\\plugins) and restart')
    finally:
        shutil.rmtree(work, ignore_errors=True)


if __name__ == '__main__':
    main()
