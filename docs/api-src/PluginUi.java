package com.group_finity.mascot.lumi.plugin;

import java.awt.EventQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.swing.AbstractButton;

/**
 * Where plugins hang their UI: tray-menu rows (drawn under the app's own rows),
 * character-menu rows and buttons on the settings window's Information tab. Registered through
 * {@link PluginContext#addTrayItem} / {@link PluginContext#addCharacterMenuItem} /
 * {@link PluginContext#addSettingsButton} (and the handle-returning
 * {@link PluginContext#addTrayItem} / {@link PluginContext#addCharacterMenuItem} /
 * {@link PluginContext#addTrayToggle} / {@link PluginContext#addCharacterMenuToggle}); read by
 * {@code TrayMenuPanel} and {@code Mascot} every time a menu is built, and by
 * {@code SettingsWindow} every time that window opens. A row registered late therefore shows on
 * the next open, and a row whose handle changed its label, its check or was removed is drawn that
 * way the next time too. A label change also reaches controls already on screen: each consumer
 * {@code bind}s the button it built, and {@code setLabel} re-texts those on the EDT.
 * <p>
 * 줄은 {@link MenuHandle} 로 돌아온다 — 라벨 갱신·체크 상태·제거가 되므로
 * '집중 모드: 켜짐' 같은 토글 줄을 만들 수 있다.
 * {@code checked} 가 {@code null} 이면 보통 줄, 아니면 <b>상태 칩</b>이 붙는 줄이다(앱의
 * 「잠깐 숨기기 · 켜짐/꺼짐」과 같은 문법 — 이름 고정 + 칩). 토글 줄은 누르면 스스로 뒤집고
 * {@code onChange} 를 부른다. 옛 {@code addTrayItem}/{@code addCharacterMenuItem} 은 그대로
 * {@code MenuHandle} 이다 — 돌려주는 형을 바꾸면 이미 구워진 플러그인 jar 가 {@code NoSuchMethodError}
 * 로 깨진다(JVM 은 반환형까지 서명이다). 이번 보완은 이 서명을 유지한다.
 * <p>
 * The settings window's 「모드」 tab holds mod slots and the
 * <i>declarative</i> settings card a plugin declares in {@code plugin.json}
 * ({@link PluginSettingsSchema}) — never a free-form Swing panel of the plugin's own.
 * A plugin may also register one {@link FeatureToggle}: the app draws it at the bottom of
 * the Lumi tab's 「기능 켜고 끄기」 card and commits it with that window's Apply button.
 */
public final class PluginUi {

    /**
     * 플러그인이 건 메뉴 줄의 손잡이 — 이름·활성·체크를 바꾸고, 뗄 수 있다. 바뀐 것은 다음에
     * 메뉴가 열릴 때 보인다(메뉴는 열 때마다 새로 짓는다). 이름({@link #setLabel})만은 이미 떠 있는
     * 설정 창 단추·트레이 줄·캐릭터 메뉴 줄에도 곧 EDT 에서 반영된다. 어느 스레드에서 불러도 된다.
     */
    public interface MenuHandle {
        void setLabel(String label);

        void setEnabled(boolean enabled);

        /** {@code null} = 보통 줄, {@code true}/{@code false} = 상태 칩(켜짐/꺼짐)이 붙는 줄. */
        void setChecked(Boolean checked);

        String label();

        boolean enabled();

        Boolean checked();

        /** 메뉴에서 뗀다. 뗀 뒤의 set… 은 무해하다. */
        void remove();

        boolean removed();
    }

    /** 두 줄 종류가 공유하는 상태. */
    abstract static class Item implements MenuHandle {
        final String pluginId;
        private volatile String label;
        private volatile boolean enabled = true;
        private volatile Boolean checked;
        private volatile boolean removed;
        /** 토글 줄이면 누를 때 새 상태를 받는 곳; 보통 줄은 {@code null}. */
        final Consumer<Boolean> onChange;
        /** {@link #bind} 로 건, 이 줄로 지은 단추들. 자물쇠이기도 하다. */
        private final List<WeakReference<AbstractButton>> views = new ArrayList<>();
        /** EDT 에 갱신이 이미 하나 걸려 있는가 — {@code views} 로 잠근다. */
        private boolean refreshQueued;

