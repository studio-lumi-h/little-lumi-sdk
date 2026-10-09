package com.group_finity.mascot.lumi.plugin;

import java.util.*;

/**
 * Read-only descriptions of the host's automatic animation activities.
 * IDs are stable, non-localized identifiers. No assets are loaded and no animation is registered.
 * Roles describe ordered logical stages of a successful interaction; contacts can fail or time out.
 * They are not a wall-clock timeline: movement,
 * conditional repetitions and concurrent partners remain engine controlled.
 */
public final class AnimationActivities {
  private AnimationActivities() { }

  /**
   * A logical stage and its actual authored visual donor. An empty effectiveAnimationId means
   * the stage is skipped. muted describes host-enforced silence, not the authored frame's sound.
   * id identifies a stage within its role, animationId identifies the optional animation to edit.
   */
  public record Step(String id, String animationId, String effectiveAnimationId,
                     boolean muted, boolean skipped) {
    public Step {
      Objects.requireNonNull(id); Objects.requireNonNull(animationId);
      Objects.requireNonNull(effectiveAnimationId);
    }
  }
  /** One participant's ordered stages. Role IDs: single, host, guest, giver, receiver. */
  public record Role(String id, List<Step> steps) {
    public Role { Objects.requireNonNull(id); steps=List.copyOf(steps); }
  }
  /**
   * Related slots are additional poses used by the same activity (such as approach/spacing).
   * available means the registered set enables this activity, not that terrain/partners are ready.
   * Lists are immutable snapshots. participantCount includes the initiating character.
   */
  public record Activity(String id, List<Role> roles, List<Step> related,
                         int participantCount, boolean available) {
    public Activity {
      Objects.requireNonNull(id); roles=List.copyOf(roles); related=List.copyOf(related);
    }
  }

  /**
   * Returns all linked activities using the selected animation, including unavailable activities.
   * registered contains logical template IDs (Idle is accepted as the Stand template's alias).
   * Unknown animation IDs return an empty list. Missing optional stages use the same donor
   * resolver as generated scenarios. Required Idle is the final donor even for incomplete drafts.
   */
  public static List<Activity> forAnimation(String animationId, Set<String> registered) {
    Objects.requireNonNull(animationId); Objects.requireNonNull(registered);
    String selected=animationId.equals("Stand")?"Idle":animationId;
    return all(registered).stream().filter(a -> a.roles().stream().flatMap(r->r.steps().stream())
        .anyMatch(s->s.animationId().equals(selected)) || a.related().stream()
        .anyMatch(s->s.animationId().equals(selected))).toList();
  }

  static List<Activity> all(Set<String> registered) { return new Catalog(registered).activities(); }

