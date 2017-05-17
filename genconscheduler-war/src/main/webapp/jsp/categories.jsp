<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.lightpegasus.scheduler.gencon.entity.GenconCategory" %>
<%@ page import="com.lightpegasus.scheduler.web.RequestContext" %>
<%@ page import="com.lightpegasus.scheduler.web.paths.PathBuilder" %>
<%@ page import="com.google.common.collect.Iterables" %>
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

<%
    RequestContext context = (RequestContext) request.getAttribute("requestContext");
    PathBuilder pathBuilder = context.getPathBuilder();
    int year = pathBuilder.getYear();
%>
<!-- content -->
<div class="container">
  <%
    List<GenconCategory> categories = context.getQueries().allCategories(year);
    int rowWidth = 2;
    // List of lists! Woo!!! It's what we have to do to partition. It may be better to not
    // partition, and instead let CSS handle laying out the list for us.

    if (categories.isEmpty()) {
      %>
    <h1 class="page-header">Gen Con <%= year %> Events aren't available yet</h1>
      <%
    } else {
      %>
    <h1 class="page-header"><%= year %> Events by type</h1>
    <div class="row">
        <div class="col-sm-12 col-md-12 main">
      <%
      for (List<GenconCategory> row : Iterables.partition(categories, rowWidth)) {
        %>
        <div class="row">
        <% for (GenconCategory category : row) { %>
          <div class="col-md-6 col-sm-12" style="padding-bottom: 5px">
            <a class="btn btn-default text-left"
               href="<%= pathBuilder.sitePath("category") + category.getEventTypeAbbreviation() %>"
               role="button"
               style="width:100%; white-space:normal"><%= category.getCategoryName() %>
               <span class="badge"><%= category.getEventCount() %></span></a>
          </div><!--/* category */-->
        <% } // for category in rows %>
        </div><!--/* row */-->
        <%
      } // for row in categories
    }
  %>
</div>
<!-- end content -->

<!-- page footer -->
<jsp:include page="/jsp/footer.jsp" />
<!-- end footer -->

</body>
</html>
