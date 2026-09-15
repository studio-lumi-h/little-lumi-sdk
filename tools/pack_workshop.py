#!/usr/bin/env python3
r"""Little LUMI Workshop package preparation.

python tools/pack_workshop.py <mod-folder> [--out <folder>] [--check] [--strict]

Checks package paths, supported executable locations, limits and Windows PE headers.
The app performs complete content validation and asks for execution consent at startup.
Voice/character packages may contain plugins; every file belongs to one package.
Unsupported files fail the operation instead of being silently omitted. Original files
and existing output are preserved on validation failure. Existing item.vdf metadata is retained.

모드·캐릭터·보이스팩 전체를 포장합니다. 지원하지 않는 파일·경로가 있으면 실패하며
일부 파일만 버린 불완전한 패키지는 만들지 않습니다. 앱에서 내용 검사와 실행 동의를
추가로 확인합니다. --strict는 이전 호출과의 호환용이며 현재는 항상 누락을 거부합니다.
"""
import argparse
import os
import re
import shutil
import sys
import tempfile
import struct
from pathlib import Path, PurePosixPath

HERE = Path(__file__).resolve().parent
SDK = HERE.parent

# ── WorkshopSync 의 규칙 (그대로 옮긴 것) ──────────────────────────────────────

# ALLOWED_SUFFIXES — 경로 어디에 있든 이 확장자만 갑니다.
ALLOWED_SUFFIXES = {'.png', '.xml', '.txt', '.wav', '.tsv', '.properties', '.json', '.ttf', '.otf'}

# REFUSED_RELATIVE — 확장자가 맞아도 절대 가지 않는 이름. 런처가 오버레이에서 막는 것 가운데
# 사용자 상태 파일 열한 개입니다(런처의 ProtectedFiles 는 더 넓어서 jar·exe·dll 과
# conf\preferences.properties·garden\ 진입 파일까지 막고, jre\ lib\ speech\ 는 뿌리째 막습니다).
REFUSED_RELATIVE = {
    'conf/steam.properties', 'conf/settings.properties', 'conf/ai.properties',
    'conf/workshop.properties', 'conf/window_spots.properties',
    'conf/plugins.properties', 'conf/logging.properties',
    'conf/shortcuts.json', 'conf/shortcuts.json.bak',
    'conf/affection.json', 'conf/affection.json.bak',
}

# MAX_ITEM_BYTES — 이미지셋·모드 항목 하나의 크기 상한 (WorkshopSync.MAX_ITEM_BYTES).
MAX_ITEM_BYTES = 4 * 1024 * 1024 * 1024

# MAX_VOICE_PACK_BYTES — 보이스팩만 상한이 다릅니다 (WorkshopSync.MAX_VOICE_PACK_BYTES).
# 무압축 wav 라 간체·번체를 함께 든 중국어 팩이 931 MiB 였고 768 MiB 로는 거부됐습니다.
MAX_VOICE_PACK_BYTES = 4 * 1024 * 1024 * 1024

DEFAULT_APPID = '5075020'

# 종류 이름
KIND_PLUGIN = 'plugin item'
KIND_MOD = 'mod layout'
KIND_IMAGE_SET = 'IMAGE SET'
KIND_VOICE_PACK = 'VOICE PACK'
KIND_UNKNOWN = 'not recognised'

KIND_KO = {
    KIND_PLUGIN: '플러그인 항목',
    KIND_MOD: '모드 배치',
    KIND_IMAGE_SET: '이미지셋(캐릭터) 항목',
    KIND_VOICE_PACK: '보이스팩 항목',
    KIND_UNKNOWN: '알 수 없음',
}

# 빠지는 사유
DROP_EXTENSION = '허용하지 않는 확장자'
DROP_PLACE = '자리가 틀림'
DROP_KIND = '이 종류에서는 안 가는 파일'
DROP_PROTECTED = '보호된 이름'
DROP_LINK = '바로가기·링크'

DROP_HELP = {
    DROP_EXTENSION: '지원 실행 경로 또는 파일 헤더 검사를 통과하지 못했습니다.',
    DROP_PLACE: '확장자는 맞지만 놓인 자리가 규칙 밖입니다.',
    DROP_KIND: '패키지 구성을 확인하세요.',
    DROP_PROTECTED: '사용자의 상태 파일이라 항목으로는 못 보냅니다.',
    DROP_LINK: '링크는 따라가지 않습니다 — 진짜 파일을 넣으세요.',
}

