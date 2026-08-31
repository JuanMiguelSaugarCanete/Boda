package com.boda.bousers.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class Family {

    private String id;

    private Long idfamily;

    private String group;

    private List<Person> people;

    // Constructores
    public Family() {}

    public Family(String id, Long idfamily, String group, List<Person> people) {
        this.id = id;
        this.idfamily = idfamily;
        this.group = group;
        this.people = people;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getIdfamily() {
        return idfamily;
    }

    public void setIdfamily(Long idfamily) {
        this.idfamily = idfamily;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public List<Person> getPeople() {
        return people;
    }

    public void setPeople(List<Person> people) {
        this.people = people;
    }
}
