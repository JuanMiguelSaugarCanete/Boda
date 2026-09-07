package com.boda.bousers.repository.dynamo;

import com.boda.bousers.model.Family;
import com.boda.bousers.model.Person;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class DynamoFamilyRepositoryImpl implements DynamoFamilyRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final String gsiEmail;
    private final String gsiTlf;
    private final String gsiGroup;

    public DynamoFamilyRepositoryImpl(DynamoDbClient dynamoDbClient,
                                      @Value("${aws.dynamodb.table}") String tableName,
                                      @Value("${aws.dynamodb.gsi-email}") String gsiEmail,
                                      @Value("${aws.dynamodb.gsi-tlf}") String gsiTlf,
                                      @Value("${aws.dynamodb.gsi-group}") String gsiGroup) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
        this.gsiEmail = gsiEmail;
        this.gsiTlf = gsiTlf;
        this.gsiGroup = gsiGroup;
    }

    @Override
    public List<Family> findAll() {
        return dynamoDbClient.scanPaginator(ScanRequest.builder().tableName(tableName).build())
                .items()
                .stream()
                .collect(Collectors.groupingBy(this::familyIdKey, LinkedHashMap::new, Collectors.toList()))
                .values()
                .stream()
                .map(this::toFamily)
                .toList();
    }

    @Override
    public List<Family> findByGroup(String group) {
        return queryByIndex(gsiGroup, "group", group).stream()
                .collect(Collectors.groupingBy(this::familyIdKey, LinkedHashMap::new, Collectors.toList()))
                .values()
                .stream()
                .map(this::toFamily)
                .toList();
    }

    @Override
    public Optional<Family> findByPeopleTlf(String tlf) {
        return queryByIndex(gsiTlf, "tlf", normalizePhone(tlf)).stream()
                .collect(Collectors.groupingBy(this::familyIdKey, LinkedHashMap::new, Collectors.toList()))
                .values()
                .stream()
                .findFirst()
                .map(this::toFamily);
    }

    @Override
    public Optional<Family> findByPeopleEmail(String email) {
        return queryByIndex(gsiEmail, "email", normalizeEmail(email)).stream()
                .collect(Collectors.groupingBy(this::familyIdKey, LinkedHashMap::new, Collectors.toList()))
                .values()
                .stream()
                .findFirst()
                .map(this::toFamily);
    }

    @Override
    public Optional<Family> findByIdfamily(Long idfamily) {
        List<Map<String, AttributeValue>> items = queryByFamilyId(idfamily);
        if (items.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toFamily(items));
    }

    @Override
    public Optional<Family> findById(String id) {
        List<Map<String, AttributeValue>> items = queryByFamilyId(parseFamilyId(id));
        if (items.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toFamily(items));
    }

    @Override
    public Family save(Family family) {
        if (family.getIdfamily() == null) {
            throw new IllegalArgumentException("idfamily is required.");
        }
        String familyId = String.valueOf(family.getIdfamily());
        List<Person> people = family.getPeople() == null ? List.of() : family.getPeople();

        deleteFamilyItems(familyId);
        for (Person person : people) {
            if (person.getPersonId() == null || person.getPersonId().isBlank()) {
                person.setPersonId(UUID.randomUUID().toString());
            }
            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName(tableName)
                    .item(toItem(family, person))
                    .build());
        }

        family.setId(familyId);
        return family;
    }

    @Override
    public void deleteById(String id) {
        queryByFamilyId(parseFamilyId(id)).forEach(item -> dynamoDbClient.deleteItem(DeleteItemRequest.builder()
                .tableName(tableName)
                .key(keyOf(item))
                .build()));
    }

    @Override
    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    private void deleteFamilyItems(String familyId) {
        queryByFamilyId(parseFamilyId(familyId)).forEach(item -> dynamoDbClient.deleteItem(DeleteItemRequest.builder()
                .tableName(tableName)
                .key(keyOf(item))
                .build()));
    }

    private Long parseFamilyId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("familyId is required.");
        }
        return Long.parseLong(id.trim());
    }

    private List<Map<String, AttributeValue>> queryByFamilyId(Long familyId) {
        return dynamoDbClient.queryPaginator(QueryRequest.builder()
                        .tableName(tableName)
                        .keyConditionExpression("familyId = :familyId")
                        .expressionAttributeValues(Map.of(":familyId", AttributeValue.builder().n(String.valueOf(familyId)).build()))
                        .build())
                .items()
                .stream()
                .toList();
    }

    private List<Map<String, AttributeValue>> queryByIndex(String indexName, String field, String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return dynamoDbClient.queryPaginator(QueryRequest.builder()
                        .tableName(tableName)
                        .indexName(indexName)
                        .keyConditionExpression(field + " = :value")
                        .expressionAttributeValues(Map.of(":value", AttributeValue.builder().s(value).build()))
                        .build())
                .items()
                .stream()
                .toList();
    }

    private Family toFamily(List<Map<String, AttributeValue>> items) {
        if (items.isEmpty()) {
            return null;
        }
        Map<String, AttributeValue> first = items.get(0);
        Family family = new Family();
        family.setId(asString(first.get("familyId")));
        family.setIdfamily(Long.parseLong(asString(first.get("familyId"))));
        family.setGroup(asString(first.get("group")));
        family.setPeople(items.stream().map(this::toPerson).toList());
        return family;
    }

    private Family toFamily(Map<String, AttributeValue> item) {
        return toFamily(List.of(item));
    }

    private Person toPerson(Map<String, AttributeValue> item) {
        Person person = new Person();
        person.setPersonId(asString(item.get("personId")));
        person.setName(asString(item.get("name")));
        person.setAdult(asBoolean(item.get("adult")));
        person.setComes(asBoolean(item.get("comes")));
        person.setDiet(asString(item.get("diet")));
        person.setTable(asString(item.get("table")));
        person.setInvitationSent(asBoolean(item.get("invitation_sent")));
        person.setTlf(asString(item.get("tlf")));
        person.setEmail(asString(item.get("email")));
        person.setImgProfile(asString(item.get("imgProfile")));
        person.setPassword(asString(item.get("password")));
        person.setAttendance(asString(item.get("attendance")));
        person.setAllergens(asStringList(item.get("allergens")));
        person.setDietNotes(asString(item.get("diet_notes")));
        return person;
    }

    private Map<String, AttributeValue> toItem(Family family, Person person) {
        Map<String, AttributeValue> item = new LinkedHashMap<>();
        item.put("familyId", AttributeValue.builder().n(String.valueOf(family.getIdfamily())).build());
        item.put("personId", AttributeValue.builder().s(person.getPersonId()).build());
        putString(item, "group", family.getGroup());
        putString(item, "name", person.getName());
        putBool(item, "adult", person.isAdult());
        putBool(item, "comes", person.isComes());
        putString(item, "diet", person.getDiet());
        putString(item, "table", person.getTable());
        putBool(item, "invitation_sent", person.isInvitationSent());
        putString(item, "tlf", normalizePhone(person.getTlf()));
        putString(item, "email", normalizeEmail(person.getEmail()));
        putString(item, "imgProfile", person.getImgProfile());
        putString(item, "password", person.getPassword());
        putString(item, "attendance", person.getAttendance());
        putStringList(item, "allergens", person.getAllergens());
        putString(item, "diet_notes", person.getDietNotes());
        return item;
    }

    private Map<String, AttributeValue> keyOf(Map<String, AttributeValue> item) {
        return Map.of(
                "familyId", item.get("familyId"),
                "personId", item.get("personId")
        );
    }

    private void putString(Map<String, AttributeValue> item, String key, String value) {
        if (value != null && !value.isBlank()) {
            item.put(key, AttributeValue.builder().s(value).build());
        }
    }

    private void putBool(Map<String, AttributeValue> item, String key, boolean value) {
        item.put(key, AttributeValue.builder().bool(value).build());
    }

    private void putStringList(Map<String, AttributeValue> item, String key, List<String> values) {
        if (values != null && !values.isEmpty()) {
            item.put(key, AttributeValue.builder().l(values.stream()
                    .filter(v -> v != null && !v.isBlank())
                    .map(v -> AttributeValue.builder().s(v).build())
                    .toList()).build());
        }
    }

    private String familyIdKey(Map<String, AttributeValue> item) {
        return asString(item.get("familyId"));
    }

    private String asString(AttributeValue value) {
        if (value == null) {
            return null;
        }
        if (value.s() != null) {
            return value.s();
        }
        if (value.n() != null) {
            return value.n();
        }
        return null;
    }

    private boolean asBoolean(AttributeValue value) {
        return value != null && Boolean.TRUE.equals(value.bool());
    }

    private List<String> asStringList(AttributeValue value) {
        if (value == null || value.l() == null) {
            return List.of();
        }
        return value.l().stream()
                .map(this::asString)
                .filter(v -> v != null && !v.isBlank())
                .toList();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String normalized = phone.replaceAll("[\\s\\-.()]", "");
        if (normalized.startsWith("+34")) {
            normalized = normalized.substring(3);
        } else if (normalized.startsWith("0034")) {
            normalized = normalized.substring(4);
        }
        return normalized.isBlank() ? null : normalized;
    }
}