# 확장자별 한 줄 처방
EXTENSION_FIX = {
    '.mp3': '소리는 .wav 로 바꾸세요.',
    '.ogg': '소리는 .wav 로 바꾸세요.',
    '.m4a': '소리는 .wav 로 바꾸세요.',
    '.flac': '소리는 .wav 로 바꾸세요.',
    '.jpg': '그림은 .png 로 바꾸세요.',
    '.jpeg': '그림은 .png 로 바꾸세요.',
    '.gif': '그림은 .png 로 바꾸세요.',
    '.bmp': '그림은 .png 로 바꾸세요.',
    '.webp': '그림은 .png 로 바꾸세요.',
    '.md': '설명은 .txt 로 바꾸세요.',
    '.html': '설명은 .txt 로 바꾸세요.',
    '.zip': '압축은 풀어서 넣으세요.',
    '.7z': '압축은 풀어서 넣으세요.',
    '.rar': '압축은 풀어서 넣으세요.',
    '.java': '소스는 빼고 빌드한 jar 만 plugins\\ 에 넣으세요.',
    '.class': '낱개 class 는 안 갑니다 — jar 로 묶어 plugins\\ 에 넣으세요.',
    '.exe': '실행 파일은 절대 가지 않습니다 — 빼세요.',
    '.dll': '실행 파일은 절대 가지 않습니다 — 빼세요.',
    '.bat': '실행 파일은 절대 가지 않습니다 — 빼세요.',
    '.cmd': '실행 파일은 절대 가지 않습니다 — 빼세요.',
    '.ps1': '실행 파일은 절대 가지 않습니다 — 빼세요.',
    '.sh': '실행 파일은 절대 가지 않습니다 — 빼세요.',
    '.vbs': '실행 파일은 절대 가지 않습니다 — 빼세요.',
    '.yml': '설정은 .json 이나 .properties 로 바꾸세요.',
    '.yaml': '설정은 .json 이나 .properties 로 바꾸세요.',
    '.ini': '설정은 .properties 로 바꾸세요.',
    '.cfg': '설정은 .properties 로 바꾸세요.',
    '.csv': '표는 .tsv 로 바꾸세요(탭으로 나눕니다).',
}

FIX_NO_EXTENSION = '확장자가 없는 파일은 가지 않습니다 — .txt 를 붙이세요.'
FIX_JAR_PLACE = 'jar 은 plugins\\ 바로 아래나 plugins\\lib\\ 한 겹까지만 갑니다.'
FIX_PY_PLACE = 'py 는 tools\\ 한 겹만입니다 — tools\\<이름>.py 로 옮기세요.'
FIX_PROTECTED = '앱이 쓰는 사용자 파일입니다 — 다른 이름을 쓰거나 빼세요.'

# 자리 예외(plugins\*.jar · tools\*.py)는 모드 항목에서만 삽니다 — WorkshopSync 에서
# copyTree 를 withPlugins 로 부르는 곳은 mirrorPluginMod 하나뿐입니다. 그래서 다른 종류에서는
# 「자리를 옮기세요」가 거짓말이 됩니다(이미 옳은 자리에 둔 파일도 안 가니까요).
FIX_KIND_IMAGE_SET = ('이 폴더는 캐릭터로 읽혀 jar·py 가 어디에 있어도 안 갑니다 — 뿌리의 '
                      'shime1.png · conf\\actions.xml 을 img\\<이름>\\ 아래로 내려 모드 배치로 만드세요.')
FIX_KIND_VOICE_PACK = ('이 폴더는 보이스팩으로 읽혀 voice\\ 아래만 갑니다 — 모드로 만들려면 '
                       'voice\\index.tsv 를 빼세요.')
FIX_KIND_OTHER = ('jar·py 는 모드 항목에서만 갑니다 — plugins\\<이름>.jar 을 넣거나 '
                  'img\\ · conf\\ 로 시작하는 모드 배치로 만드세요.')


def win(path):
    """메시지에 넣을 윈도 표기(역슬래시)."""
    return str(path).replace('/', '\\')


# ── 종류 판정 (WorkshopSync.mirror 의 차례 그대로) ─────────────────────────────

