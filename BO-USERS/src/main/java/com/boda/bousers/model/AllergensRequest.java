package com.boda.bousers.model;

import lombok.Data;

import java.util.List;

// Petición para actualizar los alérgenos del invitado logueado
@Data
public class AllergensRequest {
    private List<String> allergens;
    private String notes;
}
