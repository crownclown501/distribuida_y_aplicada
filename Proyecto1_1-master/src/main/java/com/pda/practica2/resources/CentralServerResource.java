package com.pda.practica2.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/central")
public class CentralServerResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCentralInfo() {
        // Lógica para obtener información del servidor central
        String jsonResponse = "{\"info\": \"Central Server Info\"}";
        return Response.ok(jsonResponse).build();
    }
}
