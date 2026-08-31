package com.boda.bousers.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class Person {

    private String personId;

    private String name;
    private boolean adult;
    private boolean comes;
    private String diet;
    private String table;

    private boolean invitationSent;

    private String tlf;

    private String email;

    private String imgProfile;

    // Hash BCrypt: nunca se serializa en las respuestas JSON
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    // SI / NO / AUN_NO_SE
    private String attendance;

    private List<String> allergens;

    private String dietNotes;

    // Asigna un identificador único a cada persona que no lo tenga.
    // Devuelve true si se generó algún id (para saber si hay que persistir).
    public static boolean ensureIds(List<Person> people) {
        if (people == null) {
            return false;
        }
        boolean changed = false;
        for (Person person : people) {
            if (person.getPersonId() == null || person.getPersonId().isBlank()) {
                person.setPersonId(UUID.randomUUID().toString());
                changed = true;
            }
        }
        return changed;
    }

}
