package com.boda.bousers.services;

import com.boda.bousers.model.Family;
import com.boda.bousers.model.Person;
import com.boda.bousers.model.PersonUpdateRequest;
import com.boda.bousers.repository.FamilyRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final List<String> VALID_ATTENDANCE = List.of("SI", "NO", "AUN_NO_SE");

    private final FamilyRepository familyRepository;
    private final JwtService jwtService;

    // PUT /api/users/rsvp — actualiza la asistencia del invitado del token
    public void updateAttendance(String authHeader, String attendance) {
        if (attendance == null || !VALID_ATTENDANCE.contains(attendance)) {
            throw new IllegalArgumentException("Debes indicar si asistirás.");
        }
        String email = emailFromToken(authHeader);
        Family family = findFamilyByEmail(email);
        Person person = findLoggedPerson(family, email);
        person.setAttendance(attendance);
        familyRepository.save(family);
    }

    // PUT /api/users/allergens — actualiza alérgenos y notas del invitado del token
    public void updateAllergens(String authHeader, List<String> allergens, String notes) {
        String email = emailFromToken(authHeader);
        Family family = findFamilyByEmail(email);
        Person person = findLoggedPerson(family, email);
        person.setAllergens(allergens != null ? allergens : List.of());
        person.setDietNotes(notes);
        familyRepository.save(family);
    }

    // PUT /api/users/me — actualiza los datos de la propia persona logueada
    public void updateMe(String authHeader, PersonUpdateRequest request) {
        String email = emailFromToken(authHeader);
        Family family = findFamilyByEmail(email);
        Person person = findLoggedPerson(family, email);
        applyUpdates(family, person, request);
        familyRepository.save(family);
    }

    // PUT /api/users/people/{personId} — actualiza un miembro de la familia del invitado
    public void updateFamilyMember(String authHeader, String personId, PersonUpdateRequest request) {
        String email = emailFromToken(authHeader);
        Family family = findFamilyByEmail(email);
        Person person = findPersonById(family, personId)
                .orElseThrow(() -> new IllegalArgumentException("El miembro indicado no pertenece a tu familia."));
        applyUpdates(family, person, request);
        familyRepository.save(family);
    }

    // Aplica los campos editables de la petición sobre la persona destino.
    // Solo se pueden modificar miembros de la propia familia.
    private void applyUpdates(Family family, Person person, PersonUpdateRequest request) {
        if (request.getAttendance() != null && !request.getAttendance().isBlank()
                && !VALID_ATTENDANCE.contains(request.getAttendance())) {
            throw new IllegalArgumentException("Debes indicar si asistirás.");
        }

        if (request.getTlf() != null && !request.getTlf().isBlank()) {
            String newPhone = normalizePhone(request.getTlf());
            if (!newPhone.equals(normalizePhone(person.getTlf()))) {
                checkPhoneNotUsedByOther(family, person, newPhone);
                person.setTlf(newPhone);
            }
        } else if (request.getTlf() != null) {
            person.setTlf(null);
        }

        if (request.getName() != null) {
            person.setName(request.getName().trim());
        }
        if (request.getAttendance() != null) {
            person.setAttendance(request.getAttendance().isBlank() ? null : request.getAttendance());
        }
        if (request.getDiet() != null) {
            person.setDiet(request.getDiet());
        }
        if (request.getDietNotes() != null) {
            person.setDietNotes(request.getDietNotes());
        }
        if (request.getAllergens() != null) {
            person.setAllergens(request.getAllergens());
        }
        if (request.getImgProfile() != null) {
            person.setImgProfile(request.getImgProfile().isBlank() ? null : request.getImgProfile().trim());
        }
    }

    // Evita duplicar teléfonos dentro de la misma familia o en otra familia.
    private void checkPhoneNotUsedByOther(Family family, Person person, String newPhone) {
        boolean usedInSameFamily = family.getPeople() != null && family.getPeople().stream()
                .filter(p -> p != person)
                .anyMatch(p -> normalizePhone(p.getTlf()).equals(newPhone));
        if (usedInSameFamily) {
            throw new IllegalArgumentException("Ese teléfono ya lo tiene otro miembro de tu familia.");
        }
        Optional<Family> existing = familyRepository.findByPeopleTlf(newPhone);
        if (existing.isPresent()) {
            boolean isSameFamily = existing.get().getId().equals(family.getId());
            if (!isSameFamily) {
                throw new IllegalArgumentException("Ese teléfono ya está registrado por otro invitado.");
            }
        }
    }

    private Optional<Person> findPersonById(Family family, String personId) {
        if (family.getPeople() == null || personId == null || personId.isBlank()) {
            return Optional.empty();
        }
        return family.getPeople().stream()
                .filter(p -> personId.equals(p.getPersonId()))
                .findFirst();
    }

    // Extrae y valida el email (subject) del token JWT
    private String emailFromToken(String authHeader) {
        String token = extractToken(authHeader);
        try {
            Claims claims = jwtService.validateToken(token);
            return normalizeEmail(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("El token es inválido o ha expirado.");
        }
    }

    private Family findFamilyByEmail(String email) {
        return familyRepository.findByPeopleEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("La sesión ya no es válida."));
    }

    // Localiza la persona logueada dentro de la familia a partir del email del token
    private Person findLoggedPerson(Family family, String email) {
        return findPersonByEmail(family, email)
                .orElseThrow(() -> new IllegalArgumentException("La sesión ya no es válida."));
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Token no proporcionado.");
        }
        return authHeader.substring(7).trim();
    }

    private Optional<Person> findPersonByEmail(Family family, String email) {
        if (family.getPeople() == null) {
            return Optional.empty();
        }
        return family.getPeople().stream()
                .filter(p -> p.getEmail() != null && normalizeEmail(p.getEmail()).equals(email))
                .findFirst();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        String normalized = phone.replaceAll("[\\s\\-.()]", "");
        if (normalized.startsWith("+34")) {
            normalized = normalized.substring(3);
        } else if (normalized.startsWith("0034")) {
            normalized = normalized.substring(4);
        }
        return normalized;
    }
}