def is_voice_pack(root: Path) -> bool:
    """voice\\index.tsv 하나로 정해집니다."""
    return (root / 'voice' / 'index.tsv').is_file()


def is_plugin_item(root: Path) -> bool:
    """plugins\\ 바로 아래에 .jar 이 하나라도 있으면 플러그인 항목입니다."""
    plugins = root / 'plugins'
    if not plugins.is_dir():
        return False
    return any(p.is_file() and p.name.lower().endswith('.jar') for p in plugins.iterdir())


def looks_like_image_set(root: Path) -> bool:
    """뿌리의 conf\\actions.xml 이나 shime1.png — 앱이 이 폴더를 캐릭터 길로 보내는 표시입니다.

    둘 중 하나만 있어도 모드 길(isModLayout)이 닫히고 캐릭터 길로 갑니다. 다만 캐릭터로
    **실리는** 조건은 그보다 빡빡합니다 — image_set_missing() 을 보세요.
    """
    return (root / 'conf' / 'actions.xml').is_file() or (root / 'shime1.png').is_file()


def image_set_missing(root: Path):
    """캐릭터로 실리려면 있어야 하는데 없는 파일 (ImageSetCheck.reject 의 첫 두 검사).

    앱은 conf\\actions.xml 과 conf\\behaviors.xml 을 **둘 다** 요구합니다. 하나라도 없으면
    항목이 통째로 거부되어 아무것도 미러되지 않습니다 — 뿌리에 shime1.png 만 둔 폴더가
    바로 그 경우입니다(캐릭터 길로 가되, 캐릭터로 실리지는 못합니다).
    """
    return [name for name in ('conf\\actions.xml', 'conf\\behaviors.xml')
            if not (root / name.replace('\\', '/')).is_file()]


def is_mod_layout(root: Path) -> bool:
    if looks_like_image_set(root):
        return False
    return any((root / name).is_dir() for name in ('img', 'plugins', 'conf', 'voice', 'sound', 'native', 'audio-native')) or (root / 'mod.json').is_file()


def detect_kind(root: Path):
    """(종류, 근거 한 줄) — 앱이 보는 차례 그대로 판정합니다."""
    if is_voice_pack(root):
        return KIND_VOICE_PACK, 'voice\\index.tsv 가 있습니다 — 앱은 이 파일 하나로 보이스팩이라고 정합니다.'
    if is_plugin_item(root):
        jars = sorted(p.name for p in (root / 'plugins').iterdir()
                      if p.is_file() and p.name.lower().endswith('.jar'))
        return KIND_PLUGIN, f'plugins\\{jars[0]} — plugins\\ 바로 아래에 .jar 이 있습니다.'
    if is_mod_layout(root):
        for name in ('img', 'plugins', 'conf', 'voice', 'sound', 'native', 'audio-native'):
            if (root / name).is_dir():
                return KIND_MOD, f'{name}\\ 폴더가 있는 모드 패키지입니다.'
    if looks_like_image_set(root):
        witness = 'conf\\actions.xml' if (root / 'conf' / 'actions.xml').is_file() else 'shime1.png'
        return KIND_IMAGE_SET, f'뿌리에 {witness} 가 있습니다 — 앱은 이 폴더를 캐릭터 길로 보냅니다.'
    if (root / 'mod.json').is_file():
        return KIND_MOD, 'mod.json으로 선언된 데이터 패키지입니다.'
    return KIND_UNKNOWN, '모드의 콘텐츠 디렉터리나 mod.json이 없습니다.'


def item_scope(root: Path, kind: str):
    """All package kinds retain their complete inventory, including mixed-in code."""
    return root, '', MAX_ITEM_BYTES


# ── 파일 하나 판정 (WorkshopSync.copyTree 의 차례 그대로) ──────────────────────

def is_refused(rel: PurePosixPath) -> bool:
    return str(rel).lower() in REFUSED_RELATIVE


