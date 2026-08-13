package com.tubacelik.myapp;

/**
 * Model-Klasse für einen Skill.
 * Sie beschreibt, welche Daten ein Skill besitzt.
 */
public class Skill {

    private String skillId;
    private String title;
    private String category;
    private String description;
    private String level;
    private String ownerId;
    private String ownerName;

    /*
     * Leerer Konstruktor wird von Firestore benötigt,
     * um Dokumente in Java-Objekte umzuwandeln.
     */
    public Skill() {
    }

    public Skill(
            String skillId,
            String title,
            String category,
            String description,
            String level,
            String ownerId,
            String ownerName
    ) {
        this.skillId = skillId;
        this.title = title;
        this.category = category;
        this.description = description;
        this.level = level;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
    }

    public String getSkillId() {
        return skillId;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getLevel() {
        return level;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }
}