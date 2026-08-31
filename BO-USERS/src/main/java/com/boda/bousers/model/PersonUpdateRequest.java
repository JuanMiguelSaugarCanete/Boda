package com.boda.bousers.model;

import lombok.Data;

import java.util.List;

// Petición para actualizar los datos de un miembro de la familia
// (o de la propia persona logueada).
@Data
public class PersonUpdateRequest {
    private String name;
    private String tlf;
    // SI / NO / AUN_NO_SE
    private String attendance;
    private String diet;
    private String dietNotes;
    private List<String> allergens;
    private String imgProfile;
}
