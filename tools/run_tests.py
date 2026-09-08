#!/usr/bin/env python3
r"""Little LUMI mod SDK — build a plugin and run its headless tests.

    python tools\run_tests.py examples\hello
    python tools\run_tests.py my-plugin --javac C:\jdk-25\bin\javac.exe

A plugin folder may carry tests next to its sources:

    plugin.json
    src\**\*.java           the plugin (built by tools\build_plugin.py)
    test\*.java             plain classes with a `public static void main` — no test framework
                            needed. They get lib\*.jar + the freshly built plugin jar on the class
                            path and -Dplugin.dir=<the plugin folder>, so
                            PluginTestKit.start(Path.of(System.getProperty("plugin.dir"), "plugin.json"))
                            reads the real manifest.

PluginTestKit (in lib\Shimeji-ee.jar, package com.group_finity.mascot.lumi.plugin) hands the plugin a
PluginContext whose calls are recorded and whose readings are fakes — no app, no window, no Steam.
See docs\API.md 「테스트 킷」 / "Test kit". Each test's main runs in its own JVM; a non-zero exit is a
failure. The script exits 1 when any test fails, 0 otherwise.
"""
import argparse
import glob
import os
import re
import shutil
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
SDK = os.path.dirname(HERE)
LIB = os.path.join(SDK, 'lib')
BUILD_PLUGIN = os.path.join(HERE, 'build_plugin.py')


def find_javac(cli):
    for c in [cli, os.environ.get('JAVAC')]:
        if c and os.path.isfile(c):
            return c
    home = os.environ.get('JAVA_HOME')
    if home:
        for name in ('javac.exe', 'javac'):
            p = os.path.join(home, 'bin', name)
            if os.path.isfile(p):
                return p
    found = shutil.which('javac')
    if found:
        return found
    raise SystemExit('javac not found — --javac PATH, $JAVAC, $JAVA_HOME or javac on PATH (JDK 25)')


def java_next_to(javac):
    d = os.path.dirname(javac)
    for name in ('java.exe', 'java'):
        p = os.path.join(d, name)
        if os.path.isfile(p):
            return p
    return shutil.which('java') or 'java'


def class_name(source):
    text = open(source, encoding='utf-8').read()
    m = re.search(r'^\s*package\s+([\w.]+)\s*;', text, re.M)
    simple = os.path.splitext(os.path.basename(source))[0]
    return (m.group(1) + '.' + simple) if m else simple


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('plugin', help='plugin folder (holds plugin.json, src\\, test\\)')
    ap.add_argument('--javac', default=None)
    ap.add_argument('--keep', action='store_true', help='keep the temp folder (built jar + test classes)')
    args = ap.parse_args()

    plugin = os.path.abspath(args.plugin)
    tests = sorted(glob.glob(os.path.join(plugin, 'test', '*.java')))
    if not tests:
        raise SystemExit(f'no tests: {os.path.join(plugin, "test")}\\*.java')
    if not os.path.isdir(LIB):
        raise SystemExit(f'lib\\ missing next to tools\\ — run this from the unpacked SDK ({LIB})')
    javac = find_javac(args.javac)
    java = java_next_to(javac)

    work = tempfile.mkdtemp(prefix='lumi-plugin-tests-')
    try:
        # 1. the plugin jar, exactly as build_plugin.py makes it
        out = os.path.join(work, 'plugins')
        # --no-install-hint: this jar lands in a temp folder that is deleted below, so the builder's
        # closing "copy this into the game" line would point at a folder that no longer exists.
        r = subprocess.run([sys.executable, BUILD_PLUGIN, plugin, '--out', out, '--javac', javac,
                            '--no-install-hint'])
        if r.returncode != 0:
            raise SystemExit('plugin build failed')
        jars = glob.glob(os.path.join(out, '*.jar'))
        if len(jars) != 1:
            raise SystemExit(f'expected one jar in {out}, found {jars}')
        cp = os.pathsep.join(sorted(glob.glob(os.path.join(LIB, '*.jar'))) + jars)

        # 2. compile test\*.java against lib + the plugin jar
        classes = os.path.join(work, 'test-classes')
        os.makedirs(classes)
        r = subprocess.run([javac, '-encoding', 'UTF-8', '-d', classes, '-cp', cp] + tests)
        if r.returncode != 0:
            raise SystemExit('test compilation failed')

        # 3. run every main, one JVM each
        failed = []
        for src in tests:
            name = class_name(src)
            print(f'--- {name}')
            r = subprocess.run([java, '-Dfile.encoding=UTF-8', '-Dstdout.encoding=UTF-8',
                                f'-Dplugin.dir={plugin}', '-cp', cp + os.pathsep + classes, name])
            if r.returncode != 0:
                failed.append(name)
        print()
        if failed:
            print(f'FAILED: {", ".join(failed)}')
            sys.exit(1)
        print(f'OK: {len(tests)} test class(es) passed')
    finally:
        if args.keep:
            print(f'(kept {work})')
        else:
            shutil.rmtree(work, ignore_errors=True)


if __name__ == '__main__':
    main()
