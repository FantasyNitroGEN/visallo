package org.visallo.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;

public class ClientApiDirectorySearchResponse implements ClientApiObject {
    private List<String> people = new ArrayList<String>();
    private List<String> groups = new ArrayList<String>();

    public void addPeople(List<String> people) {
        this.people.addAll(people);
    }

    public void addGroups(List<String> groups) {
        this.groups.addAll(groups);
    }

    public List<String> getPeople() {
        return people;
    }

    public List<String> getGroups() {
        return groups;
    }
}
