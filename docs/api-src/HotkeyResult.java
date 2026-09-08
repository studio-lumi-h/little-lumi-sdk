package com.group_finity.mascot.lumi.plugin;

/**
 * {@link PluginContext#bindHotkey} 의 답 — <b>왜</b> 안 됐는지까지 말한다. 설정 창은 이 값을
 * 사용자의 말로 옮긴다(「다른 프로그램이 이미 그 조합을 쓰고 있어요」).
 */
public enum HotkeyResult {
    /** 등록됐다. */
    OK,
    /** 조합이 비어 있다 — 지정 없음. 이전 등록이 있었으면 거뒀다. */
    NONE,
    /** 글자꼴을 못 읽었거나 수식 키(Win·Ctrl·Alt·Shift)가 하나도 없다. */
    BAD_COMBINATION,
    /** 윈도우가 거절했다 — 다른 프로그램이 이미 그 조합을 쥐고 있다. */
    REFUSED,
    /** 윈도우가 아니거나 {@code user32} 를 못 불렀다 — 이 자리에서는 전역 단축키가 없다. */
    UNSUPPORTED;

    /** 등록이 살아 있는가. */
    public boolean bound() {
        return this == OK;
    }
}