        Item(String pluginId, String label, Boolean checked, Consumer<Boolean> onChange) {
            this.pluginId = pluginId;
            this.label = label == null ? "" : label;
            this.checked = checked;
            this.onChange = onChange;
        }

        public String pluginId() {
            return pluginId;
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public boolean enabled() {
            return enabled;
        }

        @Override
        public Boolean checked() {
            return checked;
        }

        @Override
        public boolean removed() {
            return removed;
        }

        @Override
        public void setLabel(String label) {
            this.label = label == null ? "" : label;
            scheduleViewRefresh();
        }

        /**
         * 앱이 이 줄로 지은 단추·메뉴 줄을 건다 — 뒤의 {@link #setLabel} 이 이미 떠 있는 글자까지
         * EDT 에서 바꾼다(KK-126: 언어 변경의 {@code settings} 이벤트는 플러그인 스레드로 늦게 오고,
         * 설정 창은 그 전에 옛 이름으로 다시 지어진다). 약한 참조라 닫힌 창을 붙들지 않는다.
         * 거는 순간 지금 이름을 다시 넣으므로, 짓는 사이에 바뀐 이름도 놓치지 않는다. EDT 에서 부른다.
         */
        public void bind(AbstractButton view) {
            if (view == null) {
                return;
            }
            synchronized (views) {
                if (removed) {
                    return;
                }
                boolean known = false;
                for (Iterator<WeakReference<AbstractButton>> it = views.iterator(); it.hasNext(); ) {
                    AbstractButton bound = it.next().get();
                    if (bound == null) {
                        it.remove();
                    } else if (bound == view) {
                        known = true;
                    }
                }
                if (!known) {
                    views.add(new WeakReference<>(view));
                }
            }
            // 등록 뒤에 읽는다 — 그 사이의 setLabel 은 여기서 보이거나, 등록을 보고 새로 예약한다.
            showLabel(view, label);
        }

        /** 걸린 단추가 있을 때만 EDT 에 한 번 예약한다 — 화면 없는 setLabel 은 AWT 를 깨우지 않는다. */
        private void scheduleViewRefresh() {
            synchronized (views) {
                if (views.isEmpty() || refreshQueued) {
                    return;
                }
                refreshQueued = true;
            }
            EventQueue.invokeLater(this::refreshViews);
        }

        /** EDT — 예약할 때의 값이 아니라 <b>지금</b> 이름을 넣는다(빠른 연속 변경도 마지막 값이 이긴다). */
        private void refreshViews() {
            List<AbstractButton> live = new ArrayList<>();
            synchronized (views) {
                refreshQueued = false;
                for (Iterator<WeakReference<AbstractButton>> it = views.iterator(); it.hasNext(); ) {
                    AbstractButton bound = it.next().get();
                    if (bound == null) {
                        it.remove();
                    } else {
                        live.add(bound);
                    }
                }
                if (removed) {
                    return;
                }
            }
            String now = label;
            for (AbstractButton view : live) {
                showLabel(view, now);
            }
        }

        private static void showLabel(AbstractButton view, String text) {
            if (!text.equals(view.getText())) {
                view.setText(text);
            }
        }

        @Override
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public void setChecked(Boolean checked) {
            this.checked = checked;
        }

        @Override
        public void remove() {
            synchronized (views) {
                removed = true;
                views.clear();
            }
            PluginUi.trayItems.remove(this);
            PluginUi.characterMenuItems.remove(this);
            PluginUi.settingsButtons.remove(this);
        }

        /** 토글 줄인가 — 누르면 체크가 뒤집히고 {@code onChange} 가 불린다. */
        public boolean isToggle() {
            return onChange != null;
        }

        /** 토글 줄을 눌렀다 — 뒤집고 {@code onChange}. 새 상태를 돌려준다. */
        Boolean flip() {
            if (onChange == null) {
                return checked;
            }
            Boolean now = !Boolean.TRUE.equals(checked);
            checked = now;
            onChange.accept(now);
            return now;
        }
    }

