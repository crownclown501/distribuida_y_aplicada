package com.pda.practica2.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/peers")
public class PeerResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPeers() {
        // Lógica para obtener la lista de peers
        String jsonResponse = "{\"peers\": [\"Peer1\", \"Peer2\"]}";
        return Response.ok(jsonResponse).build();
    }
}
