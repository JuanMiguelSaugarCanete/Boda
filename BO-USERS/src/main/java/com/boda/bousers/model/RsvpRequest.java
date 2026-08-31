package com.boda.bousers.model;

import lombok.Data;

// Petición para actualizar la asistencia del invitado logueado
@Data
public class RsvpRequest {
    // SI / NO / AUN_NO_SE
    private String attendance;
}
