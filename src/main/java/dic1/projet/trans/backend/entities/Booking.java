package dic1.projet.trans.backend.entities;

import dic1.projet.trans.backend.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bookings")
public class Booking {
    @Id
    private String bookingId;
    private double totalAmount;
    private LocalDateTime bookingDate;
    private String paymentMethod;
    private BookingStatus bookingStatus;
    private String clientId;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservedTicket {
        private String ticketId;
        private int quantity;
    }
    
    private List<ReservedTicket> tickets;
}