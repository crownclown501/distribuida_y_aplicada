<%@ page import="java.io.*, java.nio.file.*" %>
<%
    String fileName = request.getParameter("fileName");
    if (fileName != null) {
        File file = new File("storage/" + fileName);
        if (file.exists()) {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            Files.copy(file.toPath(), response.getOutputStream());
            response.getOutputStream().flush();
        } else {
            out.println("Archivo no encontrado.");
        }
    } else {
        out.println("Nombre de archivo no proporcionado.");
    }
%>
