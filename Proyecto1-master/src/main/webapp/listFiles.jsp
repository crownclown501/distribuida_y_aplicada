<%@ page import="java.io.File" %>
<html>
<head>
    <title>Lista de Archivos</title>
</head>
<body>
    <h1>Archivos Disponibles</h1>
    <ul>
        <%
            File folder = new File(application.getRealPath("/storage"));
            File[] listOfFiles = folder.listFiles();

            if (listOfFiles != null) {
                for (File file : listOfFiles) {
                    if (file.isFile()) {
                        String fileName = file.getName();
        %>
                        <li>
                            <% if (fileName.endsWith(".mp3")) { %>
                                <audio controls>
                                    <source src="storage/<%= fileName %>" type="audio/mpeg">
                                    Tu navegador no soporta la reproducción de audio.
                                </audio>
                            <% } else { %>
                                <a href="download.jsp?fileName=<%= fileName %>"><%= fileName %></a>
                            <% } %>
                        </li>
        <%
                    }
                }
            }
        %>
    </ul>
</body>
</html>
