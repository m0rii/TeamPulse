package com.example.slackbot.domain;

import java.util.HashSet;
import java.util.Set;

public class Team {
    private String id;
    private String name;
    private String description;
    private Set<String> memberIds = new HashSet<>();
    private Set<String> managerIds = new HashSet<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(Set<String> memberIds) {
        this.memberIds = memberIds;
    }

    public void addMember(String memberId) {
        memberIds.add(memberId);
    }

    public void removeMember(String memberId) {
        memberIds.remove(memberId);
    }

    public boolean isMember(String userId) {
        return memberIds.contains(userId);
    }

    public Set<String> getManagerIds() {
        return managerIds;
    }

    public void setManagerIds(Set<String> managerIds) {
        this.managerIds = managerIds;
    }
    
    public void addManager(String managerId) {
        managerIds.add(managerId);
        // Ensure the manager is also a member
        addMember(managerId);
    }
    
    public void removeManager(String managerId) {
        managerIds.remove(managerId);
    }

    public boolean isManager(String userId) {
        return userId != null && managerIds.contains(userId);
    }
    
    public boolean hasManagers() {
        return !managerIds.isEmpty();
    }
} 