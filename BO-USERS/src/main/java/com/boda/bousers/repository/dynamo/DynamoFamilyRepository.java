package com.boda.bousers.repository.dynamo;

import com.boda.bousers.model.Family;

import java.util.List;
import java.util.Optional;

public interface DynamoFamilyRepository {
    List<Family> findAll();

    List<Family> findByGroup(String group);

    Optional<Family> findByPeopleTlf(String tlf);

    Optional<Family> findByPeopleEmail(String email);

    Optional<Family> findByIdfamily(Long idfamily);

    Optional<Family> findById(String id);

    Family save(Family family);

    void deleteById(String id);

    boolean existsById(String id);
}
