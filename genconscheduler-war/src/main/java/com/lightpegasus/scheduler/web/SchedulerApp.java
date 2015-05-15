package com.lightpegasus.scheduler.web;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.impl.translate.opt.BigDecimalLongTranslatorFactory;
import com.googlecode.objectify.impl.translate.opt.joda.JodaTimeTranslators;
import com.lightpegasus.objectify.DurationLongValueTranslatorFactory;
import com.lightpegasus.scheduler.gencon.entity.BackgroundTaskStatus;
import com.lightpegasus.scheduler.gencon.entity.GenconCategory;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
import com.lightpegasus.scheduler.gencon.entity.GenconEventGroup;
import com.lightpegasus.scheduler.gencon.entity.SearchQuery;
import com.lightpegasus.scheduler.gencon.entity.UpdateHistory;
import com.lightpegasus.scheduler.gencon.entity.User;
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
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Handles static initialization of various things needed for the scheduler and
 * general config of the app.
 */
public class SchedulerApp {
  /**
   * LocalPath handles creating urls for paths with parameters. All scheduler urls
   * will be of the form / or /year/basePath/otherStuff where year indicates the
   * gencon year being displayed, basePath determines which controller handles the request
   * and otherStuff is specific to the particular handler.
   *
   * Immutable.
   */
  public static class LocalPath {
    final int year;
    final String handler;
    final String remainder;
    final ImmutableMultimap<String, String> params;

    public LocalPath(int year, String handler, String remaining) {
      this(year, handler, remaining, ImmutableMultimap.<String, String>of());
    }

    private LocalPath(int year, String handler, String remainder, Multimap<String, String> params) {
      Preconditions.checkArgument(Range.openClosed(2013, 2015).contains(year),
          "Unsupported year %s", year);
      handler = Strings.nullToEmpty(handler).trim();
      remainder = Strings.nullToEmpty(remainder).trim();

      Preconditions.checkArgument(!handler.contains("/"),
          "Handler can't contain a /, found %s", handler);
      Preconditions.checkArgument(!handler.isEmpty() || remainder.isEmpty(),
          "Handler can't be empty with a remainder, remainder: %s", remainder);
      this.year = year;
      this.handler = handler;
      this.remainder = Strings.nullToEmpty(remainder);
      this.params = ImmutableMultimap.copyOf(params);
    }

    public LocalPath withParams(Multimap<String, String> params) {
      return new LocalPath(year, handler, remainder, params);
    }

    public String asUrl() {
      StringBuilder builder = new StringBuilder("/" + year + "/" + handler);
      if (!handler.isEmpty()) {
        builder.append("/" + remainder);
      }
      if (!params.isEmpty()) {
        builder.append("?");
      }
      Escaper paramEscaper = UrlEscapers.urlFormParameterEscaper();
      for (Map.Entry<String, String> param : params.entries()) {
        String paramKey = paramEscaper.escape(param.getKey());
        String paramValue = paramEscaper.escape(param.getValue());
        builder.append(paramKey).append("=").append(paramValue);
      }
      return builder.toString();
    }
  }

  /**
   * Utility class for the templates to reference, to generate site links.
   */
  public static class PathBuilder {
    private int year;

    public PathBuilder(int defaultYear) {
      this.year = defaultYear;
    }

    public void setYear(int parsedYear) {
      this.year = parsedYear;
    }

    public String sitePath(String path) {
      path = Strings.nullToEmpty(path);

      List<String> pathSegments = Splitter.on("/").omitEmptyStrings().splitToList(path);
      String handler = "";
      if (!pathSegments.isEmpty()) {
        handler = pathSegments.get(0);
        pathSegments = pathSegments.subList(1, pathSegments.size());
      }
      String remainder = Joiner.on("/").join(pathSegments);

      return new LocalPath(year, handler, remainder).asUrl();
    }

    public LocalPath parseUrl(final String path) {
      List<String> splitPath = Splitter.on("/").omitEmptyStrings().splitToList(path);

      if (splitPath.isEmpty()) {
        return new LocalPath(year, "", null);
      }

      int foundYear = year;
      try {
        foundYear = Integer.parseInt(splitPath.get(0));
        splitPath = splitPath.subList(1, splitPath.size());
      } catch (NumberFormatException e) {
        // This is okay, we use the defaultYear instead, helps for things like /about
      }

      if (splitPath.isEmpty()) {
        // No handler, we had a path like "/"
        return new LocalPath(foundYear, "", null);
      }

      return new LocalPath(foundYear, splitPath.get(0), Joiner.on("/").join(
          splitPath.subList(1, splitPath.size())));
    }
  }

  private static Logger log = Logger.getLogger(SchedulerApp.class.getSimpleName());

  static {
    log.info("Initializing Objectify.");
    initializeObjectify();
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
    LocalPath path = new PathBuilder(2014).parseUrl(getRequestPath(request));

    return controllers.get(path.handler);
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

  private static void initializeObjectify() {
    // All translators need to be installed before the entities.
    ObjectifyService.factory().getTranslators().add(new BigDecimalLongTranslatorFactory());
    ObjectifyService.factory().getTranslators().add(
        new DurationLongValueTranslatorFactory());
    JodaTimeTranslators.add(ObjectifyService.factory());

    // Now register entities.
    ObjectifyService.register(GenconEvent.class);
    ObjectifyService.register(GenconEventGroup.class);
    ObjectifyService.register(GenconCategory.class);
    ObjectifyService.register(SearchQuery.class);
    ObjectifyService.register(BackgroundTaskStatus.class);
    ObjectifyService.register(User.class);
    ObjectifyService.register(UpdateHistory.class);

    log.info("Registered entities");
  }
}
