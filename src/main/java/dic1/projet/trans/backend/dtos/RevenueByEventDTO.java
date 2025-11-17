package dic1.projet.trans.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueByEventDTO {
    private String eventId;
    private String title;
    private double totalRevenue;
}