package com.boda.bousers.controllers;

import com.boda.bousers.model.Family;
import com.boda.bousers.model.Person;
import com.boda.bousers.model.TableAssignmentRequest;
import com.boda.bousers.services.FamilyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/families")
@RequiredArgsConstructor
public class FamilyController {
    private final FamilyService familyService;

    // 1. Obtener todas las familias
    @GetMapping
    public ResponseEntity<List<Family>> getAllFamilies() {
        return ResponseEntity.ok(familyService.getAllFamilies());
    }

    // 2. Obtener una familia por ID
    @GetMapping("/idfamily/{id}")
    public ResponseEntity<Family> getFamilyById(@PathVariable Long id) {
        return familyService.getFamilyById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. Obtener familias filtradas por grupo (ej: "FAMILIA NOVIO" o "FAMILIA NOVIA")
    @GetMapping("/group")
    public ResponseEntity<List<Family>> getFamiliesByGroup(@RequestParam String group) {
        return ResponseEntity.ok(familyService.getFamiliesByGroup(group));
    }

    @GetMapping("/phone/{tlf}")
    public ResponseEntity<Family> getFamilyByPhone(@PathVariable String tlf) {
        return familyService.getFamilyByPhone(tlf)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 4. Crear una sola familia
    @PostMapping
    public ResponseEntity<Family> createFamily(@RequestBody Family family) {
        Family created = familyService.createFamily(family);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // 6. Actualizar una familia existente
    @PutMapping("/{id}")
    public ResponseEntity<Family> updateFamily(@PathVariable String id, @RequestBody Family family) {
        return familyService.updateFamily(id, family)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 7. Eliminar una familia
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFamily(@PathVariable String id) {
        if (familyService.deleteFamily(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // 8. Asignar una mesa a una persona concreta de la familia
    @PutMapping("/{id}/people/{personId}/table")
    public ResponseEntity<?> assignTable(@PathVariable String id,
                                         @PathVariable String personId,
                                         @RequestBody TableAssignmentRequest request) {
        if (request.getTable() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debes indicar la mesa."));
        }
        try {
            Person person = familyService.assignTable(id, personId, request.getTable());
            return ResponseEntity.ok(Map.of("message", "Mesa asignada correctamente.", "person", person));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}