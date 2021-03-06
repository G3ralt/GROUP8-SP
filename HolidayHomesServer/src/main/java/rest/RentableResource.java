package rest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import customExceptions.DBException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import entity.Rentable;
import facades.FacadeFactory;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.core.Response;
import static rest.JSONConverter.*;

@Path("rentables")
public class RentableResource {

    private final FacadeFactory FF;

    public RentableResource() {
        FF = new FacadeFactory();
        FF.setRentableFacade();
    }

    @Path("/rentable")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRentableByName(String jsonString) {
        try {
            JsonObject json = new JsonParser().parse(jsonString).getAsJsonObject();
            String username = json.get("username").getAsString();
            String rentableName = json.get("rentableName").getAsString();

            Rentable rentable = FF.getRentableFacade().getRentableByName(rentableName, username); //Get the locations from Database.

            return Response.status(200).entity(getJSONfromObject(rentable)).build(); //Return the locations as JSON

        } catch (Exception e) {
            return Response.status(503).entity(getJSONfromObject(e.getMessage())).build(); //Service unavailable if something is wrong

        } finally {
            FF.close();
        }
    }
    
    @Path("/all")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllRentables(String jsonString) {
        try {
            JsonObject json = new JsonParser().parse(jsonString).getAsJsonObject();
            String username = json.get("username").getAsString();

            List<Rentable> rentables = FF.getRentableFacade().getAllRentables(username); //Get the locations from Database.

            return Response.status(200).entity(getJSONfromObject(rentables)).build(); //Return the locations as JSON

        } catch (Exception e) {
            return Response.status(503).entity(getJSONfromObject(e.getMessage())).build(); //Service unavailable if something is wrong

        } finally {
            FF.close();
        }

    }

    @Path("/create")
    @RolesAllowed("Admin")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createNewRentable(String jsonString) {
        try {
            Rentable rentable = getRentableFromJSON(jsonString);

            FF.getRentableFacade().createNewRentable(rentable);

            return Response.status(201).entity(getJSONfromObject("Rentable created!")).build();

        } catch (DBException e) {
            //When the Place name is already in use
            return Response.status(406).entity(getJSONfromObject(e.getMessage())).build();

        } catch (Exception e) {
            return Response.status(503).entity(getJSONfromObject(e.getMessage())).build();

        } finally {
            FF.close();
        }
    }

    @Path("/checkName/{rentableName}")
    @RolesAllowed("Admin")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkRentableName(@PathParam("rentableName") String rentableName) {
        try {
            boolean existing = FF.getRentableFacade().checkForRentableName(rentableName);

            return existing ? Response.status(409).build() : Response.status(202).build(); //If rentableName is used - 409, otherwise 202

        } catch (Exception e) {
            return Response.status(503).entity(getJSONfromObject(e.getMessage())).build(); //Service unavailable if something is wrong

        } finally {
            FF.close();
        }
    }

    @Path("/addRating")
    @RolesAllowed("User")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addNewRatingForRentable(String jsonString) {
        try {
            //Get the information from the request
            JsonObject json = new JsonParser().parse(jsonString).getAsJsonObject();
            String rentableName = json.get("rentableName").getAsString();
            String username = json.get("username").getAsString();
            int rating = json.get("rating").getAsInt();

            int userRating = FF.getRentableFacade().getUserRating(username, rentableName);

            String message;
            if (userRating == 0) { //If user hasn`t rated the rentable yet
                FF.getRentableFacade().addRatingForRentable(rentableName, rating, username);
                message = "created!";
            } else { //if user has rated update the rating
                FF.getRentableFacade().updateRatingForRentable(rentableName, rating, username);
                message = "updated!";
            }
            return Response.status(201).entity(getJSONfromObject("Rating " + message)).build();

        } catch (Exception e) {
            return Response.status(503).entity(getJSONfromObject(e.getMessage())).build();
        }
    }

}
