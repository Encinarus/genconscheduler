package com.lightpegasus.scheduler.web.controllers;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.scheduler.web.RequestHelpers;
import com.lightpegasus.thymeleaf.ThymeleafController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import javax.servlet.http.HttpServletRequest;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 *
 */
public class StarController extends ThymeleafController {
  @Override
  protected void doProcess(WebContext context, TemplateEngine engine, Optional<User> loggedInUser) throws Exception {
    boolean isPost = context.getHttpServletRequest().getMethod().equals("POST");

    // TODO(alek): restrict to post only
//    if (!isPost) {
//      return;
//    }

    Multimap<String, String> params =
        RequestHelpers.parameterMultimap(context.getHttpServletRequest());

    String eventId = Iterables.getOnlyElement(params.get("eventId"));

    GenconEvent event =
        ofy().load().type(GenconEvent.class).id(GenconEvent.idForYear(2013, eventId)).safe();
    loggedInUser.get().toggleEventStar(event);
  }

  public boolean requiresLogin() {
    return true;
  }
}
