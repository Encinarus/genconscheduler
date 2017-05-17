<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.lightpegasus.scheduler.gencon.entity.GenconCategory" %>
<%@ page import="com.lightpegasus.scheduler.gencon.entity.GenconEventGroup" %>
<%@ page import="com.lightpegasus.scheduler.gencon.Queries" %>
<%@ page import="com.lightpegasus.scheduler.gencon.EventGroupPartition" %>
<%@ page import="com.lightpegasus.scheduler.gencon.EventOrganizer" %>
<%@ page import="com.lightpegasus.scheduler.web.RequestContext" %>
<%@ page import="com.lightpegasus.scheduler.web.paths.PathBuilder" %>
<%@ page import="com.google.common.base.Splitter" %>
<%@ page import="com.google.common.base.Strings" %>
<%@ page import="com.google.common.collect.Iterables" %>
<%@ page import="static com.google.common.html.HtmlEscapers.htmlEscaper" %>
<%@ page import="java.util.List" %>

<html>
<head>
  <link rel="stylesheet" href="//netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css"/>
  <link type="text/css" rel="stylesheet" href="/static/stylesheets/main.css"/>
  <title>Gencon Event Categories</title>
  <meta name="google-site-verification" content="a_VPdSYEO-t_BvYNh_sIhTYu76b12ltFT7kCOl-ujzE" />
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
</head>
<body>
<jsp:include page="/jsp/header.jsp" />

<!-- content -->
<%
    RequestContext context = (RequestContext) request.getAttribute("requestContext");
    PathBuilder pathBuilder = context.getPathBuilder();
    Queries queries = context.getQueries();
    int year = pathBuilder.getYear();

    List<String> splitUrl = Splitter.on("/")
        .omitEmptyStrings()
        .splitToList(pathBuilder.getLocalPath().getRemainder());
    String category = Iterables.getLast(splitUrl);

    List<GenconEventGroup> genconEventGroups =
        queries.loadEventGroupsForCategory(category, year);
    List<EventGroupPartition> eventGroupPartitions =
        EventOrganizer.partitionByRules(genconEventGroups);
%>

<div class="container">
  <h1 class="page-header">Category Listing <small><%= category %></small></h1>
  <% if ("BGM".equals(category)) { %>
  <div class="well well-sm">See BGG's list of
    <a href="https://boardgamegeek.com/geeklist/204632/my-gencon-2016-releases-watch">GenCon 2016 releases</a>
  </div>
  <% } %>

  <div class="row">
    <div class="main">
    <%
    for (EventGroupPartition partition : eventGroupPartitions) {
      String partitionName = htmlEscaper().escape(Strings.nullToEmpty(partition.getName()));
    %>
      <h4><%= partitionName %> - <%= partition.getEventGroups().size() %> events</h4>

      <div class="list-group col-md-12">
        <%
        for (GenconEventGroup event : partition.getEventGroups()) {
          String clusterId = htmlEscaper().escape(Strings.nullToEmpty(event.getClusterId()));
          String gameSystem = Strings.isNullOrEmpty(event.getGameSystem())
              ? "Unspecified"
              : htmlEscaper().escape(event.getGameSystem());
          String eventTitle = htmlEscaper().escape(Strings.nullToEmpty(event.getTitle()));
          String rulesEdition = htmlEscaper().escape(Strings.nullToEmpty(event.getRulesEdition()));
          String shortDescription = htmlEscaper().escape(Strings.nullToEmpty(event.getShortDescription()));
        %>
        <a href="<%= pathBuilder.sitePath("event") + clusterId%>"
           class="list-group-item"
           style="font-size: small">
          <h4>
            <% if (event.getSimilarEventCount() > 1) { %>
            <span class="badge"><%= event.getSimilarEventCount() %> sessions</span>
            <% } %>
            <%= eventTitle %>
            <span class="small"><%= gameSystem %> <%= rulesEdition %></span>
          </h4>
          <p><%= shortDescription %></p>
          <ul class="list-inline">
            <li><strong>Wed</strong> <%= event.getWedAvailable() %> tickets
              / <%= event.getWedOpenEvents() %> <%= event.getWedOpenEvents() != 1 ? "sessions" : "session" %>
            </li><li><strong>Thurs</strong> <%= event.getThursAvailable() %> tickets
              / <%= event.getThursOpenEvents() %> <%= event.getThursOpenEvents() != 1 ? "sessions" : "session" %>
            </li><li><strong>Fri</strong> <%= event.getFriAvailable() %> tickets
              / <%= event.getFriOpenEvents() %> <%= event.getFriOpenEvents() != 1 ? "sessions" : "session" %>
            </li><li><strong>Sat</strong> <%= event.getSatAvailable() %> tickets
              / <%= event.getSatOpenEvents() %> <%= event.getSatOpenEvents() != 1 ? "sessions" : "session" %>
            </li><li><strong>Sun</strong> <%= event.getSunAvailable() %> tickets
              / <%= event.getSunOpenEvents() %> <%= event.getSunOpenEvents() != 1 ? "sessions" : "session" %>
            </li>
          </ul>
        </a>
        <% } %>
      </div>
    <% }// for each partition in eventGroupPartitions %>
    </div> <!-- main -->
  </div> <!-- row -->
</div> <!-- container -->
<!-- end content -->


<!-- page footer -->
<jsp:include page="/jsp/footer.jsp" />
<!-- end footer -->

</body>
</html>