EXECUTABLE_SUFFIXES = {
    '.exe', '.dll', '.com', '.scr', '.cpl', '.msi', '.msp', '.class', '.jmod', '.war', '.ear',
    '.so', '.dylib', '.wasm', '.bat', '.cmd', '.ps1', '.psm1', '.psd1', '.vbs', '.vbe',
    '.wsf', '.wsh', '.hta', '.lnk', '.url', '.reg', '.sh', '.bash', '.zsh', '.fish',
    '.py', '.pyc', '.pyo', '.pyw', '.js', '.jse', '.mjs', '.cjs', '.ts', '.jsx', '.tsx',
    '.html', '.htm', '.xhtml', '.php', '.pl', '.rb', '.lua', '.groovy', '.kts', '.sc', '.desktop', '.appref-ms',
}


def is_allowed(rel: PurePosixPath) -> bool:
    path = rel.as_posix().lower()
    if path.startswith('plugins/runtime/'):
        return True
    if rel.suffix.lower() == '.jar':
        return re.fullmatch(r'(?:plugins/)?(?:lib/)?[^/]+\.jar', path) is not None
    if path.startswith(('native/', 'audio-native/')) and path.endswith('.dll'):
        return True
    if path.startswith('tools/') and rel.suffix.lower() in ('.py', '.js'):
        return True
    return rel.suffix.lower() not in EXECUTABLE_SUFFIXES


def valid_path(rel: PurePosixPath) -> bool:
    return all(part not in ('.', '..') and not part.endswith(('.', ' '))
               and not any(ord(c) < 32 or c in ':\\' for c in part)
               and not re.fullmatch(r'(?i)(con|prn|aux|nul|com[1-9]|lpt[1-9])(?:\..*)?', part)
               for part in rel.parts)


def inspect_header(path: Path, rel: PurePosixPath) -> None:
    lower = rel.as_posix().lower()
    helper = lower.startswith('plugins/runtime/')
    pe = rel.suffix.lower() in ('.exe', '.dll', '.pyd') and (helper or lower.startswith(('native/', 'audio-native/')))
    with path.open('rb') as stream:
        head = stream.read(64)
        if pe:
            if len(head) != 64 or head[:2] != b'MZ':
                raise ValueError('잘못된 Windows 실행 파일 헤더')
            offset = struct.unpack_from('<I', head, 60)[0]
            size = path.stat().st_size
            if offset < 64 or offset > size - 26:
                raise ValueError('잘못된 PE 헤더 위치')
            stream.seek(offset); header = stream.read(26)
            machine, sections = struct.unpack_from('<HH', header, 4)
            optional, flags, magic = struct.unpack_from('<HHH', header, 20)
            expected = 2 if rel.suffix.lower() == '.exe' else 0x2002
            if header[:4] != b'PE\0\0' or machine != 0x8664 or flags & 0x2002 != expected or magic != 0x20b:
                raise ValueError('Windows x64 EXE/DLL/PYD 형식만 지원합니다')
            if not 1 <= sections <= 96 or optional < 112 or offset + 24 + optional + 40 * sections > size:
                raise ValueError('손상된 PE 섹션 표')
            for i in range(sections):
                stream.seek(offset + 24 + optional + 40 * i + 16)
                length, start = struct.unpack('<II', stream.read(8))
                if length and (start > size or length > size - start):
                    raise ValueError('잘린 PE 섹션')
        elif not helper and (head.startswith((b'MZ', b'\x7fELF', b'\xca\xfe\xba\xbe'))):
            raise ValueError('실행 파일을 데이터 이름으로 배포할 수 없습니다')


def is_legal_notice(rel: PurePosixPath) -> bool:
    """Match notice tokens, including plurals and third-party/component prefixes.

    Delimiters keep unrelated words such as licensed-code.md and noticescreen.png
    from becoming legal notices merely because they contain a substring.
    """
    return re.search(r'(?:^|[._ -])(?:licen[cs]es?|notices?|copying|copyrights?)(?:$|[._ -])',
                     rel.name, re.IGNORECASE) is not None


def is_plugin_file(rel: PurePosixPath) -> bool:
    """plugins\\<이름>.jar|.json (두 조각) 또는 plugins\\lib\\<이름>.jar (세 조각)."""
    parts = rel.parts
    if len(parts) < 2 or parts[0].lower() != 'plugins':
        return False
    name = rel.name.lower()
    if len(parts) == 2:
        return name.endswith('.jar') or name.endswith('.json')
    return len(parts) == 3 and parts[1].lower() == 'lib' and name.endswith('.jar')


