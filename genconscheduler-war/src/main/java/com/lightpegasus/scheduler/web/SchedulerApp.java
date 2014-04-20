package com.lightpegasus.scheduler.web;

import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.impl.translate.opt.BigDecimalLongTranslatorFactory;
import com.googlecode.objectify.impl.translate.opt.joda.JodaTimeTranslators;
import com.lightpegasus.objectify.DurationLongValueTranslatorFactory;
import com.lightpegasus.scheduler.gencon.entity.GenconCategory;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
import com.lightpegasus.scheduler.gencon.entity.SearchQuery;
import com.lightpegasus.scheduler.web.controllers.CategoryListController;
import com.lightpegasus.scheduler.web.controllers.EventDetailsController;
import com.lightpegasus.scheduler.web.controllers.SearchController;
import com.lightpegasus.scheduler.web.controllers.StaticTemplateController;
import com.lightpegasus.thymeleaf.ThymeleafController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Handles static initialization of various things needed for the scheduler and
 * general config of the app.
 */
public class SchedulerApp {
  private static Logger log = Logger.getLogger(SchedulerApp.class.getSimpleName());

  static {
    log.info("Initializing Objectify.");
    initializeObjectify();
    log.info("Objectify initialized, moving on to Thymeleaf.");
    initializeThymeleafEngine();
    log.info("Thymeleaf initialized.");
  }

  private static class RequestPathController {
    private Pattern requestPathPattern;
    private ThymeleafController controller;

    public RequestPathController(String pattern, ThymeleafController controller) {
      this.requestPathPattern = Pattern.compile(pattern);
      this.controller = controller;
    }

    public boolean matches(String requestPath) {
      return requestPathPattern.matcher(requestPath).matches();
    }

    public ThymeleafController getController() {
      return controller;
    }
  }
  private static List<RequestPathController> controllers;
  private static TemplateEngine templateEngine;

  public static TemplateEngine getTemplateEngine() {
    return templateEngine;
  }

  public static ThymeleafController resolveControllerForRequest(final HttpServletRequest request) {
    final String path = getRequestPath(request);

    for (RequestPathController controller : controllers) {
      if (controller.matches(path)) {
        return controller.getController();
      }
    }

    return null;
  }

  private static String getRequestPath(final HttpServletRequest request) {
    String requestURI = request.getRequestURI();
    final String contextPath = request.getContextPath();

    final int fragmentIndex = requestURI.indexOf(';');
    if (fragmentIndex != -1) {
      requestURI = requestURI.substring(0, fragmentIndex);
    }

    if (requestURI.startsWith(contextPath)) {
      return requestURI.substring(contextPath.length());
    }
    return requestURI;
  }

  private static void initializeThymeleafEngine() {
    // Setup the controllers to dispatch requests to the appropriate controller.
    controllers = ImmutableList.<RequestPathController>builder()
        .add(new RequestPathController("/browse/categories", new CategoryListController()))
        .add(new RequestPathController("/browse/gameSystems", new CategoryListController()))
        .add(new RequestPathController("/eventDetails", new EventDetailsController()))
        .add(new RequestPathController("/about", new StaticTemplateController("about")))
        .add(new RequestPathController("/fullSearch", new StaticTemplateController("fullSearch")))
        .add(new RequestPathController("/search", new SearchController()))
        // TODO(alek): Make a better default page which links to by category / by rule system etc
        .add(new RequestPathController("/", new CategoryListController()))
        .build();

    // Now setup the template engine
    ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver();

    // This will convert "home" to "/WEB-INF/templates/home.html"
    templateResolver.setPrefix("/WEB-INF/templates/");
    templateResolver.setSuffix(".html");

    templateEngine = new TemplateEngine();
    templateEngine.setTemplateResolver(templateResolver);
  }

  private static void initializeObjectify() {
    // All translators need to be installed before the entities.
    ObjectifyService.factory().getTranslators().add(new BigDecimalLongTranslatorFactory());
    ObjectifyService.factory().getTranslators().add(
        new DurationLongValueTranslatorFactory());
    JodaTimeTranslators.add(ObjectifyService.factory());

    // Now register entities.
    ObjectifyService.register(GenconEvent.class);
    ObjectifyService.register(GenconCategory.class);
    ObjectifyService.register(SearchQuery.class);

    log.info("Registered entities");
  }
}
