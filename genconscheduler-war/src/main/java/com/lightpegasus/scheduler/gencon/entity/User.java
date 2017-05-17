package com.lightpegasus.scheduler.gencon.entity;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.Stringify;
import com.googlecode.objectify.stringifier.Stringifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
@Entity
@Cache
public class User {
  @Id private String googleUserId;

  // Index for groups.
  @Index private String email;
  private String nickname;

  @Load private Set<Ref<GenconEvent>> starredEvents = new HashSet<>();
  @Stringify(PreferenceStringifier.class)
  private Map<Preference, Boolean> preferences = new HashMap<>();

  @Ignore private boolean isAdmin;

  public List<GenconEvent> getStarredEvents() {
    return ImmutableList.copyOf(Iterables.transform(starredEvents,
        new Function<Ref<GenconEvent>, GenconEvent>() {
          @Override public GenconEvent apply(Ref<GenconEvent> input) {
            return input.get();
          }
        }));
  }

  public List<GenconEvent> getStarredEvents(final int year) {
    return FluentIterable.from(getStarredEvents())
        .filter(new Predicate<GenconEvent>() {
          @Override
          public boolean apply(GenconEvent genconEvent) {
            return genconEvent.getYear() == year;
          }
        }).toList();
  }

  public boolean isAdmin() {
    return isAdmin;
  }

  public void setAdmin(boolean isAdmin) {
    this.isAdmin = isAdmin;
  }

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

  public boolean starEvent(boolean starOn, GenconEvent event) {
    Ref<GenconEvent> genconEventRef = Ref.create(event);
    if (starOn) {
      starredEvents.add(genconEventRef);
    } else {
      starredEvents.remove(genconEventRef);
    }

    return starOn;
  }

  public boolean isEventStarred(GenconEvent event) {
    Ref<GenconEvent> genconEventRef = Ref.create(event);
    return starredEvents.contains(genconEventRef);
  }
}
