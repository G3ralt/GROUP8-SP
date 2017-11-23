package rest;

import com.google.gson.*;
import customExceptions.DBException;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import entity.Place;
import facades.FacadeFactory;
import java.util.List;
import static rest.JSONConverter.*;

@Path("places")
public class PlaceResource {

    private final FacadeFactory FF;

    public PlaceResource() {
        FF = new FacadeFactory();
        FF.setPlaceFacade();
    }
    
    @Path("/all")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllPlaces(String jsonString) {
        try {
            JsonObject json = new JsonParser().parse(jsonString).getAsJsonObject();
            String userName = json.get("username").getAsString();

            List<Place> locations = FF.getPlaceFacade().getAllPlaces(userName); //Get the locations from Database.

            return Response.status(200).entity(getJSONfromObject(locations)).build(); //Return the locations as JSON

        } catch (Exception e) {
            return Response.status(503).entity(getJSONfromObject(e.getMessage())).build(); //Service unavailable if something is wrong

        } finally {
            FF.close();
        }

    }

    @Path("/create")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createNewPlace(String jsonString) {
        try {
            Place place = getPlaceFromJSON(jsonString);

            FF.getPlaceFacade().createNewPlace(place);

            return Response.status(201).entity(getJSONfromObject("Location created!")).build();

        } catch (DBException e) {
            //When the Place name is already in use
            return Response.status(406).entity(getJSONfromObject(e.getMessage())).build();

        } catch (Exception e) {
            return Response.status(503).entity(getJSONfromObject(e.getMessage())).build();

        } finally {
            FF.close();
        }
    }
    
    @Path("/checkName/{placeName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkPlaceName(@PathParam("placeName") String placeName) {
        try {
            boolean existing = FF.getPlaceFacade().checkForPlaceName(placeName);
            
            return existing ? Response.status(409).build() : Response.status(202).build(); //If placeName is used - 409, otherwise 202

        } catch (Exception e) {
            return Response.status(503).entity(getJSONfromObject(e.getMessage())).build(); //Service unavailable if something is wrong

        } finally {
            FF.close();
        }
    }

    @Path("/addRating")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addNewRatingForPlace(String jsonString) {
        try {
            //Get the information from the request
            JsonObject json = new JsonParser().parse(jsonString).getAsJsonObject();
            String placeName = json.get("placeName").getAsString();
            String userName = json.get("username").getAsString();
            int rating = json.get("rating").getAsInt();

            FF.getPlaceFacade().addRatingForPlace(placeName, rating, userName);

            return Response.status(201).entity(getJSONfromObject("Rating added!")).build();

        } catch (Exception e) {
            return Response.status(503).entity(getJSONfromObject(e.getMessage())).build();
        }
    }
    
    @Path("/updateRating")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateNewRatingForPlace(String jsonString) {
        try {
            //Get the information from the request
            JsonObject json = new JsonParser().parse(jsonString).getAsJsonObject();
            String placeName = json.get("placeName").getAsString();
            String userName = json.get("username").getAsString();
            int rating = json.get("rating").getAsInt();

            FF.getPlaceFacade().updateRatingForPlace(placeName, rating, userName);

            return Response.status(201).entity(getJSONfromObject("Rating update!")).build();

        } catch (Exception e) {
            return Response.status(503).entity(getJSONfromObject(e.getMessage())).build();
        }
    }

}
