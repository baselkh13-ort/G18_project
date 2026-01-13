package logic;

import java.sql.Timestamp;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import common.Order;
import common.Table;
import common.OpeningHour;
import server.OrderRepository;
import server.TableRepository;
import server.OpeningHoursRepository;

/**
 * Logic controller for handling restaurant reservations.
 * Enforces rules regarding booking windows, table availability, and capacity.
 */
public class ReservationLogic {

    private final OrderRepository orderRepo;
    private final TableRepository tableRepo;
    private final OpeningHoursRepository openingHoursRepo;

    public ReservationLogic() {
        this.orderRepo = new OrderRepository();
        this.tableRepo = new TableRepository();
        this.openingHoursRepo = new OpeningHoursRepository();
    }

    /**
     * Validates and checks availability for a new order request.
     * @param order The requested order.
     * @return null if approved, or a List of alternative timestamps if fully booked.
     * @throws IllegalArgumentException if the validation fails (e.g. date too far).
     */
    public List<Timestamp> checkAvailability(Order order) {
    		Timestamp orderTime = new Timestamp(order.getOrderDate().getTime());
    		long now = System.currentTimeMillis();
    		
        Calendar cal = Calendar.getInstance();
        cal.setTime(orderTime);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK); 
        java.sql.Date sqlDate = new java.sql.Date(orderTime.getTime());
        
        OpeningHour hours = openingHoursRepo.getHoursForDate(sqlDate, dayOfWeek);
        //DEBUG
        System.out.println("DEBUG: Checking Time: " + orderTime + " | Day: " + dayOfWeek);
        if (hours != null) {
            System.out.println("DEBUG: Found Hours -> Open: " + hours.getOpenTime() + 
                               ", Close: " + hours.getCloseTime() + 
                               ", IsClosed: " + hours.isClosed());
        } else {
            System.out.println("DEBUG: No opening hours found for this day!");
        }
        //END DEBUG
        if (hours == null || hours.isClosed()) {
            throw new IllegalArgumentException("The restaurant is closed on this day.");
        }
        Calendar currentCal = Calendar.getInstance();
        currentCal.setTime(orderTime);

        Calendar openCal = Calendar.getInstance();
        openCal.setTime(hours.getOpenTime());
        openCal.set(currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH), currentCal.get(Calendar.DAY_OF_MONTH));
        Timestamp openTimestamp = new Timestamp(openCal.getTimeInMillis());

        Calendar closeCal = Calendar.getInstance();
        closeCal.setTime(hours.getCloseTime());
        closeCal.set(currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH), currentCal.get(Calendar.DAY_OF_MONTH));
        Timestamp closeTimestamp = new Timestamp(closeCal.getTimeInMillis());

        if (orderTime.before(openTimestamp) || orderTime.after(closeTimestamp)) {
             Time timeForMsg = new Time(orderTime.getTime());
             throw new IllegalArgumentException("The restaurant is closed at " + timeForMsg);
        }
        
       
       
        
        // Rule: Booking must be at least 1 hour from now
        if (orderTime.getTime() < now + 3600000) { 
            throw new IllegalArgumentException("Orders must be placed at least 1 hour in advance.");
        }

        // Rule: Booking cannot be more than 1 month ahead
        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.MONTH, 1);
        if (orderTime.after(maxDate.getTime())) {
            throw new IllegalArgumentException("Orders cannot be placed more than 1 month in advance.");
        }

        // Check if there is a spot available at the requested time
        if (isTableAvailable(orderTime, order.getNumberOfGuests())) {
            return null; // null means "Approved, no alternatives needed"
        }

        // If not available, find alternative times (+/- 30 mins, +/- 60 mins)
        return findAlternatives(orderTime, order.getNumberOfGuests(), hours);    
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

            if (isTableAvailable(slotTimestamp, guests)) {
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
    /**
     * Helper to calculate availability: (Total Tables) - (Occupied Tables) > 0
     */
    private boolean isTableAvailable(Timestamp time, int newGuests) {
        List<Table> allTables = tableRepo.getAllTables();
        List<Order> existingOrders = orderRepo.getOverlappingOrders(time);
        
        List<Integer> allGroups = new ArrayList<>();
        for (Order o : existingOrders) {
            allGroups.add(o.getNumberOfGuests());
        }
        allGroups.add(newGuests);
        
        Collections.sort(allGroups, Collections.reverseOrder());
        
        Collections.sort(allTables, (t1, t2) -> Integer.compare(t1.getCapacity(), t2.getCapacity()));
        
        List<Table> tempTables = new ArrayList<>(allTables);
        
        for (int groupSize : allGroups) {
            Table matchedTable = null;
            for (Table t : tempTables) {
                if (t.getCapacity() >= groupSize) {
                    matchedTable = t;
                    break; 
                }
            }
            if (matchedTable != null) {
                tempTables.remove(matchedTable);
            } else {
                return false; 
            }
        }
        return true;
    }

    private List<Timestamp> findAlternatives(Timestamp originalTime, int guests, OpeningHour hours) {
        List<Timestamp> alternatives = new ArrayList<>();
        int[] offsets = {-30, 30, -60, 60}; 

        for (int minutes : offsets) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(originalTime);
            cal.add(Calendar.MINUTE, minutes);
            Timestamp newTime = new Timestamp(cal.getTimeInMillis());
            Time newTimeCheck = new Time(newTime.getTime());

            if (newTimeCheck.before(hours.getOpenTime()) || newTimeCheck.after(hours.getCloseTime())) {
                continue; 
            }

            if (isTableAvailable(newTime, guests)) {
                alternatives.add(newTime);
            }
        }
        return alternatives;
    }
}