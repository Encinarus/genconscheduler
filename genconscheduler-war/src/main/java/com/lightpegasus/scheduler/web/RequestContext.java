package com.lightpegasus.scheduler.web;

import com.google.common.base.Optional;
import com.lightpegasus.scheduler.gencon.Queries;
import com.lightpegasus.scheduler.gencon.entity.BackgroundTaskStatus;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.scheduler.web.paths.LocalPath;
import com.lightpegasus.scheduler.web.paths.PathBuilder;

/**
 * Created by alek on 5/17/15.
 */
public class RequestContext {
  private final PathBuilder pathBuilder;
  private final Queries queries;
  private final BackgroundTaskStatus syncStatus;
  private final Optional<User> loggedInUser;
  private final String loginUrl;
  private final String logoutUrl;

  public RequestContext(PathBuilder pathBuilder, Queries queries,
                        BackgroundTaskStatus syncStatus, Optional<User> loggedInUser,
                        String loginUrl, String logoutUrl) {
    this.pathBuilder = pathBuilder;
    this.queries = queries;
    this.syncStatus = syncStatus;
    this.loggedInUser = loggedInUser;
    this.loginUrl = loginUrl;
    this.logoutUrl = logoutUrl;
  }

  public PathBuilder getPathBuilder() {
    return pathBuilder;
  }

  public LocalPath getLocalPath() {
    return pathBuilder.getLocalPath();
  }

  public Queries getQueries() {
    return queries;
  }

  public BackgroundTaskStatus getSyncStatus() {
    return syncStatus;
  }

  public Optional<User> getUser() {
    return loggedInUser;
  }

  public String getLoginUrl() {
    return loginUrl;
  }

  public String getLogoutUrl() {
    return logoutUrl;
  }

  public static class Builder {
    private PathBuilder pathBuilder;
    private Queries queries;
    private BackgroundTaskStatus syncStatus;
    private Optional<User> loggedInUser;
    private String loginUrl;
    private String logoutUrl;

    public Builder setPathBuilder(PathBuilder pathBuilder) {
      this.pathBuilder = pathBuilder;
      return this;
    }

    public Builder setQueries(Queries queries) {
      this.queries = queries;
      return this;
    }

    public Builder setSyncStatus(BackgroundTaskStatus syncStatus) {
      this.syncStatus = syncStatus;
      return this;
    }

    public Builder setLoggedInUser(Optional<User> loggedInUser) {
      this.loggedInUser = loggedInUser;
      return this;
    }

    public Builder setLoginUrl(String loginUrl) {
      this.loginUrl = loginUrl;
      return this;
    }

    public Builder setLogoutUrl(String logoutUrl) {
      this.logoutUrl = logoutUrl;
      return this;
    }

    public RequestContext build() {
      return new RequestContext(pathBuilder, queries, syncStatus, loggedInUser, loginUrl, logoutUrl);
    }
  }
}
