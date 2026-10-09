package com.group_finity.mascot.lumi.plugin;

import java.util.*;

/** Generates a complete, original minimal character configuration without copying built-in assets. */
public final class CharacterDefinition {
    private CharacterDefinition() { }
    /** Per-character held rendering. LEGACY preserves existing generated output and sprite behavior.
     * PHYSICS rotates around the existing fixed hold point (ImageAnchor minus Offset 0,120),
     * independently of frame/audio playback. Available only for the automatic profile. */
    public enum HeldMotion { LEGACY, PHYSICS }
    /** Historical automatic-output reconstruction policies. New characters should use the
     * existing automatic generation overloads without this argument. LEGACY reproduces the
     * original output; SHARED reproduces silent first-Idle helper poses; SHARED_MOVEMENT also
     * reproduces implicit tag/parade scenarios and their historical donor/sound rules.
     * These compatibility policies are not authoring-preview or activity-metadata options. */
    public enum HelperVisuals { LEGACY, SHARED, SHARED_MOVEMENT }
    private static final List<String> BEHAVIORS = List.of("ModMakerIdle", "Stand", "Walk", "Fall", "Dragged", "Thrown", "ChaseMouse");
    /** Shared animation settings; templateId selects an SDK motion, empty selects a custom motion. */
    public record Motion(String name,String templateId,List<Frame> frames) {
        public Motion {templateId=templateId==null?"":templateId;frames=List.copyOf(frames);}
    }
    public static Map<String,String> generateMotions(String imageSet,List<Motion> motions,String previewImage) {
        return SharedMotionGenerator.generate(imageSet,motions,previewImage,false);
    }
    public static Map<String,String> generateStandaloneMotions(String imageSet,List<Motion> motions,String previewImage) {
        return SharedMotionGenerator.generate(imageSet,motions,previewImage,true);
    }
    public static Map<String,String> generateLimitedMotions(String imageSet,List<Motion> motions,String previewImage) {return LimitedMotionGenerator.generate(imageSet,motions,previewImage,false);}
    public static Map<String,String> generateStandaloneLimitedMotions(String imageSet,List<Motion> motions,String previewImage) {return LimitedMotionGenerator.generate(imageSet,motions,previewImage,true);}
    /** Whether a name is a safe single Windows folder component for automatic character output.
     * Accepts 1..64 UTF-16 code units, preserving Unicode/case/spaces; rejects reserved names,
     * separators, Windows-invalid/control characters, malformed Unicode, and trailing dot/space. */
    public static boolean validAutomaticImageSetName(String name) {return AutomaticAnimationGenerator.validName(name);}
    /** Generates automatic animations and fixed app reactions. Only Idle may have an empty templateId; other names equal supported template IDs. */
    public static Map<String,String> generateAutomaticAnimations(String imageSet,List<Motion> animations,String previewImage) {return LimitedMotionGenerator.generate(imageSet,animations,previewImage,false,true);}
    /** Standalone automatic profile; rejects guide images and emits no event/exit animation aliases. */
    public static Map<String,String> generateStandaloneAutomaticAnimations(String imageSet,List<Motion> animations,String previewImage) {return LimitedMotionGenerator.generate(imageSet,animations,previewImage,true,true);}
    /** Opt-in automatic held physics; null is rejected. Existing overloads remain LEGACY. */
    public static Map<String,String> generateAutomaticAnimations(String imageSet,List<Motion> animations,String previewImage,HeldMotion heldMotion) {
        return LimitedMotionGenerator.generate(imageSet,animations,previewImage,false,true,heldMotion);
    }
    /** Standalone counterpart; the held option is persisted in generated character configuration. */
    public static Map<String,String> generateStandaloneAutomaticAnimations(String imageSet,List<Motion> animations,String previewImage,HeldMotion heldMotion) {
        return LimitedMotionGenerator.generate(imageSet,animations,previewImage,true,true,heldMotion);
    }
    /** Reconstructs a historical automatic output for read-only comparison or recovery.
     * Retains the original bytes for the selected helper/held policies; null is rejected.
     * For new output use the overload without HelperVisuals. */
    public static Map<String,String> generateAutomaticAnimations(String imageSet,List<Motion> animations,String previewImage,HeldMotion heldMotion,HelperVisuals helperVisuals) {
        return LimitedMotionGenerator.generate(imageSet,animations,previewImage,false,true,heldMotion,helperVisuals);
    }
    /** Standalone historical-output reconstruction; does not alter an installed mod or grant consent. */
    public static Map<String,String> generateStandaloneAutomaticAnimations(String imageSet,List<Motion> animations,String previewImage,HeldMotion heldMotion,HelperVisuals helperVisuals) {
        return LimitedMotionGenerator.generate(imageSet,animations,previewImage,true,true,heldMotion,helperVisuals);
    }
    /** Names provided by every generated character definition. */
    public static Set<String> behaviorNames() { return Set.copyOf(BEHAVIORS); }
    /** Frame audio gain is optional: null inherits native gain or uses 0 dB for user WAV; explicit values are finite -80..24 dB. */
    public record Frame(String image,int durationMs,int anchorX,int anchorY,String templatePose,String sound,boolean muteDefaultSound,boolean loopSound,Double soundVolumeDb,CharacterTemplates.SoundPlayback soundPlayback) {
        public Frame(String image,int durationMs,int anchorX,int anchorY,String templatePose,String sound,boolean muteDefaultSound,boolean loopSound,Double soundVolumeDb){this(image,durationMs,anchorX,anchorY,templatePose,sound,muteDefaultSound,loopSound,soundVolumeDb,null);}
        public Frame(String image,int durationMs,int anchorX,int anchorY,String templatePose,String sound,boolean muteDefaultSound,boolean loopSound){this(image,durationMs,anchorX,anchorY,templatePose,sound,muteDefaultSound,loopSound,null);}
        public Frame(String image,int durationMs,int anchorX,int anchorY,String templatePose){this(image,durationMs,anchorX,anchorY,templatePose,"",false,false);}
        public Frame {
            CharacterDefinition.validateSoundVolume(soundVolumeDb);sound=sound==null?"":sound;if(!sound.isEmpty()&&!validSoundPath(sound))throw new IllegalArgumentException("Invalid frame WAV path");}
        public Frame(String image,int durationMs,int anchorX,int anchorY) { this(image,durationMs,anchorX,anchorY, ""); }
    }
    static void validateSoundVolume(Double value) {
        if(value != null && (!Double.isFinite(value) || value < -80 || value > 24))
            throw new IllegalArgumentException("Frame sound volume must be finite and within -80..24 dB");
    }
    /** Returns package-relative UTF-8 XML files. The caller supplies PNG files at image-set-relative paths. */
    public static Map<String, String> generate(String imageSet, List<Frame> frames) {
        if (imageSet == null || !imageSet.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")
                || Set.of("lumi", "credit", "unused").contains(imageSet.toLowerCase(Locale.ROOT)))
            throw new IllegalArgumentException("Invalid or reserved image set");
        validateFrames(frames);
        return generateBehaviors(imageSet, List.of(new Behavior("ModMakerIdle", "ModMakerIdle", frames)));
    }
    /** A named frame sequence. Kind selects built-in movement semantics; use ModMakerIdle for custom poses. */
    public record Behavior(String name, String kind, List<Frame> frames, String templateId, String sound, boolean muteDefaultSound) {
        public Behavior(String name,String kind,List<Frame> frames,String templateId,String sound) { this(name,kind,frames,templateId,sound,false); }
        public Behavior(String name,String kind,List<Frame> frames,String templateId) { this(name,kind,frames,templateId, ""); }
        public Behavior(String name,String kind,List<Frame> frames) { this(name,kind,frames, ""); }
        public Behavior { frames = List.copyOf(frames); sound = sound == null ? "" : sound; if (!sound.isEmpty() && !validSoundPath(sound)) throw new IllegalArgumentException("Invalid behavior WAV path"); }
    }
    /** Generates distinct named behaviors. ModMakerIdle is required; omitted engine behaviors use its frames.
     * Names are ASCII identifiers (1..64), kinds come from behaviorNames(), at most 512 behaviors, 4096 frames per behavior
     * and 16384 total supplied frames. Built-in names must use their matching kind. */
    public static Map<String, String> generateBehaviors(String imageSet, List<Behavior> supplied) {
        return generateBehaviors(imageSet, supplied, null);
    }
    /** Generates behaviors with an optional character chooser thumbnail. previewImage is a safe
     * image-set-relative PNG path, supplied by the caller; null keeps the first idle frame.
     * This changes info.xml only, never animation frames, and does not read or write files. */
    public static Map<String, String> generateBehaviors(String imageSet, List<Behavior> supplied, String previewImage) {
        if (supplied != null && supplied.stream().anyMatch(b -> b != null && b.templateId() != null && !b.templateId().isEmpty()))
            return CharacterTemplateGenerator.generate(imageSet, supplied, previewImage);
        if (previewImage != null && !validPngPath(previewImage))
            throw new IllegalArgumentException("Invalid preview PNG path");
        if (imageSet == null || !imageSet.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")
                || Set.of("lumi", "credit", "unused").contains(imageSet.toLowerCase(Locale.ROOT)))
            throw new IllegalArgumentException("Invalid or reserved image set");
        if (supplied == null || supplied.isEmpty() || supplied.size() > 512)
            throw new IllegalArgumentException("Require 1..512 behaviors");
        Map<String, Behavior> definitions = new LinkedHashMap<>();
        int count = 0;
        for (Behavior behavior : supplied) {
            if (behavior == null || behavior.name() == null || !behavior.name().matches("[A-Za-z][A-Za-z0-9_-]{0,63}")
                    || !BEHAVIORS.contains(behavior.kind()) || definitions.putIfAbsent(behavior.name(), behavior) != null
                    || BEHAVIORS.contains(behavior.name()) && !behavior.name().equals(behavior.kind()))
                throw new IllegalArgumentException("Invalid or duplicate behavior name/kind");
            validateFrames(behavior.frames()); count += behavior.frames().size();
        }
        if (count > 16384) throw new IllegalArgumentException("Behavior frames exceed 16384");
        Behavior idle = definitions.get("ModMakerIdle");
        if (idle == null) throw new IllegalArgumentException("ModMakerIdle is required");
        for (String name : BEHAVIORS) definitions.putIfAbsent(name, new Behavior(name, name, idle.frames()));
        String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Mascot xmlns=\"http://www.group-finity.com/Mascot\">\n";
        StringBuilder actions = new StringBuilder(header).append("<ActionList>\n");
        for (Behavior behavior : definitions.values()) {
            String name = behavior.name();
            String type = switch (behavior.kind()) {
                case "Fall", "Thrown" -> "Type=\"Embedded\" Class=\"com.group_finity.mascot.action.Fall\" RegistanceX=\"0.05\" RegistanceY=\"0.1\" Gravity=\"2\"";
                case "Walk" -> "Type=\"Move\" BorderType=\"Floor\" TargetX=\"${mascot.anchor.x - 64}\"";
                case "Dragged" -> "Type=\"Embedded\" Class=\"com.group_finity.mascot.action.Dragged\"";
                default -> "Type=\"Stay\" BorderType=\"Floor\"";
            };
            actions.append("<Action Name=\"").append(name).append("\" ").append(type).append("><Animation>\n");
            for (Frame frame : behavior.frames()) actions.append("<Pose Image=\"/").append(xmlImage(frame.image())).append("\" ImageAnchor=\"")
                    .append(frame.anchorX()).append(',').append(frame.anchorY()).append("\" Velocity=\"").append(behavior.kind().equals("Walk") ? "-2,0" : "0,0").append("\" Duration=\"")
                    .append(Math.max(1, Math.round(frame.durationMs() * 60.0 / 1000))).append("\"/>\n");
            actions.append("</Animation></Action>\n");
        }
        actions.append("</ActionList></Mascot>\n");
        StringBuilder behaviors = new StringBuilder(header).append("<BehaviorList>\n");
        for (String name : definitions.keySet())
            behaviors.append("<Behavior Name=\"").append(name).append("\" Frequency=\"").append(name.equals("ModMakerIdle") ? 1 : 0)
                    .append("\"><NextBehaviorList Add=\"false\"><BehaviorReference Name=\"ModMakerIdle\" Frequency=\"1\"/></NextBehaviorList></Behavior>\n");
        behaviors.append("</BehaviorList></Mascot>\n");
        String prefix = "img/" + imageSet + "/conf/";
        return withSounds(imageSet, supplied, Map.of(prefix + "actions.xml", actions.toString(), prefix + "behaviors.xml", behaviors.toString(),
                prefix + "info.xml", header + "<Information><Name>" + imageSet + "</Name><PreviewImage>" + (xmlImage(previewImage == null ? idle.frames().getFirst().image() : previewImage)) + "</PreviewImage></Information></Mascot>\n"));
    }
    /** Final export without built-in guide artwork/audio. Editing and preview use generateBehaviors instead. */
    public static Map<String,String> generateStandaloneBehaviors(String imageSet,List<Behavior> behaviors,String previewImage) {
        return StandaloneCharacterGenerator.generate(imageSet,behaviors,previewImage);
    }
    public static Map<String,String> generateStandaloneBehaviors(String imageSet,List<Behavior> behaviors) {
        return generateStandaloneBehaviors(imageSet,behaviors,null);
    }
    /** Shared action authoring. An empty templateId creates an original stationary action. */
    public record Action(String name,String templateId,List<Frame> frames,String sound,boolean muteDefaultSound) {
        public Action {frames=List.copyOf(frames);templateId=templateId==null?"":templateId;sound=sound==null?"":sound;
            if(!sound.isEmpty()&&!validSoundPath(sound))throw new IllegalArgumentException("Invalid action WAV path");}
    }
    /** The new action APIs use the engine's real 25 Hz clock; legacy behavior API timing is unchanged. */
    public static int actionTickDurationMs(int durationMs) {
        if(durationMs<1||durationMs>60000)throw new IllegalArgumentException("Invalid action frame duration");
        return Math.max(1,(int)Math.round(durationMs/40.0))*40;
    }
    public static Map<String,String> generateActions(String imageSet,List<Action> actions,String previewImage) {
        return SharedActionGenerator.generate(imageSet,actions,previewImage,false);
    }
    public static Map<String,String> generateStandaloneActions(String imageSet,List<Action> actions,String previewImage) {
        return SharedActionGenerator.generate(imageSet,actions,previewImage,true);
    }
    /**
     * Checks a relative WAV path beneath {@code sound/}, including Unicode names and spaces.
     * Rejects traversal, absolute paths, empty components, control characters, malformed UTF-16,
     * Windows device names and characters that cannot be used in Windows file names.
     * This validates the path syntax; the host still resolves it through the approved mod resources.
     */
    public static boolean validSoundPath(String path) {
        if (path == null || !path.startsWith("sound/") || !path.endsWith(".wav")) return false;
        for (String part : path.substring(6).split("/", -1)) {
            if (part.isBlank() || part.equals(".") || part.equals("..")
                    || part.endsWith(".") || part.endsWith(" ")) return false;
            String stem = part.split("\\.", 2)[0].stripTrailing();
            if (stem.matches("(?i)(con|prn|aux|nul|com[1-9¹²³]|lpt[1-9¹²³]|conin\\$|conout\\$)")) return false;
            for (int i = 0; i < part.length();) {
                int cp = part.codePointAt(i); i += Character.charCount(cp);
                if (Character.isISOControl(cp) || cp >= 0xD800 && cp <= 0xDFFF
                        || cp == 0xFFFE || cp == 0xFFFF || "<>:\"\\|?*".indexOf(cp) >= 0) return false;
            }
        }
        return true;
    }
    static Map<String,String> withSounds(String imageSet,List<Behavior> supplied,Map<String,String> files) {
        Map<String,String> sounds = new TreeMap<>();
        for(Behavior behavior:supplied) if(!behavior.sound().isEmpty() || behavior.muteDefaultSound()) {
            sounds.put(behavior.name(),behavior.sound());
            if(behavior.templateId()!=null&&!behavior.templateId().isEmpty()) sounds.put(behavior.templateId(),behavior.sound());
        }
        if(sounds.isEmpty()) return files;
        StringBuilder properties=new StringBuilder("# Behavior entry WAV overrides; an empty entry explicitly silences inherited audio.\n");
        sounds.forEach((name,path)->properties.append(name).append('=').append(path).append('\n'));
        Map<String,String> result=new LinkedHashMap<>(files);
        result.put("img/"+imageSet+"/conf/behavior-sounds.properties",properties.toString()); return Map.copyOf(result);
    }
    static String xmlImage(String image) { return image.startsWith("lumi:") ? "@lumi/"+image.substring(5) : image; }
    static boolean validPngPath(String path) {
        return path != null && (path.matches("lumi:[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*\\.png") || path.matches("[A-Za-z0-9_-][A-Za-z0-9_./-]*\\.png")
                && !path.contains("..") && !path.contains("//"));
    }
    private static void validateFrames(List<Frame> frames) {
        if (frames == null || frames.isEmpty() || frames.size() > 4096) throw new IllegalArgumentException("Require 1..4096 frames");
        for (Frame frame : frames) {
            if (frame == null || frame.image() == null || !validPngPath(frame.image())
                    || frame.image().contains("..") || frame.image().contains("//") || frame.durationMs() < 16 || frame.durationMs() > 60000
                    || frame.anchorX() < -8192 || frame.anchorX() > 8192 || frame.anchorY() < -8192 || frame.anchorY() > 8192)
                throw new IllegalArgumentException("Invalid PNG frame, duration or anchor");
        }
    }
}