  private static final class Catalog {
    final Set<String> registered;
    final Map<String,String> owners;
    Catalog(Set<String> values) {
      var copy=new HashSet<>(values);
      if(copy.remove("Stand"))copy.add("Idle");
      registered=Set.copyOf(copy); owners=ScenarioVisuals.owners(registered);
    }
    boolean any(String... ids) { return Arrays.stream(ids).anyMatch(registered::contains); }
    Step nativeStep(String id,String animation) {
      String donor=owners.getOrDefault(id,"Idle");
      return new Step(id,animation,donor,donor.equals("Idle")&&!id.equals("Stand"),false);
    }
    Step repeat(String id,String animation,int occurrence) {
      Step stage=nativeStep(id,animation);
      return new Step(id+"-"+occurrence,animation,stage.effectiveAnimationId(),stage.muted(),false);
    }
    Step direct(String id) { return new Step(id,id,registered.contains(id)?id:"Idle",false,false); }
    Step skip(String id) {
      boolean missing=!registered.contains(id);
      return new Step(id,id,missing?"":id,missing,missing);
    }
    Role role(String id,Step... steps) { return new Role(id,List.of(steps)); }
    Activity activity(String id,boolean available,int participants,List<Role> roles,Step... related) {
      return new Activity(id,roles,List.of(related),participants,available);
    }
    List<Activity> activities() {
      List<Activity> out=new ArrayList<>();
      // This activity waits for a real desktop drag; it never synthesizes pointer movement.
      out.add(activity("held",any("Pinched"),1,List.of(role("single",direct("Pinched")))));
      out.add(activity("jump",any("Jump"),1,List.of(role("single",direct("Jump"))),direct("Land")));
      out.add(activity("landing",true,1,List.of(role("single",direct("Falling"),direct("Land")))));
      out.add(activity("hard-landing",any("HardLand","Stunned","GetUpHop"),1,
          List.of(role("single",direct("Falling"),skip("HardLand"),skip("Stunned"),skip("GetUpHop")))));
      out.add(activity("bouncing",any("Bouncing"),1,List.of(role("single",direct("Falling"),
          direct("Bouncing"),new Step("Bouncing-2","Bouncing",registered.contains("Bouncing")?"Bouncing":"Idle",false,false)))));
      out.add(activity("sprint",any("Dash","SprintBrake"),1,List.of(role("single",
          nativeStep("Dash","Dash"),nativeStep("SprintBrake","SprintBrake"))),nativeStep("Run","Run")));
      out.add(activity("face-glide",any("FaceGlide"),1,List.of(role("single",
          nativeStep("JumpGrabFace","Jump"),nativeStep("FaceGlideUp","FaceGlide"),
          nativeStep("HopOntoTop","Jump"))),nativeStep("Walk","Walk")));
      out.add(activity("parade",any("Parade","ParadeBump"),3,List.of(role("host",
          nativeStep("LeadParade","Parade"),nativeStep("ParadeBump","ParadeBump"),nativeStep("HappyHop","HappyHop")),
          role("guest",nativeStep("JoinParade","Parade"),nativeStep("ParadeBump","ParadeBump"),nativeStep("HappyHop","HappyHop")))));
      out.add(activity("high-five",any("HiFive","HiFivePrepare"),2,List.of(role("host",
          nativeStep("HiFiveCrouch","HiFivePrepare"),nativeStep("HiFiveJumpHost","HiFive")),
          role("guest",nativeStep("HiFiveCrouch","HiFivePrepare"),nativeStep("HiFiveJumpGuest","HiFive"))),
          nativeStep("HiFiveApproach","Walk"),nativeStep("Backstep","Backstep")));
      out.add(activity("chat",any("Talk","Humming","Backstep"),2,List.of(role("host",
          nativeStep("Talk","Talk"),nativeStep("TiltHead","TiltHead"),repeat("Talk","Talk",2),
          nativeStep("Humming","Humming"),repeat("Talk","Talk",3),repeat("TiltHead","TiltHead",2),repeat("Humming","Humming",2)),
          role("guest",nativeStep("TiltHead","TiltHead"),nativeStep("Talk","Talk"),nativeStep("Humming","Humming"),
          repeat("Talk","Talk",2),repeat("TiltHead","TiltHead",2),repeat("Talk","Talk",3),repeat("Humming","Humming",2))),
          nativeStep("ChatApproach","Walk"),nativeStep("Backstep","Backstep")));
      out.add(activity("gather",any("GatherCall","GatherSit"),3,List.of(role("host",
          nativeStep("GatherCall","GatherCall"),nativeStep("GatherCallAgain","GatherCall")),role("guest",
          nativeStep("GatherJoin","Walk"),nativeStep("GatherSitStay","GatherSit"))),nativeStep("Backstep","Backstep")));
      out.add(activity("tag",any("Tag"),2,List.of(role("host",nativeStep("TagDashLeg","Tag"),
          nativeStep("TagCaught","Stunned")),role("guest",nativeStep("TagChase","Tag"),nativeStep("HappyHop","HappyHop")))));
      out.add(activity("gift",any("GiftHold","GiftThrow","GiftAwait","GiftCatch"),2,List.of(role("giver",
          nativeStep("GiftHold","GiftHold"),nativeStep("GiftPose","GiftThrow"),nativeStep("TiltHead","TiltHead")),
          role("receiver",nativeStep("GiftAwait","GiftAwait"),nativeStep("CreditGetHop1","GiftCatch"),nativeStep("HappyHop","HappyHop"))),
          nativeStep("GiftApproach","Walk"),nativeStep("Backstep","Backstep")));
      out.add(activity("head-pat",any("HeadPat"),1,List.of(role("single",direct("HeadPat"),direct("HappyHop")))));
      return List.copyOf(out);
    }
  }
}
