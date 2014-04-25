package com.lightpegasus.scheduler.gencon.entity;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.Stringify;
import com.googlecode.objectify.stringifier.Stringifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 */
@Entity
public class User {
  @Id private String googleUserId;
  private String email;
  private String nickname;

  @Load private Set<Ref<GenconEvent>> starredEvents;
  @Stringify(PreferenceStringifier.class)
  private Map<Preference, Boolean> preferences = new HashMap<>();

  public enum Preference {
    RECEIVE_EMAILS(true),
    EMAIL_ON_EVENT_CHANGE(true);
    private final boolean defaultValue;

    Preference(boolean defaultValue) {
      this.defaultValue = defaultValue;
    }
  }

  public static class PreferenceStringifier implements Stringifier<Preference> {
    @Override
    public String toString(Preference obj) {
      return obj.name();
    }

    @Override
    public Preference fromString(String str) {
      return Preference.valueOf(str);
    }
  }

  public User() {

  }

  public User(String googleUserId, String email, String nickname) {
    this.googleUserId = googleUserId;
    this.email = email;
    this.nickname = nickname;
  }

  public void setPreference(Preference preference, boolean enabled) {
    preferences.put(preference, enabled);
  }

  public boolean getWantsEmail() {
    return getPreference(Preference.RECEIVE_EMAILS);
  }

  public boolean getEmailOnChange() {
    return getPreference(Preference.EMAIL_ON_EVENT_CHANGE);
  }

  private boolean getPreference(Preference preference) {
    Preconditions.checkNotNull(preferences);
    if (!preferences.containsKey(preference)) {
      preferences.put(preference, preference.defaultValue);
    }

    return preferences.get(preference);
  }

  public String getNickname() {
    return nickname;
  }

  /**
   * Returns true if the event is now starred.
   */
  public boolean toggleEventStar(GenconEvent toggledEvent) {
    Ref<GenconEvent> genconEventRef = Ref.create(toggledEvent);

    if (!starredEvents.remove(genconEventRef)) {
      starredEvents.add(genconEventRef);
      return true;
    }
    return false;
  }
}
