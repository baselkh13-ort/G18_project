package logic;

import java.sql.Timestamp;
import java.sql.Time;
import java.util.*;
import common.*;
import server.*;

public class ReservationLogic {

    private final OrderRepository orderRepo;
    private final TableRepository tableRepo;
    private final OpeningHoursRepository openingHoursRepo;

    public ReservationLogic() {
        this.orderRepo = new OrderRepository();
        this.tableRepo = new TableRepository();
        this.openingHoursRepo = new OpeningHoursRepository();
    }

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
        
       

    private void validateBookingWindow(Timestamp orderTime, long now) {
        if (orderTime.getTime() < now + 3600000)
            throw new IllegalArgumentException("Must book at least 1 hour in advance.");
        
        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.MONTH, 1);
        if (orderTime.after(maxDate.getTime()))
            throw new IllegalArgumentException("Cannot book more than 1 month ahead.");
    }
    
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