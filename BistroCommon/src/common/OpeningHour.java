package common;



import java.io.Serializable;
import java.sql.Time;
import java.time.LocalTime;
import java.sql.Date;



/**
 * Represents the restaurant's operating hours entity.
 * <p>
 * This class supports two types of schedules:
 * <ul>
 * <li><b>Regular Weekly Hours:</b> Recurring hours based on the day of the week (e.g., Every Sunday).</li>
 * <li><b>Specific Date Exceptions:</b> Special hours or closures for specific dates (e.g., Holidays), 
 * which override the regular schedule.</li>
 * </ul>
 * </p>
 * Implements {@link Serializable} to allow transfer between Client and Server.
 */

public class OpeningHour implements Serializable {  
    private static final long serialVersionUID = 1L;

    /** Unique identifier for the database record. */

    private int id;


    /** The day of the week (1 = Sunday, 7 = Saturday). 0 if irrelevant (specific date). */

    private int dayOfWeek; 

    

    /** The specific date for exceptions. Null if this represents a regular weekly schedule. */

    private Date specificDate; 

    

    /** The opening time. Null if the restaurant is closed all day. */

    private Time openTime;

    

    /** The closing time. Null if the restaurant is closed all day. */

    private Time closeTime;

    

    /** Flag indicating if the restaurant is closed on this specific day/schedule. */

    private boolean isClosed; 



    /**

     * Full constructor used for retrieving data from the database.

     * * @param id The record ID.

     * @param dayOfWeek The day of the week (1-7).

     * @param specificDate The specific date (or null).

     * @param openTime The opening time.

     * @param closeTime The closing time.

     * @param isClosed Whether the place is closed.

     */

    public OpeningHour(int id, int dayOfWeek, Date specificDate, Time openTime, Time closeTime, boolean isClosed) {

        this.id = id;

        this.dayOfWeek = dayOfWeek;

        this.specificDate = specificDate;

        this.openTime = openTime;

        this.closeTime = closeTime;

        this.isClosed = isClosed;

    }



    /**

     * Constructor for updating regular weekly hours.

     * <p>

     * Used by the client GUI when setting a recurring schedule (e.g., "Every Sunday").

     * Automatically converts {@link LocalTime} to {@link java.sql.Time}.

     * </p>

     * * @param dayOfWeek The day of the week (1=Sunday to 7=Saturday).

     * @param start The opening time (LocalTime).

     * @param end The closing time (LocalTime).

     */

    public OpeningHour(int dayOfWeek, LocalTime start, LocalTime end) {

        this.dayOfWeek = dayOfWeek;

        this.openTime = Time.valueOf(start);  

        this.closeTime = Time.valueOf(end);     

        this.specificDate = null;

        this.isClosed = false;

    }



    /**

     * Constructor for setting a specific date as OPEN with special hours.

     * <p>

     * Used for holidays or special events where hours differ from the regular schedule.

     * </p>

     * * @param specificDate The specific date for the exception.

     * @param start The opening time (LocalTime).

     * @param end The closing time (LocalTime).

     */

    public OpeningHour(java.sql.Date specificDate, LocalTime start, LocalTime end) {

        this.specificDate = specificDate;

        this.openTime = Time.valueOf(start);

        this.closeTime = Time.valueOf(end);

        this.isClosed = false;

        this.dayOfWeek = 0; // Not relevant for specific date logic

    }

    

    /**

     * Constructor for setting a specific date as CLOSED.

     * <p>

     * Used to mark holidays or closed events. Sets open/close times to null.

     * </p>

     * * @param specificDate The specific date to close.

     * @param isClosed Should be true to mark as closed.

     */

    public OpeningHour(java.sql.Date specificDate, boolean isClosed) {

        this.specificDate = specificDate;

        this.isClosed = isClosed;

        this.openTime = null;

        this.closeTime = null;

        this.dayOfWeek = 0;

    }



    //Getters and Setters



    public int getId() { return id; }

    public void setId(int id) { this.id = id; }



    public int getDayOfWeek() { return dayOfWeek; }

    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }



    public Date getSpecificDate() { return specificDate; }

    public void setSpecificDate(Date specificDate) { this.specificDate = specificDate; }



    public Time getOpenTime() { return openTime; }

    public void setOpenTime(Time openTime) { this.openTime = openTime; }



    public Time getCloseTime() { return closeTime; }

    public void setCloseTime(Time closeTime) { this.closeTime = closeTime; }



    public boolean isClosed() { return isClosed; }

    public void setClosed(boolean isClosed) { this.isClosed = isClosed; }

    

    /**

     * Returns a string representation of the opening hours.

     * Useful for debugging or simple UI display.

     * * @return "Closed" if the day is closed, otherwise "OpenTime - CloseTime".

     */

    @Override

    public String toString() {

        if (isClosed) return "Closed";

        return openTime + " - " + closeTime;

    }

}