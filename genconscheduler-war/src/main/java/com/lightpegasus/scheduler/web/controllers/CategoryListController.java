package com.lightpegasus.scheduler.web.controllers;

import com.google.common.collect.Iterables;
import com.lightpegasus.scheduler.gencon.entity.GenconCategory;
import com.lightpegasus.scheduler.gencon.entity.Queries;
import com.lightpegasus.thymeleaf.ThymeleafController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import java.util.List;

/**
 * Controller for displaying a list of categories.
 */
public class CategoryListController implements ThymeleafController {
  @Override
  public void process(WebContext context, TemplateEngine engine) throws Exception {
    Queries queries = new Queries();

    List<GenconCategory> categories = queries.allCategories();
    int rowWidth = 2;
    Iterable<List<GenconCategory>> rows = Iterables.partition(categories, rowWidth);

    context.setVariable("rows", rows);
    engine.process("categories", context, context.getHttpServletResponse().getWriter());
  }
}
