package com.project.planner.service;

import com.project.planner.dto.UserInputDTO;
import com.project.planner.logic.PlannerEngine;
import com.project.planner.logic.PlannerEngine.WeeklyPlan;
import com.project.planner.model.UserProfile;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PlannerService {

    private final PlannerEngine engine = new PlannerEngine();

    public UserProfile mapToUserProfile(UserInputDTO dto) {
        UserProfile user = new UserProfile();

        user.setName(dto.getName());
        user.setAge(dto.getAge());
        user.setSex(dto.getSex());
        user.setHeightCm(dto.getHeightCm());
        user.setWeightKg(dto.getWeightKg());
        user.setActivityLevel(dto.getActivityLevel());
        user.setExperience(dto.getExperience());
        user.setGoal(dto.getGoal());
        user.setRegion(dto.getRegion());

        user.getDiet().setVegetarian(dto.isVegetarian());
        user.getDiet().setVegan(dto.isVegan());
        user.getDiet().setLactoseFree(dto.isLactoseFree());
        user.getDiet().setGlutenFree(dto.isGlutenFree());
        user.getDiet().setHalal(dto.isHalal());

        if (dto.getAllergies() != null && !dto.getAllergies().isBlank()) {
            for (String a : dto.getAllergies().split(",")) user.getDiet().getAllergies().add(a.trim().toLowerCase());
        }
        if (dto.getDislikedIngredients() != null && !dto.getDislikedIngredients().isBlank()) {
            for (String d : dto.getDislikedIngredients().split(",")) user.getDiet().getDislikedIngredients().add(d.trim().toLowerCase());
        }
        if (dto.getPreferredCuisines() != null && !dto.getPreferredCuisines().isBlank()) {
            for (String c : dto.getPreferredCuisines().split(",")) user.getDiet().getPreferredCuisines().add(c.trim());
        }

        user.getEquipment().setHasGym(dto.isHasGym());
        user.getEquipment().setHasDumbbells(dto.isHasDumbbells());
        user.getEquipment().setHasResistanceBands(dto.isHasResistanceBands());
        user.getEquipment().setHasYogaMat(dto.isHasYogaMat());
        user.getEquipment().setCanRunOutside(dto.isCanRunOutside());

        user.getSchedule().setWorkoutDaysPerWeek(dto.getWorkoutDaysPerWeek());
        user.getSchedule().setMinutesPerWorkout(dto.getMinutesPerWorkout());
        user.getSchedule().setDailyFoodBudget(dto.getDailyFoodBudget());

        return user;
    }

    public WeeklyPlan generateWeeklyPlan(UserProfile profile) {
        return engine.generateWeeklyPlan(profile);
    }

    public Map<String, Integer> aggregateShoppingList(WeeklyPlan plan) {
        Map<String, Integer> counts = new HashMap<>();
        if (plan == null) return counts;
        for (PlannerEngine.DayPlan d : plan.days) {
            for (PlannerEngine.Meal m : d.meals) {
                for (String ing : m.ingredients) counts.merge(ing, 1, Integer::sum);
            }
        }
        return counts;
    }
}
