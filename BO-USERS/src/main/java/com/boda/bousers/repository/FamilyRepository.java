package com.boda.bousers.repository;

import com.boda.bousers.model.Family;
import com.boda.bousers.repository.dynamo.DynamoFamilyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FamilyRepository {

    private final DynamoFamilyRepository dynamoFamilyRepository;

    public List<Family> findAll() {
        return dynamoFamilyRepository.findAll();
    }

    public List<Family> findByGroup(String group) {
        return dynamoFamilyRepository.findByGroup(group);
    }

    public Optional<Family> findByPeopleTlf(String tlf) {
        return dynamoFamilyRepository.findByPeopleTlf(tlf);
    }

    public Optional<Family> findByPeopleEmail(String email) {
        return dynamoFamilyRepository.findByPeopleEmail(email);
    }

    public Optional<Family> findByIdfamily(Long idfamily) {
        return dynamoFamilyRepository.findByIdfamily(idfamily);
    }

    public Optional<Family> findById(String id) {
        return dynamoFamilyRepository.findById(id);
    }

    public Family save(Family family) {
        return dynamoFamilyRepository.save(family);
    }

    public boolean existsById(String id) {
        return dynamoFamilyRepository.existsById(id);
    }

    public void deleteById(String id) {
        dynamoFamilyRepository.deleteById(id);
    }
}
