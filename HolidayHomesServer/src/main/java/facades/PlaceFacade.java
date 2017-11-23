package facades;

import customExceptions.DBException;
import entity.Place;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

public class PlaceFacade {

    private final EntityManager EM;

    public PlaceFacade(EntityManager EM) {
        this.EM = EM;
    }

    /*
        This method is used to retrieve all locations from the databse.
        The method also retrieves the ratings for the locations through the getRatingForLocation method.
        The method also retrieves if the user has already rated this location.
        Throws DBException if soemthing is wrong with the database.
        Returns a list with all the locations and their info.
     */
    public List<Place> getAllPlaces(String userName) throws DBException {
        List<Place> toReturn = new ArrayList();
        try {
            Query q = EM.createQuery("SELECT p FROM Place p");
            toReturn = q.getResultList();
        } catch (Exception e) {
            throw new DBException("facades.PlaceFacade.getAllPlaces");
        }
        for (Place p : toReturn) {
            double rating = getRatingForPlace(p.getPlaceName()); // Get the rating from Databae
            p.setRating(rating);

            if (!userName.equals("unauthorized")) { //If the user is logged in
                int userRating = getUserRating(userName, p.getPlaceName()); // Get the user vote on this location
                p.setUserRating(userRating);
            }
        }
        return toReturn;
    }

    //Creates new location in the database, returns null if failed
    public void createNewPlace(Place location) throws DBException {
        try {
            EM.getTransaction().begin();
            EM.persist(location);
            EM.getTransaction().commit();

        } catch (Exception e) {
            throw new DBException("facades.PlaceFacade.createNewPlace");
        }
    }

    /*
        This method is used for adding ratings for locations given the user and the location name.
        Throws DBException if the Database refuses the creation.
     */
    public void addRatingForPlace(String placeName, int rating, String userName) throws DBException {
        try {
            EM.getTransaction().begin();
            Query q = EM.createNativeQuery("INSERT INTO place_rating (place_name, rating, user_name) VALUES (?, ?, ?);");
            q.setParameter(1, placeName);
            q.setParameter(2, rating);
            q.setParameter(3, userName);
            q.executeUpdate();
            EM.getTransaction().commit();

        } catch (Exception e) {
            throw new DBException("facades.PlaceFacade.addRatingForPlace");
        }

    }

    /*
        This method is used to retrieve the rating for a specific location given its name.
        The method is used by getAllLocations and getLocation.
        Throws DBExceptions if there is something wrong with the Database.
     */
    private double getRatingForPlace(String placeName) throws DBException {
        double rating = 0;

        try {
            DecimalFormat df = new DecimalFormat(".#");
            Query q = EM.createNativeQuery("SELECT AVG(rating) FROM place_rating WHERE place_name = ?;");
            q.setParameter(1, placeName);
            BigDecimal result = (BigDecimal) q.getSingleResult(); //get the result from DB
            if (result != null) {
                rating = result.doubleValue();
            }
            return rating;
        } catch (Exception e) {
            throw new DBException("facades.PlaceFacade.getRatingForPlace");
        }
    }

    /*
        This method is used to check if the user has already voted for a specific location.
        Return the user`s rating or 0 if the user hasn`t voted
     */
    private int getUserRating(String userName, String placeName) throws DBException {
        try {
            Query q = EM.createNativeQuery("SELECT rating FROM place_rating WHERE place_name = ? AND user_name = ?;");
            q.setParameter(1, placeName);
            q.setParameter(2, userName);
            int rating = (int) q.getSingleResult();
            return rating; 
            
        } catch (NoResultException e) {
            return 0; //User hasn`t rated the place
            
        } catch (Exception e) {
            throw new DBException("facades.PlaceFacade.hasUserVoted");
        }
    }
}
