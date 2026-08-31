package com.boda.bousers.services;


import com.boda.bousers.model.Family;
import com.boda.bousers.model.LoginRequest;
import com.boda.bousers.model.LoginResponse;
import com.boda.bousers.model.Person;
import com.boda.bousers.model.RegisterCompleteRequest;
import com.boda.bousers.model.RegisterVerifyRequest;
import com.boda.bousers.model.RegisterVerifyResponse;
import com.boda.bousers.model.SessionResponse;
import com.boda.bousers.repository.FamilyRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final List<String> VALID_ATTENDANCE = List.of("SI", "NO", "AUN_NO_SE");
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final FamilyRepository familyRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${register.general-password:}")
    private String registerGeneralPassword;

    // POST /api/auth/login
    public LoginResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());

        Family family = familyRepository.findByPeopleEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email o contraseña incorrectos."));

        Person person = findPersonByEmail(family, email)
                .orElseThrow(() -> new IllegalArgumentException("Email o contraseña incorrectos."));

        if (person.getPassword() == null || !passwordEncoder.matches(request.getPassword(), person.getPassword())) {
            throw new IllegalArgumentException("Email o contraseña incorrectos.");
        }

        String token = jwtService.generateToken(email, family.getId());
        return new LoginResponse(token, family);
    }

    // POST /api/auth/register/verify
    public RegisterVerifyResponse verifyRegistration(RegisterVerifyRequest request) {
        Family family = findFamilyByPhone(request.getPhone());
        checkGeneralPassword(request.getGeneralPassword());

        Person person = findPersonByPhone(family, normalizePhone(request.getPhone()))
                .orElseThrow(() -> new IllegalArgumentException("El número de teléfono no está registrado en la lista de invitados."));

        boolean alreadyRegistered = person.getEmail() != null && !person.getEmail().isBlank();
        return new RegisterVerifyResponse(person.getName(), alreadyRegistered);
    }

    // POST /api/auth/register/complete
    public LoginResponse completeRegistration(RegisterCompleteRequest request) {
        Family family = findFamilyByPhone(request.getPhone());
        checkGeneralPassword(request.getGeneralPassword());

        String phone = normalizePhone(request.getPhone());
        Person person = findPersonByPhone(family, phone)
                .orElseThrow(() -> new IllegalArgumentException("El número de teléfono no está registrado en la lista de invitados."));

        String email = normalizeEmail(request.getEmail());
        validateCompleteRequest(request, email);
        checkEmailNotUsedByOther(family, person, email);

        if (request.getName() != null && !request.getName().isBlank()) {
            person.setName(request.getName().trim());
        }
        person.setEmail(email);
        person.setPassword(passwordEncoder.encode(request.getPassword()));
        person.setAttendance(request.getAttendance());
        person.setAllergens(request.getAllergens() != null ? request.getAllergens() : List.of());
        person.setDiet(request.getDiet());
        person.setDietNotes(request.getDietNotes());
        if (request.getImgProfile() != null) {
            person.setImgProfile(request.getImgProfile().isBlank() ? null : request.getImgProfile().trim());
        }

        familyRepository.save(family);

        String token = jwtService.generateToken(email, family.getId());
        return new LoginResponse(token, family);
    }

    // GET /api/auth/me — valida el token y devuelve la sesión con datos frescos
    public SessionResponse getSession(String authHeader) {
        String token = extractToken(authHeader);

        Claims claims;
        try {
            claims = jwtService.validateToken(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("El token es inválido o ha expirado.");
        }

        String email = normalizeEmail(claims.getSubject());
        Family family = familyRepository.findByPeopleEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("La sesión ya no es válida."));

        if (Person.ensureIds(family.getPeople())) {
            family = familyRepository.save(family);
        }

        return new SessionResponse(email, family);
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Token no proporcionado.");
        }
        return authHeader.substring(7).trim();
    }

    private Family findFamilyByPhone(String phone) {
        String normalized = normalizePhone(phone);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("El número de teléfono no está registrado en la lista de invitados.");
        }
        return familyRepository.findByPeopleTlf(normalized)
                .orElseThrow(() -> new IllegalArgumentException("El número de teléfono no está registrado en la lista de invitados."));
    }

    private void checkGeneralPassword(String generalPassword) {
        String provided = generalPassword == null ? "" : generalPassword.trim();
        if (registerGeneralPassword == null || registerGeneralPassword.isBlank()
                || !registerGeneralPassword.equals(provided)) {
            throw new IllegalArgumentException("La contraseña de invitación no es correcta.");
        }
    }

    private void validateCompleteRequest(RegisterCompleteRequest request, String email) {
        if (email.isBlank() || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("El email no es válido.");
        }
        if (request.getPassword() == null || request.getPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres.");
        }
        if (request.getAttendance() == null || !VALID_ATTENDANCE.contains(request.getAttendance())) {
            throw new IllegalArgumentException("Debes indicar si asistirás.");
        }
    }

    private void checkEmailNotUsedByOther(Family family, Person person, String email) {
        Optional<Family> existing = familyRepository.findByPeopleEmail(email);
        if (existing.isPresent()) {
            boolean isSamePerson = existing.get().getId().equals(family.getId())
                    && findPersonByEmail(existing.get(), email)
                        .map(p -> normalizePhone(p.getTlf()).equals(normalizePhone(person.getTlf())))
                        .orElse(false);
            if (!isSamePerson) {
                throw new IllegalArgumentException("Ese email ya está registrado por otro invitado.");
            }
        }
    }

    private Optional<Person> findPersonByPhone(Family family, String phone) {
        if (family.getPeople() == null) {
            return Optional.empty();
        }
        return family.getPeople().stream()
                .filter(p -> p.getTlf() != null && normalizePhone(p.getTlf()).equals(phone))
                .findFirst();
    }

    private Optional<Person> findPersonByEmail(Family family, String email) {
        if (family.getPeople() == null) {
            return Optional.empty();
        }
        return family.getPeople().stream()
                .filter(p -> p.getEmail() != null && normalizeEmail(p.getEmail()).equals(email))
                .findFirst();
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

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
