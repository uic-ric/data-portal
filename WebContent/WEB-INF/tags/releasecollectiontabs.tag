<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ tag language="java" pageEncoding="ISO-8859-1"%>
<%@ attribute name="active" required="false" %>
<%@ attribute name="releaseid" required="true" type="java.lang.Long" %>
<nav>
<ul>
<li<%= "rawdata".equals(active) ? " class='active'" : ""%>><a href="<%= request.getContextPath() %>/release/<%= releaseid %>/rawdata">Raw Data</a></li>
<li<%= "results".equals(active) ? " class='active'" : ""%>><a href="<%= request.getContextPath() %>/release/<%= releaseid %>/results">Results</a></li>
<li<%= "reports".equals(active) ? " class='active'" : ""%>><a href="<%= request.getContextPath() %>/release/<%= releaseid %>/reports">Reports</a></li>
<jsp:doBody/>
</ul>
</nav>