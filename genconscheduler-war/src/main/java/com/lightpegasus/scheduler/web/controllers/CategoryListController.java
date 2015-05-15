package com.lightpegasus.scheduler.web.controllers;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.lightpegasus.scheduler.gencon.entity.GenconCategory;
import com.lightpegasus.scheduler.gencon.Queries;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.scheduler.web.SchedulerApp;
import com.lightpegasus.scheduler.web.ThymeleafController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import java.util.List;

/**
 * Controller for displaying a list of categories.
 */
public class CategoryListController extends ThymeleafController {
  @Override
  public void doProcess(SchedulerApp.PathBuilder pathBuilder, WebContext context, TemplateEngine engine, Optional<User> loggedInUser,
                        int genconYear) throws Exception {
    Queries queries = new Queries();

    List<GenconCategory> categories = queries.allCategories(genconYear);
    int rowWidth = 2;
    // List of lists! Woo!!! It's what we have to do to partition. It may be better to not
    // partition, and instead let CSS handle laying out the list for us.
    ImmutableList<List<GenconCategory>> rows = ImmutableList.copyOf(
        Iterables.partition(categories, rowWidth));

    context.setVariable("rows", rows);
    engine.process("allCategories", context, context.getHttpServletResponse().getWriter());
  }
}
