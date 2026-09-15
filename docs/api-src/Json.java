package com.group_finity.mascot.lumi.plugin;

import com.group_finity.mascot.lumi.ai.MiniJson;

/**
 * 의존성 없는 작은 JSON — LLM 답장, 설정 파일, 창작마당 항목 같은 것을 읽고 쓴다.
 * <p>
 * 파싱 결과는 {@code Map<String,Object>} / {@code List<Object>} / {@code String} / {@code Double} /
 * {@code Boolean} / {@code null} 뿐이다. 숫자는 전부 {@code Double} 로 온다. 믿을 수 없는 입력을
 * 전제로 한다: 깊이 200, 길이 8 MB 를 넘으면 {@link IllegalArgumentException} 으로 <b>깨끗이</b>
 * 실패한다({@link StackOverflowError} 로 스레드가 죽지 않는다).
 * <p>
 * 왜 여기 있나 — LLM 을 부르는 플러그인은 예외 없이 JSON 이 필요하고, 그때마다 라이브러리를
 * {@code plugins\lib\} 에 넣게 하면 같은 라이브러리의 다른 판이 한 클래스패스에 여럿 실린다.
 * 앱이 이미 쓰는 것을 약속된 이름으로 내놓는 쪽이 싸다.
 */
public final class Json {
    private Json() {
    }

    /** 문자열 → 값. 잘못된 문서는 {@link IllegalArgumentException}. */
    public static Object parse(String text) {
        return MiniJson.parse(text);
    }

    /**
     * 경로로 꺼낸다 — 각 단계는 Map 의 열쇠({@code String})거나 List 의 번호({@code Integer}).
     * 중간에 없으면 {@code null}. 예: {@code get(root, "choices", 0, "message", "content")}.
     */
    public static Object get(Object root, Object... path) {
        return MiniJson.get(root, path);
    }

    /** {@link #get} 의 문자열판 — 없거나 문자열이 아니면 {@code null}. */
    public static String getString(Object root, Object... path) {
        return MiniJson.getString(root, path);
    }

    /** 값 → 문자열. Map/List/String/Number/Boolean/null 을 받는다. */
    public static String write(Object value) {
        return MiniJson.write(value);
    }
}
