package com.group_finity.mascot.lumi.plugin;

import com.group_finity.mascot.lumi.Features;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Read-only Workshop creator data and subscription identities, without depending on Steam internals. */
public final class WorkshopContent {
    private static final String HOST = "com.group_finity.mascot.lumi.steam.WorkshopSync";

    private WorkshopContent() { }

    /**
     * Creator files for one character. Paths are read-only and may be absent; handle read failures.
     * Bind explicit user selection to both workshopId and subscriptionId. The subscription ID
     * survives updates and switches, but changes after unsubscribe and resubscribe.
     * modStyleName preserves the mod-style persona label (including the Workshop ID).
     */
    public record PromptSource(Path promptsFile, Path personaFile, String workshopId,
                               String subscriptionId, boolean modStyleName) { }

    /**
     * Available creator sources in mod order (later entries win collisions). After startup only
     * the host's prepared inventory is visible. Disabled, unsubscribed or unapproved providers
     * are excluded. Empty for an invalid image set or when the host lacks Workshop support.
     * Returning a source never selects a creator prompt or grants code execution consent.
     */
    public static List<PromptSource> promptSources(String imageSet) {
        Object result = Features.call(HOST, "promptSources", imageSet);
        if (!(result instanceof List<?> rows)) return List.of();
        List<PromptSource> sources = new ArrayList<>();
        for (Object value : rows) {
            if (value instanceof String[] row && row.length == 5)
                sources.add(new PromptSource(Path.of(row[0]), Path.of(row[1]), row[2], row[3],
                        Boolean.parseBoolean(row[4])));
        }
        return List.copyOf(sources);
    }

    /**
     * Whether the host manages this ID as a direct source, including an unsubscribed tombstone.
     * Use this to suppress legacy mirror fallback; true does not mean enabled or approved.
     * False when the host lacks Workshop support.
     */
    public static boolean hasDirectSource(String workshopId) {
        return Boolean.TRUE.equals(Features.call(HOST, "hasDirectSource", workshopId));
    }
}
