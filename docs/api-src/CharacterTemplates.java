package com.group_finity.mascot.lumi.plugin;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.util.SafeXml;
import java.nio.file.*;
import java.util.*;
import org.w3c.dom.*;

/** Read-only templates supplied by the installed LUMI application. No artwork is redistributed. */
public final class CharacterTemplates {
    private CharacterTemplates() { }
    /** FRAME_ENTRY retriggers per frame/cycle; BEHAVIOR_ENTRY plays one logical sound per behavior episode. */
    public enum SoundPlayback { FRAME_ENTRY, BEHAVIOR_ENTRY }
    static SoundPlayback nativeSoundPlayback(String sound) {
        if(sound==null||sound.isBlank())return SoundPlayback.FRAME_ENTRY;
        String name=sound.replace('\\','/');name=name.substring(name.lastIndexOf('/')+1).replaceFirst("^lumi:","").replaceFirst("\\.wav$","");
        return com.group_finity.mascot.lumi.SoundGate.REPEATABLE.contains(name)?SoundPlayback.FRAME_ENTRY:SoundPlayback.BEHAVIOR_ENTRY;
    }
    public record TemplateFrame(String slot,String image,int durationMs,int anchorX,int anchorY,String sound,double volumeDb,SoundPlayback soundPlayback) {
        public TemplateFrame(String slot,String image,int durationMs,int anchorX,int anchorY,String sound,double volumeDb){this(slot,image,durationMs,anchorX,anchorY,sound,volumeDb,nativeSoundPlayback(sound));}
        public TemplateFrame {if(soundPlayback==null)soundPlayback=nativeSoundPlayback(sound);}
        public TemplateFrame(String slot,String image,int durationMs,int anchorX,int anchorY){this(slot,image,durationMs,anchorX,anchorY,"",0);}
    }
    public static Set<String> requiredBehaviorIds() { return Set.of("ChaseMouse", "Fall", "Dragged", "Thrown"); }
    public record Template(String id, String displayName, List<TemplateFrame> frames, boolean required, boolean hasDefaultSound) {
        public Template(String id,String displayName,List<TemplateFrame> frames,boolean required) { this(id,displayName,frames,required,false); }
        public Template(String id,String displayName,List<TemplateFrame> frames) { this(id,displayName,frames,requiredBehaviorIds().contains(id)); }
        public Template { frames = List.copyOf(frames); }
    }
    /** IDs owned by source actions and behaviors; editors reserve these except the template's own ID. */
    public static Set<String> reservedIds() {
        Source source=source(); Set<String> names=new LinkedHashSet<>(source.byName.keySet());
        for(Element e:elements(source.behaviors,"Behavior")) names.add(e.getAttribute("Name"));
        names.addAll(MotionCatalog.motionIds().keySet());names.addAll(CharacterDefinition.behaviorNames());names.add("Idle");names.add("MakerFallPhysics");names.add("MakerDragPhysics");names.add("MakerHappyHop");names.addAll(AutomaticAnimationGenerator.SUPPORTED);
        return Set.copyOf(names);
    }
    /** Reads the installed catalog; missing/invalid host content fails explicitly. */
    public static List<Template> behaviors() {
        Source source = source(); List<Template> result = new ArrayList<>();
        for (Element behavior : elements(source.behaviors, "Behavior")) {
            String id = behavior.getAttribute("Name");
            LinkedHashMap<String, Element> reachable = reachable(source, id);
            List<TemplateFrame> frames = new ArrayList<>();
            boolean hasSound = false;
            for (var entry : reachable.entrySet()) {
                List<Element> poses = elements(entry.getValue(), "Pose");
                for (int i = 0; i < poses.size(); i++) {
                    Element pose = poses.get(i);
                    hasSound |= !pose.getAttribute("Sound").isBlank();
                    String[] anchor = pose.getAttribute("ImageAnchor").split(",");
                    if (!pose.hasAttribute("Image")) continue;
                    frames.add(new TemplateFrame(entry.getKey()+"/"+i, reference(pose.getAttribute("Image")),
                        Math.max(16, (int)Math.round(number(pose.getAttribute("Duration"), 6)*1000.0/60)),
                        anchor.length == 2 ? number(anchor[0],96) : 96, anchor.length == 2 ? number(anchor[1],192) : 192));
                }
            }
            if (!frames.isEmpty()) result.add(new Template(id, com.group_finity.mascot.lumi.SpeechDirector.localized(id,id), frames, requiredBehaviorIds().contains(id), hasSound));
        }
        return List.copyOf(result);
    }
    /** A shared primitive: only animations directly owned by this named action, never a scenario graph. */
    public record ActionTemplate(String id,String displayName,List<TemplateFrame> frames,boolean hasDefaultSound) {
        public ActionTemplate {frames=List.copyOf(frames);}
    }
    public static List<ActionTemplate> actions() {
        Source source=source();List<ActionTemplate> result=new ArrayList<>();
        for(var entry:source.byName.entrySet()) {
            List<Element> animations=direct(entry.getValue(),"Animation");
            if(animations.isEmpty())continue;
            List<TemplateFrame> frames=new ArrayList<>();boolean sound=false;
            for(int ai=0;ai<animations.size();ai++) {
                List<Element> poses=direct(animations.get(ai),"Pose");
                for(Element pose:poses)sound|=!pose.getAttribute("Sound").isBlank();
                for(var group:ActionFrameGroups.groups(poses)) {
                    Element pose=group.poses().getFirst();if(!pose.hasAttribute("Image"))continue;
                    String[] anchor=pose.getAttribute("ImageAnchor").split(",");String audio=pose.getAttribute("Sound");
                    double volume=0;try{volume=Double.parseDouble(pose.getAttribute("Volume"));}catch(NumberFormatException ignored){}
                    frames.add(new TemplateFrame(group.slot(entry.getKey(),ai),reference(pose.getAttribute("Image")),group.ticks()>0?group.ticks()*40:Math.max(40,number(pose.getAttribute("Duration"),1)*40),
                        anchor.length==2?number(anchor[0],96):96,anchor.length==2?number(anchor[1],192):192,audio.isBlank()?"":"lumi:"+audio.replaceFirst("^/", ""),volume));
                }
            }
            if(!frames.isEmpty())result.add(new ActionTemplate(entry.getKey(),com.group_finity.mascot.lumi.SpeechDirector.localized(entry.getKey(),entry.getKey()),frames,sound));
        }
        return List.copyOf(result);
    }
    /** One editable animation shared by execution actions whose physical parameters remain independent. */
    public record MotionTemplate(String id,String displayName,List<TemplateFrame> frames,List<String> bindings,boolean hasDefaultSound) {
        public MotionTemplate {frames=List.copyOf(frames);bindings=List.copyOf(bindings);}
    }
    /** Reachable motions only; bindings[0] supplies canonical timing, artwork and frame sound. */
    public static List<MotionTemplate> motions() {return MotionCatalog.catalog();}
    /** Supported minimal character motions; Pinched supplies only its neutral pose. */
    /** Closed automatic animation IDs, including app-owned requests; no user event configuration. */
    public static List<String> automaticAnimationIds() {return AutomaticAnimationGenerator.SUPPORTED;}
    /** Basic animations, fixed activities and app reactions. Execution conditions and same-set partner roles belong to the host. */
    public static List<MotionTemplate> automaticAnimations() {return LimitedMotionGenerator.catalog(true);}
    public static List<MotionTemplate> limitedMotions() {return LimitedMotionGenerator.catalog();}
    static List<Element> direct(Element element,String tag) {
        List<Element> result=new ArrayList<>();for(Node n=element.getFirstChild();n!=null;n=n.getNextSibling())
            if(n instanceof Element e&&e.getTagName().equals(tag))result.add(e);return result;
    }
    /** Bounded original PNG bytes. Only named files in the built-in Lumi set are accepted. */
    public static byte[] image(String reference) {
        try {
            Path path = imagePath(reference); long size = Files.size(path);
            if (size < 8 || size > 4*1024*1024) throw new IllegalArgumentException("Host image size exceeds budget");
            return Files.readAllBytes(path);
        } catch (java.io.IOException failure) { throw new IllegalStateException("LUMI artwork unavailable: "+reference, failure); }
    }
    /** Read-only original LUMI WAV, at most 4 MiB, never a project resource or arbitrary path. */
    public static byte[] sound(String reference) {
        if(reference==null||!reference.matches("lumi:[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*\\.wav"))throw new IllegalArgumentException("Invalid LUMI sound reference");
        try{Path path=ModResources.file(Main.IMAGE_DIRECTORY.resolve("Lumi/sound").resolve(reference.substring(5)));long size=Files.size(path);if(size<12||size>4*1024*1024)throw new IllegalArgumentException("Host WAV exceeds size limit");return Files.readAllBytes(path);}
        catch(java.io.IOException e){throw new IllegalStateException("LUMI sound unavailable: "+reference,e);}
    }
    static Path imagePath(String reference) {
        if (reference == null || !reference.matches("lumi:[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*\\.png")) throw new IllegalArgumentException("Invalid LUMI image reference");
        Path path = Main.IMAGE_DIRECTORY.resolve("Lumi").resolve(reference.substring(5));
        return ModResources.file(path);
    }
    static String reference(String path) {
        String ref = "lumi:" + path.replaceFirst("^/", ""); imagePath(ref); return ref;
    }
    static int number(String value,int fallback) { try { return Integer.parseInt(value); } catch (NumberFormatException e) { return fallback; } }
    static List<Element> elements(Node node,String tag) {
        NodeList list = node instanceof Document d ? d.getElementsByTagName(tag) : ((Element)node).getElementsByTagName(tag);
        List<Element> out = new ArrayList<>(); for(int i=0;i<list.getLength();i++) out.add((Element)list.item(i)); return out;
    }
    static record Source(Document actions,Document behaviors,Map<String,Element> byName) { }
    static Source source() {
        try {
            var builder = SafeXml.newDocumentBuilder(true);
            Document actions = builder.parse(ModResources.file(Main.IMAGE_DIRECTORY.resolve("Lumi/conf/actions.xml")).toFile());
            Document behaviors = builder.parse(ModResources.file(Main.IMAGE_DIRECTORY.resolve("Lumi/conf/behaviors.xml")).toFile());
            Map<String,Element> names = new LinkedHashMap<>();
            for(Element action:elements(actions,"Action")) if(action.hasAttribute("Name")) names.put(action.getAttribute("Name"),action);
            return new Source(actions,behaviors,names);
        } catch(Exception failure) { throw new IllegalStateException("Installed LUMI templates unavailable",failure); }
    }
    static LinkedHashMap<String,Element> reachable(Source source,String id) {
        LinkedHashMap<String,Element> result=new LinkedHashMap<>(); visit(source,id,result); return result;
    }
    static void visit(Source source,String id,Map<String,Element> result) {
        if(result.containsKey(id)) return; Element action=source.byName.get(id); if(action==null)return;
        result.put(id,action); for(Element ref:elements(action,"ActionReference")) visit(source,ref.getAttribute("Name"),result);
    }
}
