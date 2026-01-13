package logic;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import common.Order;
import server.OrderRepository;
import server.TableRepository;

/**
 * Logic controller for handling restaurant reservations.
 * Enforces rules regarding booking windows, table availability, and capacity.
 */
public class ReservationLogic {

    private final OrderRepository orderRepo;
    private final TableRepository tableRepo;

    public ReservationLogic() {
        this.orderRepo = new OrderRepository();
        this.tableRepo = new TableRepository();
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
        return findAlternatives(orderTime, order.getNumberOfGuests());
    }

    /**
     * Helper to calculate availability: (Total Tables) - (Occupied Tables) > 0
     */
    private boolean isTableAvailable(Timestamp time, int guests) {
        int totalTables = tableRepo.countTablesByCapacity(guests);
        int occupiedTables = orderRepo.getOverlappingOrdersCount(time, guests);
        return (totalTables - occupiedTables) > 0;
    }

    /**
     * Searches for close available slots if the requested time is taken.
     */
    private List<Timestamp> findAlternatives(Timestamp originalTime, int guests) {
        List<Timestamp> alternatives = new ArrayList<>();
        int[] offsets = {-30, 30, -60, 60}; // Check half an hour and an hour before/after

        for (int minutes : offsets) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(originalTime);
            cal.add(Calendar.MINUTE, minutes);
            Timestamp newTime = new Timestamp(cal.getTimeInMillis());

            if (isTableAvailable(newTime, guests)) {
                alternatives.add(newTime);
            }
        }
        return alternatives;
    }
}