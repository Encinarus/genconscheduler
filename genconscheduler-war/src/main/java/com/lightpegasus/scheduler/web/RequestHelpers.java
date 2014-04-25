package com.lightpegasus.scheduler.web;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RequestHelpers {
  public static String asDebugString(HttpServletRequest req) {
    Multimap<String, String> parameters = parameterMultimap(req);
    List<String> cookieDebugStrings = new ArrayList<>();
    if (req.getCookies() != null) {
      for (Cookie cookie : req.getCookies()) {
        cookieDebugStrings.add(cookieDebugString(cookie));
      }
    }
    return Joiner.on(" ").useForNull("n/a").join(
        "RequestUrl:", req.getRequestURL(),
        "QueryString:", req.getQueryString(),
        "Cookies:", Joiner.on(", ").join(cookieDebugStrings),
        "HeaderNames:", Objects.toStringHelper("Headers")
            .addValue(ImmutableList.copyOf(Iterators.forEnumeration(req.getHeaderNames()))),
        "PathInfo:", req.getPathInfo(),
        "Method:", req.getMethod(),
        "LocalAddress:", req.getLocalAddr(),
        "Parameters:", parameters);
  }

  public static String cookieDebugString(Cookie cookie) {
    return Objects.toStringHelper(cookie)
        .add("name", cookie.getName())
        .add("domain", cookie.getDomain())
        .add("maxAge", cookie.getMaxAge())
        .add("path", cookie.getPath())
        .add("value", cookie.getValue())
        .add("comment", cookie.getComment())
        .add("secure", cookie.getSecure())
        .add("version", cookie.getVersion())
        .toString();
  }

  public static Multimap<String, String> parameterMultimap(HttpServletRequest req) {
    Multimap<String, String> parameters = HashMultimap.create();
    for (Map.Entry<String, String[]> entry
        : ((Map<String, String[]>)req.getParameterMap()).entrySet()) {
      parameters.putAll(entry.getKey(), Arrays.asList(entry.getValue()));
    }
    return parameters;
  }
}
