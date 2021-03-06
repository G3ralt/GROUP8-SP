package facades;

import customExceptions.DBException;
import entity.Booking;
import entity.Rentable;
import java.math.BigDecimal;
import java.util.*;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

public class RentableFacade {

    private final EntityManager EM;

    public RentableFacade(EntityManager EM) {
        this.EM = EM;
    }

    /*
        Get the Rentable for a specific name.
     */
    public Rentable getRentableByName(String rentableName, String username) throws DBException {
        Rentable toReturn;
        try {
            EM.getEntityManagerFactory().getCache().evictAll();
            Query q = EM.createQuery("SELECT r FROM Rentable r WHERE r.rentableName = :rentableName");
            q.setParameter("rentableName", rentableName);
            toReturn = (Rentable) q.getSingleResult();

        } catch (Exception e) {
            throw new DBException("facades.RentableFacade.getRentableByName");
        }

        double rating = getRatingForRentable(rentableName); // Get the rating from Databae
        toReturn.setRating(rating);

        if (!username.equals("unauthorized")) { //If the user is logged in
            int userRating = getUserRating(username, toReturn.getRentableName()); // Get the user vote on this location
            toReturn.setUserRating(userRating);
        }
        //Check availability
        ArrayList<String> list = getAvailableWeeksForRentable(toReturn);
        toReturn.setAvailableWeeks(list);

        return toReturn;
    }

    /*
        This method is used to retrieve all rentables from the databse.
        The method also retrieves the ratings for the rentables through the getRatingForRentable method.
        The method also retrieves if the user has already rated this rentable.
        The method get all the available weeks for this rentable
        Throws DBException if something is wrong with the database.
        Returns a list with all the rentables and their info.
     */
    public List<Rentable> getAllRentables(String username) throws DBException {
        List<Rentable> toReturn = new ArrayList();
        try {
            EM.getEntityManagerFactory().getCache().evictAll();
            Query q = EM.createQuery("SELECT r FROM Rentable r");
            toReturn = q.getResultList();

        } catch (Exception e) {
            throw new DBException("facades.RentableFacade.getAllRentables");
        }

        for (Rentable r : toReturn) {
            double rating = getRatingForRentable(r.getRentableName()); // Get the rating from Databae
            r.setRating(rating);

            if (!username.equals("unauthorized")) { //If the user is logged in
                int userRating = getUserRating(username, r.getRentableName()); // Get the user vote on this location
                r.setUserRating(userRating);
            }
            //Check availability
            ArrayList<String> list = getAvailableWeeksForRentable(r);
            r.setAvailableWeeks(list);

        }

        return toReturn;
    }

    public boolean checkForRentableName(String rentableName) throws DBException {
        try {
            EM.getTransaction().begin();
            Query q = EM.createQuery("SELECT r FROM Rentable r WHERE r.rentableName = :rentableName");
            q.setParameter("rentableName", rentableName);
            Rentable r = (Rentable) q.getSingleResult();

        } catch (NoResultException e) {
            return false; //Return false = NON EXISTING NAME

        } catch (Exception e) {
            throw new DBException("facades.RentableFacade.checkForRentableName");
        }

        return true; //Return true if NAME is found
    }

    //Creates new location in the database, returns null if failed
    public void createNewRentable(Rentable rentable) throws DBException {
        try {
            EM.getTransaction().begin();
            EM.persist(rentable);
            EM.getTransaction().commit();

        } catch (Exception e) {
            throw new DBException("facades.RentableFacade.createNewRentable");
        }
    }

    /*
        This method is used for adding ratings for locations given the user and the location name.
        Throws DBException if the Database refuses the creation.
     */
    public void addRatingForRentable(String rentableName, int rating, String userName) throws DBException {
        try {
            int userRating = getUserRating(userName, rentableName);
            if (userRating != 0) { // If the rating is different from 0 == user has voted
                throw new Exception();
            }

            EM.getTransaction().begin();
            Query q = EM.createNativeQuery("INSERT INTO rentable_rating (rentable_name, rating, user_name) VALUES (?, ?, ?);");
            q.setParameter(1, rentableName);
            q.setParameter(2, rating);
            q.setParameter(3, userName);
            q.executeUpdate();
            EM.getTransaction().commit();

        } catch (Exception e) {
            throw new DBException("facades.RentableFacade.addRatingForPlace");
        }

    }

    /*
        This method is used to retrieve the rating for a specific location given its name.
        The method is used by getAllLocations and getLocation.
        Throws DBExceptions if there is something wrong with the Database.
     */
    private double getRatingForRentable(String rentableName) throws DBException {
        try {
            Query q = EM.createNativeQuery("SELECT AVG(rating) FROM rentable_rating WHERE rentable_name = ?;");
            q.setParameter(1, rentableName);
            BigDecimal result = (BigDecimal) q.getSingleResult(); //get the result from DB
            if (result == null) { //if there are no ratings
                return 0;
            }
            result = result.setScale(1); //format the result to #.#
            double rating = result.doubleValue(); //parse it to double

            return rating;

        } catch (Exception e) {
            throw new DBException("facades.RentableFacade.getRatingForRentable");
        }
    }

    /*
        This method is used to check if the user has already voted for a specific rentable.
        Return the user`s rating or 0 if the user hasn`t voted
     */
    public int getUserRating(String userName, String rentableName) throws DBException {
        try {
            Query q = EM.createNativeQuery("SELECT rating FROM rentable_rating WHERE rentable_name = ? AND user_name = ?;");
            q.setParameter(1, rentableName);
            q.setParameter(2, userName);
            int rating = (int) q.getSingleResult();
            return rating;

        } catch (NoResultException e) {
            return 0; //User hasn`t rated the place

        } catch (Exception e) {
            throw new DBException("facades.RentableFacade.hasUserVoted");
        }
    }

    public void updateRatingForRentable(String rentableName, int rating, String userName) throws DBException {
        try {
            EM.getTransaction().begin();
            Query q = EM.createNativeQuery("UPDATE rentable_rating SET rating = ? WHERE user_name = ? AND rentable_name = ?");
            q.setParameter(1, rating);
            q.setParameter(2, userName);
            q.setParameter(3, rentableName);
            q.executeUpdate();
            EM.getTransaction().commit();

        } catch (Exception e) {
            throw new DBException("facades.RentableFacade.updateRatingForRentable");
        }
    }

    private ArrayList<String> getAvailableWeeksForRentable(Rentable rentable) {
        ArrayList<String> availableWeeks = new ArrayList();
        //Get the date, year and week
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int currentWeek = cal.get(Calendar.WEEK_OF_YEAR);
        int currentYear = cal.get(Calendar.YEAR);
        int nextYear = currentYear + 1;

        //Add remaining weeks for current Year
        while (currentWeek <= 52) {
            String week = currentYear + "-" + currentWeek;
            availableWeeks.add(week);
            currentWeek++;
        }

        //Add weeks for next year
        for (int i = 1; i <= 52; i++) {
            String week = nextYear + "-" + i;
            availableWeeks.add(week);
        }

        //Get the bookings
        ArrayList<String> bookedWeeks = new ArrayList();
        for (Booking booking : rentable.getBookingCollection()) {
            bookedWeeks.add(booking.getWeekNumber());
        }

        //Remove all after 6 months time
        availableWeeks.subList(25, availableWeeks.size()).clear();

        //Remove booked weeks
        availableWeeks.removeAll(bookedWeeks);

        return availableWeeks;
    }
}
