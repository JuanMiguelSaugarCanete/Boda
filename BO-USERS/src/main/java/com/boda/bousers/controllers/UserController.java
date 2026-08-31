package com.boda.bousers.controllers;

import com.boda.bousers.model.AllergensRequest;
import com.boda.bousers.model.PersonUpdateRequest;
import com.boda.bousers.model.RsvpRequest;
import com.boda.bousers.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private static final List<String> VALID_ATTENDANCE = List.of("SI", "NO", "AUN_NO_SE");

    // Actualiza la asistencia del invitado logueado
    @PutMapping("/rsvp")
    public ResponseEntity<?> updateAttendance(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody RsvpRequest request) {
        if (request.getAttendance() == null || !VALID_ATTENDANCE.contains(request.getAttendance())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Debes indicar si asistirás."));
        }
        try {
            userService.updateAttendance(authHeader, request.getAttendance());
            return ResponseEntity.ok(Map.of("message", "Asistencia actualizada correctamente."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Actualiza los alérgenos del invitado logueado
    @PutMapping("/allergens")
    public ResponseEntity<?> updateAllergens(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody AllergensRequest request) {
        try {
            userService.updateAllergens(authHeader, request.getAllergens(), request.getNotes());
            return ResponseEntity.ok(Map.of("message", "Alérgenos actualizados correctamente."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Actualiza los datos de la propia persona logueada
    @PutMapping("/me")
    public ResponseEntity<?> updateMe(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody PersonUpdateRequest request) {
        try {
            userService.updateMe(authHeader, request);
            return ResponseEntity.ok(Map.of("message", "Información actualizada correctamente."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Actualiza la información de un miembro de la familia del invitado logueado
    @PutMapping("/people/{personId}")
    public ResponseEntity<?> updateFamilyMember(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String personId,
            @RequestBody PersonUpdateRequest request) {
        try {
            userService.updateFamilyMember(authHeader, personId, request);
            return ResponseEntity.ok(Map.of("message", "Miembro de la familia actualizado correctamente."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
