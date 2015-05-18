<%@ page import="com.lightpegasus.scheduler.web.RequestContext" %>
<%@ page import="com.lightpegasus.scheduler.web.paths.PathBuilder" %>

<!-- header -->
<%
    RequestContext context = (RequestContext) request.getAttribute("requestContext");
    PathBuilder pathBuilder = context.getPathBuilder();
    int year = pathBuilder.getYear();
%>
<div class="navbar navbar-default navbar-fixed-top" role="navigation">
  <div class="container-fluid">
    <div class="navbar-header">
      <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
        <span class="sr-only">Toggle navigation</span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
      </button>
      <a class="navbar-brand" href="/">Gen Con <%= year %></a>
    </div>
    <div class="navbar-collapse collapse">
      <ul class="nav navbar-nav">
        <li>
        <%
          if (context.getUser().isPresent()) {
        %>
          <a href="#" class="dropdown-toggle" data-toggle="dropdown">
            <%= context.getUser().get().getNickname() %>'s Account<b class="caret"></b></a>
          <ul class="dropdown-menu">
            <li><a href="<%= pathBuilder.sitePath("starred") %>">Starred Events</a></li>
            <!--<li><a href="<%= pathBuilder.sitePath("prefs") %>">Preferences</a></li>-->
            <!-- TODO: party -->
            <li class="divider"></li>
            <li><a href="<%= context.getLogoutUrl() %>">Sign out</a></li>
          </ul>
        <%
          } else {
        %>
          <a href="<%= context.getLoginUrl() %>">Sign in</a>
        <%
          } // context.getUser().isPresent()
        %>
        </li>
        <li><a href="<%= pathBuilder.sitePath("categories") %>">Browse Categories</a></li>
      </ul>
      <form class="navbar-form navbar-right" action="/search">
        <input type="text" class="form-control" style="min-width: 300px;" placeholder="Search..." name="q"/>
      </form>
    </div><!--/.nav-collapse -->
  </div>
</div>
<!-- end header -->