    /** A tray-menu row: the action runs on the EDT after the menu closes. */
    public static final class TrayItem extends Item {
        private final Runnable action;

        TrayItem(String pluginId, String label, Runnable action) {
            super(pluginId, label, null, null);
            this.action = action == null ? () -> { } : action;
        }

        TrayItem(String pluginId, String label, boolean initial, Consumer<Boolean> onChange) {
            super(pluginId, label, initial, onChange == null ? b -> { } : onChange);
            this.action = null;
        }

        /** 누름 — 보통 줄은 action, 토글 줄은 뒤집고 onChange. 메뉴는 이것 하나만 부른다. */
        public Runnable action() {
            return () -> {
                if (isToggle()) {
                    flip();
                } else {
                    action.run();
                }
            };
        }
    }

    /** A row in a character's right-click menu; shown when {@code forImageSet} says so. */
    public static final class CharacterMenuItem extends Item {
        private final Predicate<String> forImageSet;
        private final BiConsumer<String, Integer> action;

        CharacterMenuItem(String pluginId, String label, Predicate<String> forImageSet,
                          BiConsumer<String, Integer> action) {
            super(pluginId, label, null, null);
            this.forImageSet = forImageSet == null ? s -> true : forImageSet;
            this.action = action == null ? (s, i) -> { } : action;
        }

        CharacterMenuItem(String pluginId, String label, Predicate<String> forImageSet,
                          boolean initial, Consumer<Boolean> onChange) {
            super(pluginId, label, initial, onChange == null ? b -> { } : onChange);
            this.forImageSet = forImageSet == null ? s -> true : forImageSet;
            this.action = null;
        }

        public Predicate<String> forImageSet() {
            return forImageSet;
        }

        /** 누름 — 보통 줄은 action(imageSet, mascotId), 토글 줄은 뒤집고 onChange. */
        public BiConsumer<String, Integer> action() {
            return (imageSet, mascotId) -> {
                if (isToggle()) {
                    flip();
                } else {
                    action.accept(imageSet, mascotId);
                }
            };
        }
    }

    /** A button on the settings window's Information tab; the action runs on the EDT. */
    public static final class SettingsButton extends Item {
        private final Runnable action;

        SettingsButton(String pluginId, String label, Runnable action) {
            super(pluginId, label, null, null);
            this.action = action == null ? () -> { } : action;
        }

        public Runnable action() {
            return action;
        }
    }

    /**
     * A plugin-owned switch in the Lumi tab's 「기능 켜고 끄기」 card. The app asks
     * {@code state} whenever a settings window opens and calls {@code onChange} when the user changes the switch.
     * Returning false rejects the proposed value, so the checkbox goes back to {@code state}.
     * A dependency such as {@code bongo} only disables the row; it never clears its check.
     */
    public static final class FeatureToggle {
        private final String pluginId;
        private final String label;
        private final String description;
        private final BooleanSupplier state;
        private final Predicate<Boolean> onChange;
        private final String dependsOnFeature;
        private volatile boolean removed;

        FeatureToggle(String pluginId, String label, String description, BooleanSupplier state,
                      Predicate<Boolean> onChange, String dependsOnFeature) {
            this.pluginId = pluginId;
            this.label = label == null ? "" : label;
            this.description = description == null ? "" : description;
            this.state = state == null ? () -> false : state;
            this.onChange = onChange == null ? value -> false : onChange;
            this.dependsOnFeature = dependsOnFeature == null ? "" : dependsOnFeature.strip();
        }

        public String pluginId() {
            return pluginId;
        }

        public String label() {
            return label;
        }

        public String description() {
            return description;
        }

