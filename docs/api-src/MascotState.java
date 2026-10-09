package com.group_finity.mascot.lumi.plugin;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * 한 캐릭터의 지금 상태 — {@link PluginContext#mascots()} 계열이 돌려주는 값.
 * <p>
 * <b>좌표는 바탕화면 좌표</b>다 — Swing 창을 놓는 {@code setLocation} 이 받는 그 공간이고,
 * {@link java.awt.GraphicsEnvironment} 의 모니터 사각형과 그대로 견줄 수 있다. 방송 모드의
 * 전용 창 안에서 엔진이 쓰는 창 지역좌표는 앱이 이미 올려서 내보내므로 플러그인은 신경
 * 쓰지 않아도 된다. 배율(DPI)이 있는 모니터에서도 자바가 보는 논리 좌표 그대로다.
 * <ul>
 *   <li>{@code id} — 그 개체의 번호. 같은 이미지셋이 여럿일 때 하나를 집는 손잡이이고,
 *       {@code click}·{@code mascot}·{@code behavior} 이벤트의 {@code mascotId} 와 같은 값이다.
 *       그 개체가 사는 동안만 유효하다(사라지면 재사용될 수 있다).</li>
 *   <li>{@code imageSet} — {@code img\<폴더>} 이름. 캐릭터의 신원.</li>
 *   <li>{@code bounds} — 그림 창의 사각형(회전·배율 포함). 논리 물리 충돌 영역과는 별개.</li>
 *   <li>{@code anchor} — 발끝(닻). 서 있는 바닥의 y 가 여기다.</li>
 *   <li>{@code lookRight} — 오른쪽을 보고 있는가.</li>
 *   <li>{@code behavior} — 지금 행동 이름({@code behaviors.xml} 의 이름). 모르면 {@code ""}.</li>
 * </ul>
 * 값은 <b>그 순간의 사본</b>이다 — 캐릭터는 틱마다 움직이므로 오래 쥐고 있지 말고 필요할 때
 * 다시 물어라(50 ms 마다 물어도 비용은 사본 몇 개다).
 */
public record MascotState(int id, String imageSet, Rectangle bounds, Point anchor, boolean lookRight,
                          String behavior) {

    public MascotState {
        imageSet = imageSet == null ? "" : imageSet;
        behavior = behavior == null ? "" : behavior;
        bounds = bounds == null ? new Rectangle() : new Rectangle(bounds);
        anchor = anchor == null ? new Point() : new Point(anchor);
    }

    /** 그림 창의 가로 가운데 — 머리 위 효과·말풍선을 놓을 x. */
    public int centerX() {
        return bounds.x + bounds.width / 2;
    }

    /** 머리 위 한 점 (가운데, 창의 윗변) — 하트·쪽지가 터지는 자리. */
    public Point head() {
        return new Point(centerX(), bounds.y);
    }
}
