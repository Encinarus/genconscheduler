package com.lightpegasus.scheduler.web;

import com.google.common.collect.ImmutableMap;
import com.lightpegasus.scheduler.gencon.Queries;
import com.lightpegasus.scheduler.web.controllers.AdvancedSearchController;
import com.lightpegasus.scheduler.web.controllers.CategoryDetailsController;
import com.lightpegasus.scheduler.web.controllers.CategoryListController;
import com.lightpegasus.scheduler.web.controllers.DeleteGenconYearController;
import com.lightpegasus.scheduler.web.controllers.EventDetailsController;
import com.lightpegasus.scheduler.web.controllers.EventParserController;
import com.lightpegasus.scheduler.web.controllers.NewCategoryDetailsController;
import com.lightpegasus.scheduler.web.controllers.SearchController;
import com.lightpegasus.scheduler.web.controllers.StarController;
import com.lightpegasus.scheduler.web.controllers.StaticTemplateController;
import com.lightpegasus.scheduler.web.controllers.UserPreferencesController;
import com.lightpegasus.scheduler.web.controllers.UserStarredController;
import com.lightpegasus.scheduler.web.paths.LocalPath;
import com.lightpegasus.scheduler.web.paths.PathBuilder;
import com.lightpegasus.scheduler.web.paths.PathUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Handles static initialization of various things needed for the scheduler and
 * general config of the app.
 */
public class SchedulerApp {

  private static Logger log = Logger.getLogger(SchedulerApp.class.getSimpleName());

  static {
    log.info("Initializing Objectify.");
    Queries.initializeObjectify();
    log.info("Objectify initialized, moving on to Thymeleaf.");
    initializeThymeleafEngine();
    log.info("Thymeleaf initialized.");
  }

  private static Map<String, ThymeleafController> controllers;
  private static TemplateEngine templateEngine;

  public static TemplateEngine getTemplateEngine() {
    return templateEngine;
  }

  public static ThymeleafController resolveControllerForRequest(final HttpServletRequest request) {
    LocalPath path = new PathBuilder(PathUtils.getRequestPath(request)).getLocalPath();

    return controllers.get(path.getHandler());
  }

  private static void initializeThymeleafEngine() {
    // Setup the controllers to dispatch requests to the appropriate controller.
    controllers = ImmutableMap.<String, ThymeleafController>builder()
        .put("categories", new CategoryListController())
        .put("event", new EventDetailsController())
        .put("oldCategory", new CategoryDetailsController())
        .put("category", new NewCategoryDetailsController())
        .put("about", new StaticTemplateController("about"))
        .put("search", new SearchController())
        .put("advancedSearch", new AdvancedSearchController())
        .put("prefs", new UserPreferencesController())
        .put("star", new StarController())
        .put("starred", new UserStarredController())
        // TODO(alek): Consolidate the two below controllers into an admin controller, which then
        // routes on it's own
        .put("deleteEvents", new DeleteGenconYearController())
        .put("parseEvents", new EventParserController())
        .put("", new CategoryListController())
        .build();

    // Now setup the template engine
    ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver();

    // This will convert "home" to "/WEB-INF/templates/home.html"
    templateResolver.setPrefix("/WEB-INF/templates/");
    templateResolver.setSuffix(".html");

    templateEngine = new TemplateEngine();

    templateEngine.setTemplateResolver(templateResolver);
  }

}
