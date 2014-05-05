package com.lightpegasus.scheduler.web.controllers;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.scheduler.web.RequestHelpers;
import com.lightpegasus.scheduler.web.ThymeleafController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import java.io.PrintWriter;

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

    // TODO(alek): restrict to post only
    if (!isPost) {
      return;
    }

    Multimap<String, String> params =
        RequestHelpers.parameterMultimap(context.getHttpServletRequest());

    String eventId = Iterables.getOnlyElement(params.get("eventId"));

    GenconEvent event =
        ofy().load().type(GenconEvent.class).id(GenconEvent.idForYear(genconYear, eventId)).safe();
    boolean isStarred = loggedInUser.get().toggleEventStar(event);

    ofy().save().entities(loggedInUser.get()).now();

    context.setVariable("isStarred", isStarred);

    PrintWriter writer = context.getHttpServletResponse().getWriter();
    writer.write("{ \"starred\": " + isStarred + ", \"eventId\": \"" + eventId + "\" }");
    writer.flush();
  }

  @Override
  protected boolean requiresLogin() {
    return true;
  }
}
