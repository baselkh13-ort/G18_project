package logic;

import java.sql.Timestamp;
import java.sql.Time;
import java.util.*;
import common.*;
import server.*;

/**
 * The logic class that manages all table reservations.
 *
 * Software Structure:
 * This class is part of the Logic Layer in the software architecture.
 * It receives reservation requests from the Server and communicates with the
 * Database Layer (Repositories) to check for table availability and opening hours.
 * It enforces the restaurant's rules before saving an order.
 *
 * @author Dana Zablev
 * @version 1.0
 */
public class ReservationLogic {

    /** Connection to the order database table. */
    private final OrderRepository orderRepo;
    /** Connection to the table database table. */
    private final TableRepository tableRepo;
    /** Connection to the opening hours database table. */
    private final OpeningHoursRepository openingHoursRepo;

    /**
     * Constructor for the class.
     * Initializes the connections to the database repositories.
     */
    public ReservationLogic() {
        this.orderRepo = new OrderRepository();
        this.tableRepo = new TableRepository();
        this.openingHoursRepo = new OpeningHoursRepository();
    }

    /**
     * Main method to check if an order can be placed.
     * It checks opening hours, booking window rules, and table availability.
     *
     * @param order The order object containing date and guest count.
     * @return A list of alternative times if the table is full, or null if the booking is successful.
     */
    public List<Timestamp> checkAvailability(Order order) {
        System.out.println("DEBUG: Checking Availability for: " + order.getOrderDate() + ", Guests: " + order.getNumberOfGuests());
        
        Timestamp orderTime = new Timestamp(order.getOrderDate().getTime());
        long now = System.currentTimeMillis();
        
        try {
            validateOpeningHours(orderTime);
        } catch (IllegalArgumentException e) {
            System.out.println("DEBUG: Failed Opening Hours: " + e.getMessage());
            throw e;
        }

        try {
            validateBookingWindow(orderTime, now);
        } catch (IllegalArgumentException e) {
            System.out.println("DEBUG: Failed Booking Window. Now: " + new Date(now) + ", Order: " + orderTime);
            throw e;
        }

        if (isTableAvailableBestFit(orderTime, order.getNumberOfGuests())) {
            System.out.println("DEBUG: Table IS Available! (Returning null for success)");
            return null; 
        }

        System.out.println("DEBUG: Table NOT Available. Searching alternatives...");
        return findAlternativesBestFit(orderTime, order.getNumberOfGuests());
    }

    /**
     * The algorithm that finds the best table for a group.
     * It checks which tables are free and picks the one that fits best (Best Fit).
     *
     * @param time The time of the reservation.
     * @param newGroupSize The number of guests.
     * @return True if a suitable table is found, False otherwise.
     */
    private boolean isTableAvailableBestFit(Timestamp time, int newGroupSize) {
        List<Table> allTables = tableRepo.getAllTables();
        
        if (allTables.isEmpty()) {
            System.out.println("CRITICAL ERROR: No tables found in DB! Did you run INSERT INTO tables?");
            return false;
        }

        List<Order> existingOrders = orderRepo.getOverlappingOrders(time);
        System.out.println("DEBUG: Found " + allTables.size() + " tables and " + existingOrders.size() + " overlapping orders.");

        List<Integer> groups = new ArrayList<>();
        for (Order o : existingOrders) {
            groups.add(o.getNumberOfGuests());
        }
        groups.add(newGroupSize);

        groups.sort(Collections.reverseOrder());

        List<Table> availableTables = new ArrayList<>(allTables);

        for (int groupSize : groups) {
            Table bestTable = null;
            for (Table t : availableTables) {
                if (t.getCapacity() >= groupSize) {
                    if (bestTable == null || t.getCapacity() < bestTable.getCapacity()) {
                        bestTable = t;
                    }
                }
            }

            if (bestTable != null) {
                availableTables.remove(bestTable); 
            } else {
                System.out.println("DEBUG: Failed to find table for group size: " + groupSize);
                return false; 
            }
        }
        return true;
    }

    
    /**
     * Checks if the restaurant is open at the requested time.
     *
     * @param orderTime The time the customer wants to book.
     * @throws IllegalArgumentException If the restaurant is closed.
     */
    private void validateOpeningHours(Timestamp orderTime) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(orderTime);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        java.sql.Date sqlDate = new java.sql.Date(orderTime.getTime());
        OpeningHour hours = openingHoursRepo.getHoursForDate(sqlDate, dayOfWeek);

