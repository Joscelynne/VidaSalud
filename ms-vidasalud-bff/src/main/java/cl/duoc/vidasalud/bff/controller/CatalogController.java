package cl.duoc.vidasalud.bff.controller;

import cl.duoc.vidasalud.bff.dto.CatalogDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    @GetMapping("/specialties")
    @PreAuthorize("hasAnyRole('Admin', 'Operator')")
    public ResponseEntity<List<CatalogDto>> getSpecialties() {
        List<CatalogDto> specialties = List.of(
                CatalogDto.builder()
                        .id("SPEC-01")
                        .name("Medicina General")
                        .description("Atención primaria integral para adultos y jóvenes.")
                        .availableDoctorsCount(14)
                        .consultationFee(25000)
                        .active(true)
                        .build(),
                CatalogDto.builder()
                        .id("SPEC-02")
                        .name("Pediatría")
                        .description("Cuidado especializado para recién nacidos, niños y adolescentes.")
                        .availableDoctorsCount(8)
                        .consultationFee(32000)
                        .active(true)
                        .build(),
                CatalogDto.builder()
                        .id("SPEC-03")
                        .name("Cardiología")
                        .description("Diagnóstico y tratamiento de patologías del sistema cardiovascular.")
                        .availableDoctorsCount(5)
                        .consultationFee(45000)
                        .active(true)
                        .build(),
                CatalogDto.builder()
                        .id("SPEC-04")
                        .name("Dermatología")
                        .description("Tratamiento clínico y quirúrgico de enfermedades de la piel.")
                        .availableDoctorsCount(6)
                        .consultationFee(40000)
                        .active(true)
                        .build()
        );
        return ResponseEntity.ok(specialties);
    }
}
