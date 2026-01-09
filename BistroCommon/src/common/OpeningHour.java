package common;

import java.io.Serializable;
import java.sql.Time;
import java.util.Date;

/**
 * Represents the restaurant's operating hours logic.
 * Can define regular weekly hours or specific exception dates (holidays, etc.).
 */
public class OpeningHour implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private int id;
    private int dayOfWeek; // 1=Sunday, 7=Saturday
    private Date specificDate; // Null if it's a regular weekly schedule
    private Time openTime;
    private Time closeTime;
    private boolean isClosed; // True if the restaurant is closed on this day

    /**
     * Full constructor for database retrieval.
     */
    public OpeningHour(int id, int dayOfWeek, Date specificDate, Time openTime, Time closeTime, boolean isClosed) {
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.specificDate = specificDate;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.isClosed = isClosed;
    }

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
}