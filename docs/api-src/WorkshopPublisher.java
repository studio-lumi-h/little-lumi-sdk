package com.group_finity.mascot.lumi.plugin;

import java.nio.file.Path;

/**
 * Explicit, user-confirmed publishing to the current Steam account. No upload is automatic.
 * Call account() off the EDT. publish() returns immediately; listener callbacks run off EDT.
 * Initial visibility is explicit; updates preserve the item's existing visibility.
 * A host journal remembers created items by plugin + clientProjectId + account before submission.
 * Keep clientProjectId stable across saves and renames; do not reuse it for another project.
 */
public interface WorkshopPublisher extends AutoCloseable {
    /** Signed-in identity, or an unavailable account with empty strings. */
    record Account(boolean available, String steamId, String displayName) { }
    enum Visibility { PUBLIC, PRIVATE }
    /**
     * An exported package, separate PNG/JPEG preview, metadata and confirmed account.
     * existingItemId may be empty for first publication. expectedSteamId is mandatory.
     * The host copies and verifies the package before Steam receives any file.
     * Non-null expectedFiles opts into exact content: every relative slash-separated file path
     * maps to its lowercase SHA-256, including hidden/marker files; no extra or missing file is allowed.
     * Strict requests also require expectedPreviewSha256 for the separate raw PNG/JPEG preview.
     * The caller fixes both hashes before confirmation; the host never accepts a changed baseline.
     */
    record Request(Path contentDirectory, Path previewFile, String title, String description,
                   String existingItemId, String expectedSteamId, String clientProjectId,
                   Visibility visibility, java.util.Map<String, String> expectedFiles,
                   String expectedPreviewSha256) {
        /** Existing clients retain general mod-package inspection. */
        public Request(Path contentDirectory, Path previewFile, String title, String description,
                       String existingItemId, String expectedSteamId, String clientProjectId, Visibility visibility) {
            this(contentDirectory, previewFile, title, description, existingItemId, expectedSteamId, clientProjectId,
                    visibility, null, null);
        }
        public Request {
            expectedFiles = expectedFiles == null ? null : java.util.Map.copyOf(expectedFiles);
        }
    }
    record Result(String itemId, String ownerSteamId, String url, boolean needsLegalAgreement) { }
    /** itemId may be nonempty when Steam created an item but content submission failed. */
    record Failure(String code, String message, String itemId, String ownerSteamId) { }
    interface Listener {
        /** The host will never read contentDirectory or previewFile again. Called exactly once
         * off EDT on success, preparation failure, cancellation or rejection, even after close().
         * Release caller-owned temporary files here; this is not publication success or a UI callback. */
        default void sourceReleased() { }
        default void progress(String stage, int percent) { }
        /** Persist this identity. It is not an upload success; the item may have no content yet. */
        default void created(String itemId, String ownerSteamId) { }
        void completed(Result result);
        void failed(Failure failure);
    }
    /** May open a Steam session; never call on EDT. */
    Account account();
    /** Call only after the user confirms the account, target and visibility. */
    void publish(Request request, Listener listener);
    /**
     * Refuses future requests and suppresses future UI callbacks. Stops work before submission
     * when possible; Steam submission already accepted cannot be rolled back. The host retains
     * its journal and upload snapshot until Steam finishes. Context shutdown closes this service.
     * sourceReleased() is a resource-lifetime notification and still runs after close().
     */
    @Override void close();
}