def is_tool_script(rel: PurePosixPath) -> bool:
    """tools\\<이름>.py — 딱 한 겹, 다른 자리는 안 됩니다."""
    parts = rel.parts
    return len(parts) == 2 and parts[0].lower() == 'tools' and rel.name.lower().endswith('.py')


def suffix_of(rel: PurePosixPath) -> str:
    name = rel.name.lower()
    dot = name.rfind('.')
    return name[dot:] if dot > 0 else ''


def kind_fix(kind) -> str:
    """모드 항목이 아닐 때 jar·py 에 붙일 처방 — 자리가 아니라 종류가 문제입니다."""
    if kind == KIND_IMAGE_SET:
        return FIX_KIND_IMAGE_SET
    if kind == KIND_VOICE_PACK:
        return FIX_KIND_VOICE_PACK
    return FIX_KIND_OTHER


def judge(rel: PurePosixPath, with_plugins: bool, link: bool, kind=None):
    if link:
        return False, DROP_LINK, '링크 말고 파일 자체를 넣으세요.'
    if not valid_path(rel):
        return False, DROP_PLACE, 'Windows에서 사용할 수 없는 경로입니다.'
    if is_refused(rel):
        return False, DROP_PROTECTED, FIX_PROTECTED
    if is_allowed(rel):
        return True, '', ''
    return False, DROP_EXTENSION, '지원 실행 코드는 plugins/, tools/, native/ 또는 plugins/runtime/ 규칙을 확인하세요.'


def scan(root: Path, with_plugins: bool, kind=None):
    """(가는 것, 빠지는 것, 폴더 목록). 각 항목은 (상대경로, 크기, 사유, 처방).

    `root`는 전체 창작마당 항목의 뿌리입니다. 혼합 패키지의 코드와 데이터를 함께 검사합니다.
    """
    kept, dropped, folders = [], [], []
    folded = set()
    if is_link(root):
        return [], [(PurePosixPath('.'), 0, DROP_LINK, '링크 말고 파일 자체를 넣으세요.')], []
    for dirpath, dirnames, filenames in os.walk(root):
        here = Path(dirpath)
        dirnames.sort()
        # os.walk(followlinks=False) still enters Windows directory junctions.
        # Prune every directory reparse point before os.walk can descend into it.
        for name in list(dirnames):
            full = here / name
            rel = PurePosixPath(full.relative_to(root).as_posix())
            if is_link(full):
                dirnames.remove(name)
                dropped.append((rel, 0, DROP_LINK, '링크 말고 파일 자체를 넣으세요.'))
            else:
                folders.append(rel)
        for name in sorted(filenames):
            full = here / name
            rel = PurePosixPath(full.relative_to(root).as_posix())
            link = is_link(full)
            size = 0 if link else full.stat().st_size
            ok, reason, fix = judge(rel, with_plugins, link, kind)
            if rel.as_posix().lower() in folded:
                ok, reason, fix = False, DROP_PLACE, '대소문자만 다른 중복 경로입니다.'
            folded.add(rel.as_posix().lower())
            if ok:
                try: inspect_header(full, rel)
                except (OSError, ValueError, struct.error) as error:
                    ok, reason, fix = False, DROP_EXTENSION, str(error)
            (kept if ok else dropped).append((rel, size, reason, fix))
    return kept, dropped, folders


def outside_scope(root: Path, base: Path):
    """미러되는 뿌리 밖에 있는 파일 — 규칙을 따지기도 전에 아예 훑지 않는 자리입니다."""
    if base == root:
        return []
    found = []
    for dirpath, dirnames, filenames in os.walk(root):
        here = Path(dirpath)
        if here == base or base in here.parents:
            dirnames[:] = []
            continue
        dirnames.sort()
        dirnames[:] = [name for name in dirnames if not is_link(here / name)]
        for name in sorted(filenames):
            found.append(PurePosixPath((here / name).relative_to(root).as_posix()))
    return found


def _is_reparse(path: Path) -> bool:
    """정션(junction)도 링크로 봅니다 — 앱은 isOther() 로 같이 거릅니다."""
    try:
        return bool(path.lstat().st_file_attributes & 0x400)  # FILE_ATTRIBUTE_REPARSE_POINT
    except (AttributeError, OSError):
        return False


def is_link(path: Path) -> bool:
    return path.is_symlink() or (os.name == 'nt' and _is_reparse(path))


