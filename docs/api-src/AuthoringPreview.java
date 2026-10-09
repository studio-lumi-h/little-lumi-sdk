package com.group_finity.mascot.lumi.plugin;

import java.util.*;

/**
 * Experimental, explicit live authoring in the running desktop app. A session owns one isolated
 * real engine actor. It never installs a mod, approves code, changes the summon pool, or persists
 * variables. Snapshots accept only bounded PNG/WAV bytes and declarative VisualLogic rules;
 * no XML, scripts, JARs or filesystem paths are accepted. Stop the session when its editor closes.
 */
public final class AuthoringPreview {
    private AuthoringPreview() { }
    /** Frame audio gain is optional: null inherits native gain or uses 0 dB for user WAV; explicit values are finite -80..24 dB. */
    public record Frame(byte[] png, int durationMs, int anchorX, int anchorY,String hostImage,String templatePose,String sound,boolean muteDefaultSound,boolean loopSound,Double soundVolumeDb,CharacterTemplates.SoundPlayback soundPlayback) {
        public Frame(byte[] png,int durationMs,int anchorX,int anchorY,String hostImage,String templatePose,String sound,boolean muteDefaultSound,boolean loopSound,Double soundVolumeDb){this(png,durationMs,anchorX,anchorY,hostImage,templatePose,sound,muteDefaultSound,loopSound,soundVolumeDb,null);}
        public Frame(byte[] png,int durationMs,int anchorX,int anchorY,String hostImage,String templatePose,String sound,boolean muteDefaultSound,boolean loopSound){this(png,durationMs,anchorX,anchorY,hostImage,templatePose,sound,muteDefaultSound,loopSound,null);}
        public Frame(byte[] png,int durationMs,int anchorX,int anchorY,String hostImage,String templatePose){this(png,durationMs,anchorX,anchorY,hostImage,templatePose,"",false,false);}
        public Frame(byte[] png,int durationMs,int anchorX,int anchorY) { this(png,durationMs,anchorX,anchorY,"", ""); }
        public Frame {
            CharacterDefinition.validateSoundVolume(soundVolumeDb);
            sound=sound==null?"":sound;if(!sound.isEmpty()&&!CharacterDefinition.validSoundPath(sound))throw new IllegalArgumentException("Invalid frame WAV path");
            hostImage = hostImage == null ? "" : hostImage; templatePose = templatePose == null ? "" : templatePose;
            if (!hostImage.isEmpty()) { CharacterTemplates.imagePath(hostImage); png = new byte[8]; }
            Objects.requireNonNull(png);
            if (png.length < 8 || png.length > 4 * 1024 * 1024 || durationMs < 16 || durationMs > 60000
                    || Math.abs((long)anchorX) > 8192 || Math.abs((long)anchorY) > 8192)
                throw new IllegalArgumentException("Invalid preview frame");
            png = png.clone();
        }
        @Override public byte[] png() { return png.clone(); }
    }
    /** Uses the same name/kind contract as CharacterDefinition.Behavior. */
    public record Behavior(String name, String kind, List<Frame> frames, String templateId, String sound, boolean muteDefaultSound) {
        public Behavior(String name,String kind,List<Frame> frames,String templateId,String sound) { this(name,kind,frames,templateId,sound,false); }
        public Behavior(String name,String kind,List<Frame> frames,String templateId) { this(name,kind,frames,templateId, ""); }
        public Behavior(String name,String kind,List<Frame> frames) { this(name,kind,frames, ""); }
        public Behavior { frames = List.copyOf(frames); sound = sound == null ? "" : sound; if(!sound.isEmpty() && !CharacterDefinition.validSoundPath(sound)) throw new IllegalArgumentException("Invalid behavior WAV path"); }
    }
    /** One shared action definition; native scenarios reuse templateId directly. */
    public record Action(String name,String templateId,List<Frame> frames,String sound,boolean muteDefaultSound) {
        public Action {frames=List.copyOf(frames);templateId=templateId==null?"":templateId;sound=sound==null?"":sound;
            if(!sound.isEmpty()&&!CharacterDefinition.validSoundPath(sound))throw new IllegalArgumentException("Invalid action WAV path");}
    }
    /** Animation shared by all execution bindings of a public motion template. */
    public record Motion(String name,String templateId,List<Frame> frames) {
        public Motion {templateId=templateId==null?"":templateId;frames=List.copyOf(frames);}
    }
    public record Snapshot(List<Frame> frames, VisualLogic.Document document, Map<String, byte[]> audio, List<Behavior> behaviors,List<Action> actions,List<Motion> motions,boolean limitedRuntime,boolean automaticRuntime,CharacterDefinition.HeldMotion heldMotion) {
        /** Backward-compatible automatic snapshot: keeps legacy held rendering. */
        public Snapshot(List<Frame> frames,VisualLogic.Document document,Map<String,byte[]> audio,List<Behavior> behaviors,List<Action> actions,List<Motion> motions,boolean limitedRuntime,boolean automaticRuntime){this(frames,document,audio,behaviors,actions,motions,limitedRuntime,automaticRuntime,CharacterDefinition.HeldMotion.LEGACY);}
        public Snapshot(List<Frame> frames,VisualLogic.Document document,Map<String,byte[]> audio,List<Behavior> behaviors,List<Action> actions,List<Motion> motions,boolean limitedRuntime){this(frames,document,audio,behaviors,actions,motions,limitedRuntime,false);}
        /** Live automatic preview without event/dialogue authoring. Paired activities own two actors; parade/gathering own three. State.mascotId identifies the primary actor. */
        public static Snapshot forAutomaticAnimations(List<Motion> animations,Map<String,byte[]> audio) {
            return forAutomaticAnimations(animations,audio,CharacterDefinition.HeldMotion.LEGACY);
        }
        /** Uses the same held rendering as the matching automatic export overload. */
        public static Snapshot forAutomaticAnimations(List<Motion> animations,Map<String,byte[]> audio,CharacterDefinition.HeldMotion heldMotion) {
            var idle=animations.stream().filter(m->m.name().equals("Idle")).findFirst().orElseThrow(()->new IllegalArgumentException("Idle animation required"));
            return new Snapshot(idle.frames(),new VisualLogic.Document("maker.automatic","Preview",Map.of(),Map.of(),List.of()),audio,List.of(),List.of(),animations,true,true,heldMotion);
        }
        public Snapshot(List<Frame> frames,VisualLogic.Document document,Map<String,byte[]> audio,List<Behavior> behaviors,List<Action> actions,List<Motion> motions){this(frames,document,audio,behaviors,actions,motions,false);}
        public static Snapshot forLimitedMotions(List<Motion> motions,VisualLogic.Document document,Map<String,byte[]> audio){var idle=motions.stream().filter(m->m.name().equals("Idle")).findFirst().orElseThrow(()->new IllegalArgumentException("Idle motion required"));return new Snapshot(idle.frames(),document,audio,List.of(),List.of(),motions,true);}

