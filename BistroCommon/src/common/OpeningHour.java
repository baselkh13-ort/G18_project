package common;



import java.io.Serializable;
import java.sql.Time;
import java.time.LocalTime;
import java.sql.Date;

/**
 * Represents the restaurant's operating hours entity.
 * This class supports two types of schedules:
 * Regular Weekly Hours:Recurring hours based on the day of the week
 * (e.g., Every Sunday).
 * Specific Date Exceptions:Special hours or closures for specific
 * dates (e.g., Holidays).
 * Implements {@link Serializable} to allow transfer between Client and Server.
 */
public class OpeningHour implements Serializable {

	private static final long serialVersionUID = 1L;

	/** Unique identifier for the database record. */
	private int id;

	/**
	 * The day of the week (1 = Sunday, 7 = Saturday). 0 if irrelevant (specific
	 * date).
	 */
	private int dayOfWeek;

	/**
	 * The specific date for exceptions. Null if this represents a regular weekly
	 * schedule.
	 */
	private Date specificDate;

	/** The opening time. Null if the restaurant is closed all day. */
	private Time openTime;

	/** The closing time. Null if the restaurant is closed all day. */
	private Time closeTime;

	/**
	 * Flag indicating if the restaurant is closed on this specific day/schedule.
	 */
	private boolean isClosed;

	/**
	 * Full constructor used for retrieving data from the database.
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
	 * Constructor for updating regular weekly hours (OPEN). Used for: "Every Sunday
	 * open 08:00-12:00".
	 */
	public OpeningHour(int dayOfWeek, LocalTime start, LocalTime end) {
		this.dayOfWeek = dayOfWeek;
		this.openTime = Time.valueOf(start);
		this.closeTime = Time.valueOf(end);
		this.specificDate = null;
		this.isClosed = false;
	}

	/**
	 * Constructor for updating regular weekly hours (CLOSED). Used for: "Every
	 * Saturday CLOSED". * @param dayOfWeek The day index (1-7).
	 * 
	 * @param isClosed Should be true.
	 */
	public OpeningHour(int dayOfWeek, boolean isClosed) {
		this.dayOfWeek = dayOfWeek;
		this.isClosed = isClosed;
		// Reset other fields
		this.openTime = null;
		this.closeTime = null;
		this.specificDate = null;
	}

	/**
	 * Constructor for setting a specific date as OPEN with special hours. Used for:
	 * "15/05/2025 open 10:00-14:00".
	 */
	public OpeningHour(java.sql.Date specificDate, LocalTime start, LocalTime end) {
		this.specificDate = specificDate;
		this.openTime = Time.valueOf(start);
		this.closeTime = Time.valueOf(end);
		this.isClosed = false;
		this.dayOfWeek = 0;
	}

	/**
	 * Constructor for setting a specific date as CLOSED. Used for: "15/05/2025
	 * CLOSED".
	 */
	public OpeningHour(java.sql.Date specificDate, boolean isClosed) {
		this.specificDate = specificDate;
		this.isClosed = isClosed;
		this.openTime = null;
		this.closeTime = null;
		this.dayOfWeek = 0;
	}

	// Getters and Setters

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getDayOfWeek() {
		return dayOfWeek;
	}

	public void setDayOfWeek(int dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}

	public Date getSpecificDate() {
		return specificDate;
	}

	public void setSpecificDate(Date specificDate) {
		this.specificDate = specificDate;
	}

	public Time getOpenTime() {
		return openTime;
	}

	public void setOpenTime(Time openTime) {
		this.openTime = openTime;
	}

	public Time getCloseTime() {
		return closeTime;
	}

	public void setCloseTime(Time closeTime) {
		this.closeTime = closeTime;
	}

	public boolean isClosed() {
		return isClosed;
	}

	public void setClosed(boolean isClosed) {
		this.isClosed = isClosed;
	}

	@Override
	public String toString() {
		if (isClosed)
			return "Closed";
		return openTime + " - " + closeTime;
	}
}