def reject_overlapping_output(source: Path, target: Path) -> None:
    """Both paths are resolved first, including existing junction/symlink parents."""
    if source == target or source in target.parents or target in source.parents:
        raise SystemExit('원본과 출력 폴더가 겹칩니다 — 서로 포함하지 않는 별도 폴더를 주세요.')


# ── item.vdf ─────────────────────────────────────────────────────────────────

def read_appid() -> str:
    """build\\steam\\appid.txt 가 있으면 거기서, 없으면 정식 App ID 로 물러섭니다."""
    for candidate in (SDK / 'appid.txt', SDK.parent / 'steam' / 'appid.txt',
                      SDK.parent.parent / 'build' / 'steam' / 'appid.txt'):
        try:
            text = candidate.read_text(encoding='utf-8')
        except OSError:
            continue
        for line in text.splitlines():
            line = line.strip()
            if re.fullmatch(r'\d+', line):
                return line
    return DEFAULT_APPID


def vdf_path(text: Path) -> str:
    return str(text).replace('\\', '\\\\').replace('"', '\\"')


def update_contentfolder(text: str, content: Path) -> str:
    """Replace only the root contentfolder value, preserving user metadata and comments."""
    token = re.compile(r'\s+|//[^\r\n]*|"(?:\\.|[^"\\])*"|[{}]|[^\s{}"]+')
    tokens = []
    end = 0
    for match in token.finditer(text):
        if match.start() != end:
            raise SystemExit('기존 item.vdf의 따옴표가 닫히지 않았습니다 — 원본을 보존했습니다.')
        end = match.end()
        value = match.group()
        if value.isspace() or value.startswith('//'):
            continue
        tokens.append((value, match.start(), match.end()))
    if end != len(text):
        raise SystemExit('기존 item.vdf의 따옴표가 닫히지 않았습니다 — 원본을 보존했습니다.')

    def word(raw):
        return re.sub(r'\\(.)', r'\1', raw[1:-1]) if raw.startswith('"') else raw

    found = []

    def object_end(at, depth):
        while at < len(tokens) and tokens[at][0] != '}':
            key = tokens[at][0]
            if key in ('{', '}') or at + 1 >= len(tokens):
                raise ValueError('expected a key and value')
            value = tokens[at + 1]
            if value[0] == '{':
                at = object_end(at + 2, depth + 1) + 1
            elif value[0] == '}':
                raise ValueError('missing value')
            else:
                if depth == 1 and word(key).lower() == 'contentfolder':
                    found.append(value)
                at += 2
        if at >= len(tokens):
            raise ValueError('unclosed object')
        return at

    try:
        if len(tokens) < 3 or word(tokens[0][0]).lower() != 'workshopitem' or tokens[1][0] != '{':
            raise ValueError('expected workshopitem object')
        closing = object_end(2, 1)
        if closing != len(tokens) - 1 or len(found) > 1:
            raise ValueError('ambiguous workshopitem or contentfolder')
    except ValueError as error:
        raise SystemExit(f'기존 item.vdf를 읽지 못했습니다 — 원본을 보존했습니다: {error}')
    value = '"' + vdf_path(content) + '"'
    if found:
        _, start, end = found[0]
        return text[:start] + value + text[end:]
    newline = '\r\n' if '\r\n' in text else '\n'
    at = tokens[closing][1]
    return text[:at] + '\t"contentfolder"\t\t' + value + newline + text[at:]


