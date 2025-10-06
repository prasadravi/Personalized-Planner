package com.project.planner.model;

import java.util.HashSet;
import java.util.Set;

public class UserProfile {

    private String name;
    private int age;
    private String sex;
    private double heightCm;
    private double weightKg;
    private String activityLevel;
    private String experience;
    private String goal;
    private String region;

    private DietPreference diet = new DietPreference();
    private Equipment equipment = new Equipment();
    private Schedule schedule = new Schedule();

    // Getters & Setters

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }

    public double getHeightCm() { return heightCm; }
    public void setHeightCm(double heightCm) { this.heightCm = heightCm; }

    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }

    public String getActivityLevel() { return activityLevel; }
    public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public DietPreference getDiet() { return diet; }
    public void setDiet(DietPreference diet) { this.diet = diet; }

    public Equipment getEquipment() { return equipment; }
    public void setEquipment(Equipment equipment) { this.equipment = equipment; }

    public Schedule getSchedule() { return schedule; }
    public void setSchedule(Schedule schedule) { this.schedule = schedule; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
}