        public Snapshot(List<Frame> frames,VisualLogic.Document document,Map<String,byte[]> audio,List<Behavior> behaviors,List<Action> actions) {
            this(frames,document,audio,behaviors,actions,List.of());
        }
        public static Snapshot forMotions(List<Motion> motions,VisualLogic.Document document,Map<String,byte[]> audio) {
            var idle=motions.stream().filter(m->m.name().equals("Idle")).findFirst().orElseThrow(()->new IllegalArgumentException("Idle motion required"));
            return new Snapshot(idle.frames(),document,audio,List.of(),List.of(),motions);
        }
        public Snapshot(List<Frame> frames,VisualLogic.Document document,Map<String,byte[]> audio,List<Behavior> behaviors) {
            this(frames,document,audio,behaviors,List.of());
        }
        public static Snapshot forActions(List<Action> actions,VisualLogic.Document document,Map<String,byte[]> audio) {
            var idle=actions.stream().filter(a->a.name().equals("Idle")).findFirst().orElseThrow(()->new IllegalArgumentException("Idle action required"));
            return new Snapshot(idle.frames(),document,audio,List.of(),actions);
        }
        /** Backward-compatible common-frame snapshot. */
        public Snapshot(List<Frame> frames, VisualLogic.Document document, Map<String, byte[]> audio) {
            this(frames, document, audio, List.of());
        }
        public Snapshot {
            Objects.requireNonNull(heldMotion,"heldMotion");
            if(!automaticRuntime && heldMotion!=CharacterDefinition.HeldMotion.LEGACY)throw new IllegalArgumentException("Held physics requires automatic preview");
            if(automaticRuntime && (!limitedRuntime || !document.rules().isEmpty() || !document.dialogues().isEmpty() || !document.variables().isEmpty()))throw new IllegalArgumentException("Automatic preview cannot contain visual logic");
            frames = List.copyOf(frames); Objects.requireNonNull(document); Objects.requireNonNull(audio);
            if (frames.isEmpty() || frames.size() > 16384 || audio.size() > 256)
                throw new IllegalArgumentException("Preview asset count exceeds budget");
            behaviors = List.copyOf(behaviors); actions=List.copyOf(actions);motions=List.copyOf(motions);
            if((actions.isEmpty()?0:1)+(behaviors.isEmpty()?0:1)+(motions.isEmpty()?0:1)>1)throw new IllegalArgumentException("Choose motions, actions or legacy behaviors");
            if(!limitedRuntime&&document.rules().stream().anyMatch(r->r.event().equals("exit")&&!r.behavior().isEmpty()))throw new IllegalArgumentException("Exit motion requires limited character profile");
            if(limitedRuntime&&motions.isEmpty())throw new IllegalArgumentException("Limited preview requires motions");
            Set<String> allowed = limitedRuntime?new HashSet<>():new HashSet<>(CharacterDefinition.behaviorNames());
            List<Frame> effective = frames;
            if (!behaviors.isEmpty()) {
                List<CharacterDefinition.Behavior> definitions = new ArrayList<>();
                List<Frame> all = new ArrayList<>();
                for (Behavior behavior : behaviors) {
                    all.addAll(behavior.frames()); allowed.add(behavior.name());
                    definitions.add(new CharacterDefinition.Behavior(behavior.name(), behavior.kind(), behavior.frames().stream()
                            .map(f -> new CharacterDefinition.Frame(f.hostImage().isEmpty()?"frame.png":f.hostImage(), f.durationMs(), f.anchorX(), f.anchorY(),f.templatePose(),f.sound(),f.muteDefaultSound(),f.loopSound(),f.soundVolumeDb(),f.soundPlayback())).toList(), behavior.templateId(),behavior.sound(),behavior.muteDefaultSound()));
                }
                CharacterDefinition.generateBehaviors("PreviewValidation", definitions);
                effective = all;
            }
            if(!actions.isEmpty()) {
                List<CharacterDefinition.Action> definitions=new ArrayList<>();List<Frame> all=new ArrayList<>();
                for(var action:actions) {
                    all.addAll(action.frames());allowed.add(action.name());
                    definitions.add(new CharacterDefinition.Action(action.name(),action.templateId(),action.frames().stream()
                        .map(f->new CharacterDefinition.Frame(f.hostImage().isEmpty()?"frame.png":f.hostImage(),f.durationMs(),f.anchorX(),f.anchorY(),f.templatePose(),f.sound(),f.muteDefaultSound(),f.loopSound(),f.soundVolumeDb(),f.soundPlayback())).toList(),action.sound(),action.muteDefaultSound()));
                }
                CharacterDefinition.generateActions("PreviewValidation",definitions,null);effective=all;
            }
            if(!motions.isEmpty()) {
                List<CharacterDefinition.Motion> definitions=new ArrayList<>();List<Frame> all=new ArrayList<>();
                for(var motion:motions) {
                    all.addAll(motion.frames());allowed.add(motion.name());
                    definitions.add(new CharacterDefinition.Motion(motion.name(),motion.templateId(),motion.frames().stream()
                        .map(f->new CharacterDefinition.Frame(f.hostImage().isEmpty()?"frame.png":f.hostImage(),f.durationMs(),f.anchorX(),f.anchorY(),f.templatePose(),f.sound(),f.muteDefaultSound(),f.loopSound(),f.soundVolumeDb(),f.soundPlayback())).toList()));
                }
                if(automaticRuntime)CharacterDefinition.generateAutomaticAnimations("PreviewValidation",definitions,null,heldMotion);else if(limitedRuntime)CharacterDefinition.generateLimitedMotions("PreviewValidation",definitions,null);else CharacterDefinition.generateMotions("PreviewValidation",definitions,null);effective=all;
            }
            if (effective.size() > 16384) throw new IllegalArgumentException("Preview asset count exceeds budget");
            long bytes = 0; for (Frame frame : effective) bytes += frame.png.length;
            Map<String, byte[]> copy = new LinkedHashMap<>();
            for (var entry : audio.entrySet()) {
                String key = entry.getKey(); byte[] value = entry.getValue();
                if (!CharacterDefinition.validSoundPath(key)
                        || value == null || value.length < 12 || value.length > 4 * 1024 * 1024)
                    throw new IllegalArgumentException("Invalid preview WAV asset");
                bytes += value.length; copy.put(key, value.clone());
            }
            if (bytes > 24 * 1024 * 1024) throw new IllegalArgumentException("Preview exceeds 24 MiB");
            for (var behavior : behaviors)
                if (!behavior.sound().isEmpty() && !copy.containsKey(behavior.sound())) throw new IllegalArgumentException("Missing behavior WAV");
            for(var action:actions)if(!action.sound().isEmpty()&&!copy.containsKey(action.sound()))throw new IllegalArgumentException("Missing action WAV");
            for(var frame:effective)if(!frame.sound().isEmpty()&&!copy.containsKey(frame.sound()))throw new IllegalArgumentException("Missing frame WAV");
            for (var line : document.dialogues().values())
                if (!line.audio().isEmpty() && !copy.containsKey(line.audio())) throw new IllegalArgumentException("Missing dialogue WAV");
            for (var rule : document.rules())
                if (!rule.behavior().isEmpty() && !allowed.contains(rule.behavior()))
                    throw new IllegalArgumentException("Unknown preview behavior");
            audio = Collections.unmodifiableMap(copy);
        }
        @Override public Map<String, byte[]> audio() {
            Map<String, byte[]> copy = new LinkedHashMap<>(); audio.forEach((key, value) -> copy.put(key, value.clone()));
            return Collections.unmodifiableMap(copy);
        }
    }
    public record State(boolean closed, int mascotId, long revision, Map<String, Double> variables, List<String> trace) {
        public State { variables = Map.copyOf(variables); trace = List.copyOf(trace); }
    }
    public enum ActivityPhase { IDLE, RUNNING, PAUSED, COMPLETED, CANCELLED, FAILED }
    /** Actual current native stage. Empty animation/step means no matching visible stage. */
    public record ActivityActor(int mascotId, String roleId, String animationId, String stepId) { }
    /** Reason is an invariant machine code, never localized host text. */
    public record ActivityState(String activityId, ActivityPhase phase, List<ActivityActor> actors, String reason) {
        public ActivityState { actors = List.copyOf(actors); }
    }
    public interface Session extends AutoCloseable {
        /** Off EDT. Run one bounded native activity with this session's isolated actors. */
        default void startActivity(String activityId) { throw new UnsupportedOperationException("Activity playback unavailable"); }
        /** Off EDT. Freeze/resume owned engine actors and their audio/props. */
        default void pauseActivity(boolean paused) { throw new UnsupportedOperationException("Activity playback unavailable"); }
        /** Off EDT. Cancel the activity and release its owned choreography/props/audio. */
        default void stopActivity() { throw new UnsupportedOperationException("Activity playback unavailable"); }
        /** Safe on any thread; does not advance playback. */
        default ActivityState activityState() { return new ActivityState("",ActivityPhase.IDLE,List.of(),""); }

        /** Off EDT. Validates completely before replacement; rejection preserves the previous actor.
         * Successful replacement preserves position but resets variables; ready is dispatched automatically. */
        void replace(Snapshot snapshot);
        /** Off EDT. Dispatches ready, click, or a menu event declared by the current document. */
        void dispatch(String event);
        /** Immutable bounded state/trace snapshot; safe on any thread. */
        State state();
        /** Idempotent. On EDT cleanup is scheduled; off EDT waits for owned resources to be released. */
        @Override void close();
    }
}