def prepare_vdf(target: Path, content: Path, appid: str):
    out = target.parent / (target.name + '-item.vdf')
    if is_link(out):
        raise SystemExit(f'업로드 정의가 링크입니다 — 원본을 보존했습니다: {win(out)}')
    if out.exists():
        with out.open(encoding='utf-8-sig', newline='') as stream:
            return out, update_contentfolder(stream.read(), content)
    body = f'''// Little LUMI 창작마당 업로드 정의 — tools\\pack_workshop.py 가 만든 밑틀입니다.
//
// 채워 넣을 것은 둘입니다.
//   1) "title"       — 창작마당에 보일 항목 이름.
//   2) "previewfile" — 미리보기 그림의 절대 경로(.png, 1 MiB 이하). 없으면 그 줄을 지우세요.
//
// 처음 올릴 때는 "publishedfileid" 를 0 으로 둔 채 올리면 스팀이 새 항목 번호를 돌려줍니다.
// 그 번호를 "publishedfileid" 에 적어 두면 다음부터는 같은 항목을 갱신합니다(0 이면 매번 새 항목이 생깁니다).
//
// "visibility" 는 0=공개 · 1=친구만 · 2=비공개입니다. 다 확인하기 전에는 2 로 두세요.
// 올리기: steamcmd +login <계정> +workshop_build_item "{vdf_path(out)}" +quit
//
// "contentfolder" 는 이 도구가 규칙에 맞게 걸러 만든 폴더입니다 — 그 안의 파일이 구독자에게 그대로 갑니다.
"workshopitem"
{{
\t"appid"\t\t\t"{appid}"
\t"publishedfileid"\t"0"
\t"contentfolder"\t\t"{vdf_path(content)}"
\t"previewfile"\t\t""
\t"visibility"\t\t"2"
\t"title"\t\t\t""
\t"description"\t\t""
\t"changenote"\t\t"첫 버전"
}}
'''
    return out, body.replace('\n', '\r\n')


def write_vdf(out: Path, body: str) -> Path:
    # Replacing an existing definition must not truncate the user's metadata on interruption.
    temporary = None
    try:
        with tempfile.NamedTemporaryFile(mode='w', encoding='utf-8', newline='',
                                         dir=out.parent, prefix='.' + out.name + '.', delete=False) as stream:
            temporary = Path(stream.name)
            stream.write(body)
        temporary.replace(out)
    finally:
        if temporary is not None:
            temporary.unlink(missing_ok=True)
    return out


# ── 보고 ─────────────────────────────────────────────────────────────────────

def human(size: int) -> str:
    if size >= 1024 * 1024 * 1024:
        return f'{size / (1024 * 1024 * 1024):.1f} GiB'
    if size >= 1024 * 1024:
        return f'{size / (1024 * 1024):.1f} MiB'
    if size >= 1024:
        return f'{size / 1024:.1f} KiB'
    return f'{size} B'


