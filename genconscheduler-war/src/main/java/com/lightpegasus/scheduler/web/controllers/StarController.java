package com.lightpegasus.scheduler.web.controllers;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.scheduler.web.RequestHelpers;
import com.lightpegasus.scheduler.web.ThymeleafController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * This one doesn't actually use a template. Because the payload is so simple (tiny bit of json)
 * it's constructed directly in the response.
 */
public class StarController extends ThymeleafController {
  @Override
  protected void doProcess(WebContext context, TemplateEngine engine, Optional<User> loggedInUser,
      int genconYear) throws Exception {
    boolean isPost = context.getHttpServletRequest().getMethod().equals("POST");

    User user = loggedInUser.get();
    if (isPost) {
      handlePost(context, genconYear, user);
    } else {
      handleGet(context, genconYear, user);
    }

    System.err.println("Saving the user");
    ofy().save().entities(user).now();
  }

  private void handleGet(WebContext context, int genconYear, User user) throws IOException {
    StringBuilder responseBuilder = new StringBuilder("{[");

    for (GenconEvent starredEvent : user.getStarredEvents()) {
      responseBuilder.append("\"").append(starredEvent.getGameId()).append("\", \n");
    }

    responseBuilder.append("}]");

    PrintWriter writer = context.getHttpServletResponse().getWriter();
    writer.write(responseBuilder.toString());
    writer.flush();
  }

  private void handlePost(WebContext context, int genconYear, User user) throws IOException {
    Multimap<String, String> params =
        RequestHelpers.parameterMultimap(context.getHttpServletRequest());
    Collection<String> eventIds = params.get("eventId[]");

    boolean starOn = Boolean.valueOf(Iterables.getFirst(params.get("starOn"), "true"));

    Collection<String> jsonLines = new ArrayList<>();
    for (GenconEvent event : loadAllForIds(genconYear, eventIds)) {
      jsonLines.add("\"" + event.getGameId() + "\": \"" + user.starEvent(starOn, event) + "\"");
    }

    PrintWriter writer = context.getHttpServletResponse().getWriter();
    writer.write("{" + Joiner.on(",\n").join(jsonLines) + "}");
    writer.flush();
  }

  private Collection<GenconEvent> loadAllForIds(int genconYear, Collection<String> eventIds) {
    Collection<String> actualIds = new ArrayList<>();
    for (String eventId : eventIds) {
      actualIds.add(GenconEvent.idForYear(genconYear, eventId));
    }
    return ofy().load().type(GenconEvent.class).ids(actualIds).values();
  }

  @Override
  protected boolean requiresLogin() {
    return true;
  }
}
