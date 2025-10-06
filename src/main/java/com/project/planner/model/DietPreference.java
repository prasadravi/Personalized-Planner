package com.project.planner.model;

import java.util.HashSet;
import java.util.Set;

public class DietPreference {
    private boolean vegetarian;
    private boolean vegan;
    private boolean lactoseFree;
    private boolean glutenFree;
    private boolean halal;
    private Set<String> allergies = new HashSet<>();
    private Set<String> dislikedIngredients = new HashSet<>();
    private Set<String> preferredCuisines = new HashSet<>();

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

    public Set<String> getAllergies() { return allergies; }
    public void setAllergies(Set<String> allergies) { this.allergies = allergies; }

    public Set<String> getDislikedIngredients() { return dislikedIngredients; }
    public void setDislikedIngredients(Set<String> dislikedIngredients) { this.dislikedIngredients = dislikedIngredients; }

    public Set<String> getPreferredCuisines() { return preferredCuisines; }
    public void setPreferredCuisines(Set<String> preferredCuisines) { this.preferredCuisines = preferredCuisines; }
}