def report_drops(dropped, prefix: str = '') -> None:
    order = [DROP_EXTENSION, DROP_PLACE, DROP_KIND, DROP_PROTECTED, DROP_LINK]
    groups = {}
    for rel, size, reason, fix in dropped:
        groups.setdefault(reason, []).append((prefix + str(rel), fix))
    for reason in order:
        rows = sorted(groups.get(reason, []))
        if not rows:
            continue
        print(f'  [{reason}] {DROP_HELP[reason]}')
        width = max(len(win(rel)) for rel, _ in rows)
        for rel, fix in rows:
            print(f'    {win(rel).ljust(width)}  → {fix}')
        print()


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('mod_dir', help='창작마당에 올릴 모드 폴더 (plugins\\, img\\, conf\\ 를 담은 뿌리)')
    ap.add_argument('--out', default=None,
                    help='만들 폴더 (기본: ' + win(SDK / 'dist' / 'workshop') + '\\<이름> — '
                         '현재 폴더가 아니라 SDK 뿌리 기준입니다)')
    ap.add_argument('--check', action='store_true', help='검사만 하고 아무것도 만들지 않습니다')
    ap.add_argument('--strict', action='store_true',
                    help='이전 호출과의 호환용: 현재는 누락 파일이 있으면 항상 실패합니다')
    args = ap.parse_args()

    root = Path(args.mod_dir).resolve()
    if not root.is_dir():
        raise SystemExit(f'폴더가 없습니다: {win(root)}')
    target = Path(args.out).resolve() if args.out else (SDK / 'dist' / 'workshop' / root.name).resolve()
    if not args.check:
        reject_overlapping_output(root, target)

    kind, why = detect_kind(root)
    with_plugins = True
    base, prefix, max_bytes = item_scope(root, kind)
    kept, dropped, folders = scan(base, with_plugins, kind) if base.is_dir() else ([], [], [])
    ignored = outside_scope(root, base)
    total = sum(size for _, size, _, _ in kept)

    print('Little LUMI 창작마당 컨텐츠 폴더 검사')
    print(f'  폴더: {win(root)}')
    print()
    print(f'항목 종류: {KIND_KO[kind]} ({kind})')
    print(f'  근거: {why}')
    print()

    refused = None
    if kind == KIND_UNKNOWN:
        refused = '모드·보이스팩·캐릭터 배치를 확인할 수 없습니다.'
    if dropped or ignored:
        refused = '지원하지 않는 파일이나 경로가 있습니다. 원본을 확인한 뒤 다시 실행하세요.'
    if len(kept) + len(dropped) > 100_000:
        refused = '패키지 파일 수 상한(100,000개)을 넘었습니다.'
    print('전체 패키지를 보존합니다. 혼합 패키지는 실행 코드에 동의한 뒤 코드와 데이터를 함께 적용합니다.')
    print('파일 내용과 실제 실행 동의는 앱에서 추가 검사합니다.')

    if kind == KIND_PLUGIN:
        print('코드 모드는 구독 후 기본 OFF입니다. 모드 체크박스에서 동의해야 코드와 데이터가 다음 실행부터 적용됩니다.')
        print('JAR·plugins\\lib 라이브러리 변경 시 재동의가 필요합니다. 매니페스트 Class-Path와 JAR 인덱스는 지원하지 않습니다.')
        print()

    if dropped:
        print(f'거부된 파일 {len(dropped)} 개 — 이 파일을 버린 패키지는 만들지 않습니다.')
        print()
        report_drops(dropped, prefix)
    else:
        print('빠지는 파일 없습니다.')
        print()

    if ignored:
        edge = win(prefix.rstrip('/')) + '\\'
        print(f'{edge} 밖의 파일 {len(ignored)} 개 — 이 종류는 {edge} 아래만 미러합니다.')
        for rel in ignored[:20]:
            print(f'    {win(rel)}')
        if len(ignored) > 20:
            print(f'    … 그리고 {len(ignored) - 20} 개 더')
        print()

    print(f'가는 파일: {len(kept)} 개 · {human(total)} / 상한 {human(max_bytes)}')
    if total > max_bytes:
        refused = refused or '가는 파일의 합이 크기 상한을 넘었습니다.'
        print('  크기 상한을 넘었습니다 — 앱이 통째로 거절합니다. 파일을 줄이거나 항목을 나누세요.')
    print()

    missing_notices = [rel for rel, _, _, _ in dropped if is_legal_notice(rel)]
    missing_notices.extend(rel for rel in ignored if is_legal_notice(rel))
    if missing_notices:
        refused = refused or '라이선스·저작권 고지가 구독자에게 전달되지 않습니다.'
        print('고지 파일 누락은 --strict 없이도 실패합니다:')
        for rel in missing_notices:
            print(f'  {win(rel)}')
        print('고지 본문을 보존하고 파일명을 .txt로 바꾸거나, 플러그인 src/META-INF/에 넣어 JAR를 다시 빌드하세요.')
        print('Missing legal notices: preserve the text in .txt files or inside the JAR (src/META-INF/), then retry.')
        print()

    if args.check:
        print('(--check 이라 아무것도 만들지 않았습니다.)')
        print()
    elif refused:
        print('폴더를 만들지 않았습니다 — 이 항목은 이대로 올릴 수 없습니다.')
        print()
    else:
        vdf, definition = prepare_vdf(target, target, read_appid())
        if target.exists():
            shutil.rmtree(target)
        target.mkdir(parents=True, exist_ok=True)
        for rel in folders:
            (target / rel).mkdir(parents=True, exist_ok=True)
        for rel, _, _, _ in kept:
            destination = target / rel
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(base / rel, destination)
        write_vdf(vdf, definition)
        print(f'만들었습니다: {win(target)}  (여기 있는 것만 구독자에게 갑니다)')
        print(f'업로드 정의: {win(vdf)}  (title 과 previewfile 을 채우세요)')
        print()

    missing_count = len(dropped) + len(ignored)
    if refused:
        print(f'올릴 수 없습니다 — {refused}')
        return 1
    if missing_count and args.strict:
        print(f'올릴 수는 있지만 안 가는 파일이 {missing_count} 개 있습니다 — --strict 라 실패로 칩니다.')
        return 1
    if missing_count:
        print(f'OK — 올려도 됩니다. 다만 파일 {missing_count} 개는 구독자에게 가지 않습니다 — '
              '의도한 것인지 확인하세요.')
        return 0
    print('OK — 포장 경로 검사를 통과했습니다. 앱의 내용 검사와 실행 동의는 별도입니다.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
