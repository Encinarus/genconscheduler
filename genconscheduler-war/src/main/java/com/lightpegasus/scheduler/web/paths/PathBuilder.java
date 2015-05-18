package com.lightpegasus.scheduler.web.paths;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import java.util.List;

/**
 * Utility class for the templates to reference, to generate site links.
 */
public class PathBuilder {
  private final LocalPath localPath;
  private final int year;

  public PathBuilder(String path) {
    this.localPath = parseLocalPath(path, PlannerPaths.DEFAULT_YEAR);
    this.year = localPath.year;
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

  private LocalPath parseLocalPath(String path, int defaultYear) {
    List<String> splitPath = Splitter.on("/").omitEmptyStrings().splitToList(path);

    if (splitPath.isEmpty()) {
      return new LocalPath(defaultYear, "", null);
    }

    int foundYear = defaultYear;
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

  public LocalPath getLocalPath() {
    return localPath;
  }

  public int getYear() {
    return year;
  }
}
