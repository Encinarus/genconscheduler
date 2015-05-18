package com.lightpegasus.scheduler.web.paths;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import com.lightpegasus.scheduler.web.SchedulerApp;

import java.util.Map;

/**
 * LocalPath handles creating urls for paths with parameters. All scheduler urls
 * will be of the form / or /year/basePath/otherStuff where year indicates the
 * gencon year being displayed, basePath determines which controller handles the request
 * and otherStuff is specific to the particular handler.
 *
 * Immutable.
 */
public class LocalPath {
  final int year;
  final String handler;
  final String remainder;
  final ImmutableMultimap<String, String> params;

  public int getYear() {
    return year;
  }

  public String getHandler() {
    return handler;
  }

  public String getRemainder() {
    return remainder;
  }

  public ImmutableMultimap<String, String> getParams() {
    return params;
  }

  public LocalPath(int year, String handler, String remaining) {
    this(year, handler, remaining, ImmutableMultimap.<String, String>of());
  }

  private LocalPath(int year, String handler, String remainder, Multimap<String, String> params) {
//    Preconditions.checkArgument(Range.closed(2013, 2016).contains(year),
//        "Unsupported year %s", year);
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