        if (hours == null || hours.isClosed()) 
            throw new IllegalArgumentException("Restaurant is closed on this date.");
        java.time.LocalTime reqLocal = orderTime.toLocalDateTime().toLocalTime();
        java.time.LocalTime openLocal = hours.getOpenTime().toLocalTime();
        java.time.LocalTime closeLocal = hours.getCloseTime().toLocalTime();

        if (reqLocal.isBefore(openLocal) || reqLocal.isAfter(closeLocal)) {
            throw new IllegalArgumentException("Time outside opening hours (" + hours.getOpenTime() + "-" + hours.getCloseTime() + ")");
        }
    }
        
        
    /**
     * Checks if the booking is made within the allowed time window.
     * Rule: Must be at least 1 hour in advance and not more than 1 month ahead.
     *
     * @param orderTime The requested reservation time.
     * @param now The current server time.
     */
    private void validateBookingWindow(Timestamp orderTime, long now) {
        if (orderTime.getTime() < now + 3600000)
            throw new IllegalArgumentException("Must book at least 1 hour in advance.");
        
        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.MONTH, 1);
        if (orderTime.after(maxDate.getTime()))
            throw new IllegalArgumentException("Cannot book more than 1 month ahead.");
    }
    
    /**
     * Finds alternative time slots if the requested time is full.
     * It checks 30 and 60 minutes before and after the requested time.
     *
     * @param originalTime The time the user originally wanted.
     * @param guests The number of guests.
     * @return A list of available alternative times.
     */
    private List<Timestamp> findAlternativesBestFit(Timestamp originalTime, int guests) {
        List<Timestamp> alternatives = new ArrayList<>();
        int[] offsets = {-30, 30, -60, 60};
        for (int min : offsets) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(originalTime);
            cal.add(Calendar.MINUTE, min);
            Timestamp newTime = new Timestamp(cal.getTimeInMillis());
            if (isTableAvailableBestFit(newTime, guests)) {
                alternatives.add(newTime);
            }
        }
        return alternatives;
    }
    
    /**
     * Generates a list of all available time slots for a specific date.
     * This is used by the UI to show the user what times they can pick.
     *
     * @param date The date to check.
     * @param guests The number of guests.
     * @return A list of strings representing available times (e.g., "18:00", "18:30").
     */
    public List<String> getAvailableSlotsForDate(java.sql.Date date, int guests) {
        List<String> validSlots = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        OpeningHour hours = openingHoursRepo.getHoursForDate(date, dayOfWeek);
        if (hours == null || hours.isClosed()) {
                validSlots.add("CLOSED");
            return validSlots; 
        }
        Calendar currentSlot = Calendar.getInstance();
        currentSlot.setTime(date); 
        Calendar openCal = Calendar.getInstance();
        openCal.setTime(hours.getOpenTime());
        currentSlot.set(Calendar.HOUR_OF_DAY, openCal.get(Calendar.HOUR_OF_DAY));
        currentSlot.set(Calendar.MINUTE, openCal.get(Calendar.MINUTE));
        currentSlot.set(Calendar.SECOND, 0);
        Calendar closeCal = Calendar.getInstance();
        closeCal.setTime(date);
        Calendar closeTimeOrig = Calendar.getInstance();
        closeTimeOrig.setTime(hours.getCloseTime());
        closeCal.set(Calendar.HOUR_OF_DAY, closeTimeOrig.get(Calendar.HOUR_OF_DAY));
        closeCal.set(Calendar.MINUTE, closeTimeOrig.get(Calendar.MINUTE));
        closeCal.add(Calendar.HOUR_OF_DAY, -1); 
        long now = System.currentTimeMillis();
        while (currentSlot.before(closeCal) || currentSlot.equals(closeCal)) {
            Timestamp slotTimestamp = new Timestamp(currentSlot.getTimeInMillis());
            if (slotTimestamp.getTime() < now + 3600000) { 
                currentSlot.add(Calendar.MINUTE, 30);
                continue;
            }
            if (isTableAvailableBestFit(slotTimestamp, guests)) {
                String timeString = String.format("%02d:%02d", 
                                                currentSlot.get(Calendar.HOUR_OF_DAY), 
                                                currentSlot.get(Calendar.MINUTE));
                validSlots.add(timeString);
            }
            currentSlot.add(Calendar.MINUTE, 30);
        }
        if (validSlots.isEmpty()) {
            validSlots.add("FULL"); 
        }
        return validSlots;
    }
}