package com.project.planner.dto;

public class UserInputDTO {
    private String name;
    private int age;
    private String sex;
    private double heightCm;
    private double weightKg;
    private String activityLevel;
    private String experience;
    private String goal;

    private boolean vegetarian;
    private boolean vegan;
    private boolean lactoseFree;
    private boolean glutenFree;
    private boolean halal;

    private String allergies;
    private String dislikedIngredients;
    private String preferredCuisines;

    private boolean hasGym;
    private boolean hasDumbbells;
    private boolean hasResistanceBands;
    private boolean hasYogaMat;
    private boolean canRunOutside;

    private int workoutDaysPerWeek;
    private int minutesPerWorkout;
    private double dailyFoodBudget;

    private String region;

    // ✅ Getters and Setters (generate or use Lombok later)

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

    public boolean isVegetarian() { return vegetarian; }
    public void setVegetarian(boolean vegetarian) { this.vegetarian = vegetarian; }

    public boolean isVegan() { return vegan; }
    public void setVegan(boolean vegan) { this.vegan = vegan; }

    public boolean isLactoseFree() { return lactoseFree; }
    public void setLactoseFree(boolean lactoseFree) { this.lactoseFree = lactoseFree; }

    public boolean isGlutenFree() { return glutenFree; }
    public void setGlutenFree(boolean glutenFree) { this.glutenFree = glutenFree; }

    public boolean isHalal() { return halal; }
    public void setHalal(boolean halal) { this.halal = halal; }

    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }

    public String getDislikedIngredients() { return dislikedIngredients; }
    public void setDislikedIngredients(String dislikedIngredients) { this.dislikedIngredients = dislikedIngredients; }

    public String getPreferredCuisines() { return preferredCuisines; }
    public void setPreferredCuisines(String preferredCuisines) { this.preferredCuisines = preferredCuisines; }

    public boolean isHasGym() { return hasGym; }
    public void setHasGym(boolean hasGym) { this.hasGym = hasGym; }

    public boolean isHasDumbbells() { return hasDumbbells; }
    public void setHasDumbbells(boolean hasDumbbells) { this.hasDumbbells = hasDumbbells; }

    public boolean isHasResistanceBands() { return hasResistanceBands; }
    public void setHasResistanceBands(boolean hasResistanceBands) { this.hasResistanceBands = hasResistanceBands; }

    public boolean isHasYogaMat() { return hasYogaMat; }
    public void setHasYogaMat(boolean hasYogaMat) { this.hasYogaMat = hasYogaMat; }

    public boolean isCanRunOutside() { return canRunOutside; }
    public void setCanRunOutside(boolean canRunOutside) { this.canRunOutside = canRunOutside; }

    public int getWorkoutDaysPerWeek() { return workoutDaysPerWeek; }
    public void setWorkoutDaysPerWeek(int workoutDaysPerWeek) { this.workoutDaysPerWeek = workoutDaysPerWeek; }

    public int getMinutesPerWorkout() { return minutesPerWorkout; }
    public void setMinutesPerWorkout(int minutesPerWorkout) { this.minutesPerWorkout = minutesPerWorkout; }

    public double getDailyFoodBudget() { return dailyFoodBudget; }
    public void setDailyFoodBudget(double dailyFoodBudget) { this.dailyFoodBudget = dailyFoodBudget; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
}
