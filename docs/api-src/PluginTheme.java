package com.group_finity.mascot.lumi.plugin;

import com.group_finity.mascot.lumi.LumiTheme;
import com.group_finity.mascot.lumi.SpeechBubbleWindow;
import com.group_finity.mascot.lumi.settings.ProtoUi;
import com.group_finity.mascot.lumi.settings.SettingsUi;

import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.LayoutManager;
import java.util.function.Supplier;

/**
 * 플러그인 창을 <b>앱과 같은 옷</b>으로 입히는 최소한 — {@link PluginContext#theme()}.
 * <p>
 * 앱은 FlatLaf 위에 제 시안(다크·라이트)을 얹어 쓰고, 테마가 갈리면 열린 창을 다시 입힌다.
 * 플러그인이 맨 Swing 으로 창을 만들면 그 창만 다른 세계에 서고, 테마를 바꾸는 순간 옛 색으로
 * 남는다. 여기 있는 것은 LUMI Chat 의 설정 창과 입력 창이 실제로 쓴 것 전부다 — 색 여덟,
 * 글꼴 하나, 손질 여덟, 조각 다섯. 그 이상은 <b>약속 밖</b>이다(앱의 {@code settings} 패키지는
 * 판마다 바뀐다).
 * <p>
 * 두 가지 규칙만 알면 된다. ① 색은 <b>다크 시안 값</b>으로 말한다 — {@link #tone(int)} 이 지금
 * 테마의 색으로 옮긴다(라이트면 표대로 바꿔치기). ② 테마가 갈릴 때 다시 해야 하는 일은
 * {@link #colour}/{@link #dress} 로 <b>걸어 둔다</b>, 한 번 칠하고 말지 않는다.
 * 모든 메서드는 EDT 에서 부른다.
 */
public final class PluginTheme {
    static final PluginTheme INSTANCE = new PluginTheme();

    /** 단추의 종류 — 앱의 설정 창이 쓰는 네 가지. */
    public enum Button { OUTLINE, PRIMARY, DANGER, LINK }

    PluginTheme() {
    }

    // ── 색 ────────────────────────────────────────────────────────────────

    /** 지금 룩앤필이 다크인가. 룩앤필을 못 읽으면 다크. */
    public boolean dark() {
        return ProtoUi.dark();
    }

    /** 다크 시안 값 하나({@code 0xRRGGBB}) → 지금 테마의 색. 라이트면 앱의 대응표로 옮긴다. */
    public Color tone(int darkRgb) {
        return ProtoUi.tone(darkRgb);
    }

    /** 설명 글의 잉크(본문보다 흐리다). */
    public Color inkSoft() {
        return ProtoUi.INK62;
    }

    /** 힌트·단축키 표시의 잉크(가장 흐리다). */
    public Color inkFaint() {
        return ProtoUi.INK55;
    }

    /** 카드 사이의 가는 선. */
    public Color line() {
        return ProtoUi.LINE;
    }

    /** 카드 바탕. */
    public Color cardBackground() {
        return LumiTheme.cardBackground();
    }

    /** 경고·실패 글의 색. 본문색으로 덮이지 않게 {@link #colour} 로 걸어 둘 것. */
    public Color danger() {
        return LumiTheme.danger();
    }

    /** 성공 글의 색. */
    public Color success() {
        return LumiTheme.success();
    }

    // ── 글꼴 ──────────────────────────────────────────────────────────────

    /**
     * 이 글을 그릴 수 있는 앱 글꼴 — 한국어는 Jua, 일본어·중국어는 그 글자가 있는 얼굴로
     * 물러난다(말풍선과 같은 셈법). 물러난 얼굴은 85% 로 줄여 돌아오므로 크기를 지켜야 하면
     * {@code deriveFont(size)} 를 한 번 더.
     */
    public Font font(String text, float size) {
        return SpeechBubbleWindow.fontFor(text == null || text.isEmpty() ? "가" : text, size);
    }

    // ── 걸어 두는 손질 ─────────────────────────────────────────────────────

    /** 글자색을 <b>판정</b>으로 걸어 둔다 — 테마가 갈리면 다시 묻는다. */
    public void colour(JComponent component, Supplier<Color> colour) {
        LumiTheme.speakColour(component, colour);
    }

    /** 테마가 갈릴 때 다시 해야 하는 손질(배경·직접 깐 UI)을 걸어 둔다. 지금 한 번 돈다. */
    public void dress(JComponent component, Runnable dress) {
        LumiTheme.speakDress(component, dress);
    }

    /**
     * FlatLaf 스타일 문자열을 <b>다크 시안 값</b>으로 걸어 둔다(예:
     * {@code "background:#131F33; foreground:#E9F2FA; arc:8"}). 라이트로 갈리면 색만 옮겨 다시 입는다.
     */
    public void style(JComponent component, String darkStyle) {
        ProtoUi.style(component, darkStyle);
    }

    /** 앱의 단추 옷. */
    public void button(AbstractButton button, Button kind) {
        ProtoUi.buttonize(button, switch (kind == null ? Button.OUTLINE : kind) {
            case PRIMARY -> ProtoUi.ButtonStyle.PRIMARY;
            case DANGER -> ProtoUi.ButtonStyle.DANGER;
            case LINK -> ProtoUi.ButtonStyle.LINK;
            default -> ProtoUi.ButtonStyle.OUTLINE;
        });
    }

    /** 체크박스를 앱의 토글 스위치 모양으로. */
    public void toggle(JCheckBox box) {
        ProtoUi.toggleize(box);
    }

    /** 슬라이더를 앱의 가는 트랙·둥근 손잡이로. {@code thumbOnly} 면 트랙은 그대로 두고 손잡이만. */
    public void slider(JSlider slider, boolean thumbOnly) {
        ProtoUi.sliderize(slider, thumbOnly);
    }

    /** 설명 한 줄의 옷 — 본문보다 한 단계 작고 흐리다. 라벨을 그대로 돌려준다. */
    public <T extends JLabel> T describe(T label) {
        return ProtoUi.describe(label);
    }

    // ── 조각 ──────────────────────────────────────────────────────────────

    /** 카드 안 소제목. */
    public JLabel sectionTitle(String text) {
        return ProtoUi.sectionTitle(text);
    }

    /** 카드 안 가는 구분선. */
    public JComponent hairline() {
        return ProtoUi.hairline();
    }

    /** 투명한 줄 판 — 설정 한 줄(라벨 + 컨트롤)을 얹는 자리. */
    public JPanel row(LayoutManager layout) {
        return SettingsUi.row(layout);
    }

    /** 카드 — 둥근 모서리, 카드 바탕, 안쪽 여백. 내용은 위에서 아래로 쌓인다. */
    public JPanel card(Component... contents) {
        return SettingsUi.card(contents);
    }

    /** 가로로 나란히, 사이 {@code gap}px. */
    public JPanel gapRow(int gap, Component... items) {
        return ProtoUi.gapRow(gap, items);
    }

    /** 카드 사이 간격(px). */
    public int cardGap() {
        return SettingsUi.CARD_GAP;
    }

    /** 설명 글을 줄바꿈되는 HTML 로 — {@code JLabel.setText} 에 넣는다. */
    public String hintHtml(String text) {
        return SettingsUi.hintHtml(text);
    }

    /** 같되 {@code wrapPx} 에서 줄을 바꾼다. */
    public String hintHtml(String text, int wrapPx) {
        return SettingsUi.hintHtml(text, wrapPx);
    }
}
