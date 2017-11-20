package rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import entity.Place;
import facades.FacadeFactory;

@Path("places")
public class PlaceRest {

    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final FacadeFactory FF;

    public PlaceRest() {
        FF = new FacadeFactory();
        FF.setPlaceFacade();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getAll() {
        return gson.toJson(FF.getPlaceFacade().getAllPlaces());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String registerPlace(String place) {
        entity.Place pl = null;
        try {
            JsonObject json = new JsonParser().parse(place).getAsJsonObject();

            String city = json.get("city").getAsString();
            int zip = Integer.parseInt(json.get("zip").getAsString());
            String street = json.get("street").getAsString();
            String gpsLocation = json.get("gpsLocation").getAsString();
            String description = json.get("description").getAsString();
            int rating = Integer.parseInt(json.get("rating").getAsString());
            String imgUri = json.get("imgUri").getAsString();
            Place newPlace = new Place(city, zip, street, gpsLocation, description, rating, imgUri);

            pl = FF.getPlaceFacade().registerPlace(newPlace);
        } catch (JsonSyntaxException | NumberFormatException ex) {
            Logger.getLogger(PlaceRest.class.getName()).log(Level.SEVERE, null, ex);
        }   
        return gson.toJson(pl);
    }

}