        public boolean state() {
            try {
                return state.getAsBoolean();
            } catch (Throwable ignored) {
                return false;
            }
        }

        public boolean change(boolean value) {
            try {
                return onChange.test(value);
            } catch (Throwable ignored) {
                return false;
            }
        }

        public String dependsOnFeature() {
            return dependsOnFeature;
        }

        public void remove() {
            removed = true;
            featureToggles.remove(this);
        }

        public boolean removed() {
            return removed;
        }
    }

    private static final List<CharacterMenuItem> characterMenuItems = new CopyOnWriteArrayList<>();
    private static volatile String clickGreetingClaimedBy;
    private static final List<SettingsButton> settingsButtons = new CopyOnWriteArrayList<>();
    private static final List<TrayItem> trayItems = new CopyOnWriteArrayList<>();
    private static final List<FeatureToggle> featureToggles = new CopyOnWriteArrayList<>();

    private PluginUi() {
    }

    static TrayItem addTrayItem(TrayItem item) {
        trayItems.add(item);
        return item;
    }

    static CharacterMenuItem addCharacterMenuItem(CharacterMenuItem item) {
        characterMenuItems.add(item);
        return item;
    }

    static SettingsButton addSettingsButton(SettingsButton item) {
        settingsButtons.add(item);
        return item;
    }

    static FeatureToggle addFeatureToggle(FeatureToggle item) {
        // 카드가 플러그인마다 한 줄이라는 계약. 같은 플러그인이 다시 등록하면 최신 선언이
        // 그 한 자리를 갈아 끼운다(중복 줄을 늘리지 않는다).
        for (FeatureToggle old : featureToggles) {
            if (old.pluginId().equals(item.pluginId())) {
                old.removed = true;
                featureToggles.remove(old);
            }
        }
        featureToggles.add(item);
        return item;
    }

    /** 살아 있는(떼지 않은) 캐릭터 메뉴 줄, 건 차례대로. */
    public static List<CharacterMenuItem> characterMenuItems() {
        return List.copyOf(characterMenuItems);
    }

    /** A plugin that greets on click itself (the AI chat) turns the app's own click greeting off. */
    static void claimClickGreeting(String pluginId) {
        synchronized (PluginUi.class) {
            clickGreetingClaimedBy = pluginId;
        }
    }

    /**
     * 점유를 푼다 — <b>그 플러그인이 쥔 경우에만</b>: 두 플러그인이 차례로 점유했으면 나중 것이 주인이고,
     * 먼저 것이 거둬져도 손대지 않는다 ({@code PluginContext.releaseAll}).
     */
    static void releaseClickGreeting(String pluginId) {
        synchronized (PluginUi.class) {
            if (pluginId != null && pluginId.equals(clickGreetingClaimedBy)) {
                clickGreetingClaimedBy = null;
            }
        }
    }

    /** 검사용 — 지금 클릭 인사를 쥔 플러그인 id, 없으면 {@code null}. */
    static String clickGreetingHolder() {
        return clickGreetingClaimedBy;
    }

    public static boolean clickGreetingClaimed() {
        return clickGreetingClaimedBy != null;
    }

    /** 살아 있는(떼지 않은) 트레이 줄, 건 차례대로. */
    public static List<TrayItem> trayItems() {
        return List.copyOf(trayItems);
    }

    /** 살아 있는(떼지 않은) 정보 탭 단추, 건 차례대로. */
    public static List<SettingsButton> settingsButtons() {
        return List.copyOf(settingsButtons);
    }

    /** 살아 있는 「기능 켜고 끄기」 플러그인 줄, 건 차례대로. */
    public static List<FeatureToggle> featureToggles() {
        return List.copyOf(featureToggles);
    }

    /** 검사용 — 전부 뗀다. */
    static void clearForTest() {
        trayItems.clear();
        characterMenuItems.clear();
        settingsButtons.clear();
        featureToggles.clear();
        clickGreetingClaimedBy = null;
    }
}
