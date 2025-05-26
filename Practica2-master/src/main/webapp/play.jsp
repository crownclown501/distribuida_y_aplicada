<%@ page import="java.io.*, java.nio.file.*" %>
<%
    String fileName = request.getParameter("fileName");
    if (fileName != null) {
        File file = new File("storage/" + fileName);
        if (file.exists()) {
            response.setContentType("audio/mpeg");
            response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
            Files.copy(file.toPath(), response.getOutputStream());
            response.getOutputStream().flush();
        }
    }
%>
