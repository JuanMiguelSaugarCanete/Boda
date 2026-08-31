package com.boda.bousers.services;

import com.boda.bousers.model.Family;
import com.boda.bousers.model.Person;
import com.boda.bousers.model.PersonUpdateRequest;
import com.boda.bousers.repository.FamilyRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String TOKEN = "Bearer token";
    private static final String EMAIL = "padre@test.com";
    private static final String MEMBER_ID = "person-1";

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private Claims claims;

    private UserService userService;

    private Family family;

    private Person padre;
    private Person hijo;

    @BeforeEach
    void setUp() {
        userService = new UserService(familyRepository, jwtService);

        padre = new Person();
        padre.setPersonId(MEMBER_ID);
        padre.setName("Padre");
        padre.setAdult(true);
        padre.setComes(true);
        padre.setDiet("NINGUNA");
        padre.setTable("Mesa 1");
        padre.setInvitationSent(true);
        padre.setTlf("600111222");
        padre.setEmail(EMAIL);
        padre.setAttendance("SI");
        padre.setAllergens(List.of());

        hijo = new Person();
        hijo.setPersonId("person-2");
        hijo.setName("Hijo");
        hijo.setAdult(false);
        hijo.setComes(true);
        hijo.setDiet("NINGUNA");
        hijo.setTable("Mesa 1");
        hijo.setInvitationSent(false);
        hijo.setTlf("600333444");
        hijo.setAttendance("AUN_NO_SE");
        hijo.setAllergens(List.of("GLUTEN"));

        family = new Family("family-1", 1L, "FAMILIA NOVIO", List.of(padre, hijo));

        when(jwtService.validateToken("token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn(EMAIL);
        when(familyRepository.findByPeopleEmail(EMAIL)).thenReturn(Optional.of(family));
    }

    @Test
    void updatesExistingFamilyMember() {
        PersonUpdateRequest request = new PersonUpdateRequest();
        request.setName("Hijo Modificado");
        request.setAttendance("NO");
        request.setAllergens(List.of("FISH"));

        userService.updateFamilyMember(TOKEN, "person-2", request);

        assertEquals("Hijo Modificado", hijo.getName());
        assertEquals("NO", hijo.getAttendance());
        assertEquals(List.of("FISH"), hijo.getAllergens());
        verify(familyRepository).save(family);
    }

    @Test
    void rejectsMemberThatDoesNotBelongToFamily() {
        PersonUpdateRequest request = new PersonUpdateRequest();
        request.setName("Intruso");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.updateFamilyMember(TOKEN, "other-person", request));

        assertTrue(ex.getMessage().contains("no pertenece a tu familia"));
        verify(familyRepository, never()).save(any(Family.class));
    }

    @Test
    void rejectsInvalidAttendance() {
        PersonUpdateRequest request = new PersonUpdateRequest();
        request.setAttendance("QUIZAS");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.updateFamilyMember(TOKEN, MEMBER_ID, request));

        assertTrue(ex.getMessage().contains("asistirás"));
        verify(familyRepository, never()).save(any(Family.class));
    }

    @Test
    void rejectsPhoneAlreadyUsedByAnotherGuest() {
        when(familyRepository.findByPeopleTlf("611999888")).thenReturn(Optional.of(nonFamily()));
        PersonUpdateRequest request = new PersonUpdateRequest();
        request.setTlf("611 999 888");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.updateFamilyMember(TOKEN, "person-2", request));

        assertTrue(ex.getMessage().contains("ya está registrado"));
        verify(familyRepository, never()).save(any(Family.class));
    }

    @Test
    void updatesLoggedPersonPhone() {
        when(familyRepository.findByPeopleTlf("600777888")).thenReturn(Optional.empty());
        PersonUpdateRequest request = new PersonUpdateRequest();
        request.setTlf("600777888");

        userService.updateMe(TOKEN, request);

        assertEquals("600777888", padre.getTlf());
        verify(familyRepository).save(family);
    }

    @Test
    void rejectsPhoneDuplicateInsideSameFamily() {
        PersonUpdateRequest request = new PersonUpdateRequest();
        request.setTlf("600333444");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.updateFamilyMember(TOKEN, MEMBER_ID, request));

        assertTrue(ex.getMessage().contains("otro miembro de tu familia"));
        verify(familyRepository, never()).save(any(Family.class));
    }

    private Family nonFamily() {
        Family other = new Family();
        other.setId("family-999");
        Person stranger = new Person();
        stranger.setTlf("611999888");
        other.setPeople(List.of(stranger));
        return other;
    }
}
