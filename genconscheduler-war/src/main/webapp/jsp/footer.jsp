<%@ page import="com.lightpegasus.scheduler.web.RequestContext" %>
<%
    RequestContext context = (RequestContext) request.getAttribute("requestContext");
%>

<div class="container">
  <center><a href="/about">About</a>
        - Last updated at <%= context.getSyncStatus().getReadableSyncTime() %>
        - Ticket availability likely not accurate</center>
</div>

<script>
(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
  })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

ga('create', 'UA-16179668-4', 'lightpegasus.com');
ga('send', 'pageview');
</script>
<script src="//code.jquery.com/jquery-1.11.0.min.js"></script>
<script src="//netdna.bootstrapcdn.com/bootstrap/3.1.1/js/bootstrap.min.js"></script>
