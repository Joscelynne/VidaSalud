package cl.duoc.vidasalud.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogDto {
    private String id;
    private String name;
    private String description;
    private int availableDoctorsCount;
    private double consultationFee;
    private boolean active;
}
