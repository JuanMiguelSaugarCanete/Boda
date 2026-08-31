package com.boda.bousers.services;

import com.boda.bousers.model.Family;
import com.boda.bousers.model.Person;
import com.boda.bousers.repository.FamilyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
@Service
@RequiredArgsConstructor
public class FamilyService {

    private final FamilyRepository familyRepository;

    public List<Family> getAllFamilies() {
        return familyRepository.findAll().stream().map(this::ensurePersonIds).toList();
    }

    public List<Family> getFamiliesByGroup(String group) {
        return familyRepository.findByGroup(group).stream().map(this::ensurePersonIds).toList();
    }

    public Family createFamily(Family family) {
        Person.ensureIds(family.getPeople());
        return familyRepository.save(family);
    }

    public Optional<Family> updateFamily(String id, Family familyDetails) {
        return familyRepository.findById(id).map(existingFamily -> {
            existingFamily.setIdfamily(familyDetails.getIdfamily());
            existingFamily.setGroup(familyDetails.getGroup());
            existingFamily.setPeople(familyDetails.getPeople());
            return familyRepository.save(existingFamily);
        });
    }

    public Optional<Family> getFamilyByPhone(String tlf) {
        return familyRepository.findByPeopleTlf(tlf).map(this::ensurePersonIds);
    }

    public boolean deleteFamily(String id) {
        if (familyRepository.existsById(id)) {
            familyRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<Family> getFamilyById(Long id) {
        return familyRepository.findByIdfamily(id).map(this::ensurePersonIds);
    }

    // Asigna una mesa a una persona concreta dentro de la familia.
    public Person assignTable(String id, String personId, String table) {
        Family family = familyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La familia no existe."));
        Person person = family.getPeople().stream()
                .filter(p -> personId.equals(p.getPersonId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La persona no existe en esa familia."));
        person.setTable(table);
        familyRepository.save(family);
        return person;
    }

    // Asigna personId a los miembros legacy y persiste el cambio una sola vez.
    private Family ensurePersonIds(Family family) {
        if (family != null && Person.ensureIds(family.getPeople())) {
            return familyRepository.save(family);
        }
        return family;
    }
}
