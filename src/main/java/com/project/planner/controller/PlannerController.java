package com.project.planner.controller;

import com.project.planner.dto.UserInputDTO;
import com.project.planner.logic.PlannerEngine;
import com.project.planner.logic.PlannerEngine.WeeklyPlan;
import com.project.planner.model.UserProfile;
import com.project.planner.service.PlannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

@Controller
public class PlannerController {

    @Autowired
    private PlannerService plannerService;

    @GetMapping("/")
    public String showForm() {
        return "index";
    }

    @PostMapping("/generate")
    public String generatePlan(UserInputDTO userInputDTO, Model model) {
        UserProfile profile = plannerService.mapToUserProfile(userInputDTO);

        WeeklyPlan weeklyPlan = plannerService.generateWeeklyPlan(profile);
        Map<String, Integer> shoppingList = plannerService.aggregateShoppingList(weeklyPlan);

        model.addAttribute("profile", profile);
        model.addAttribute("dailyCalories", weeklyPlan.days.size() > 0 ? weeklyPlan.days.get(0).targetCalories : 0);
        model.addAttribute("dailyBudget", profile.getSchedule().getDailyFoodBudget());
        model.addAttribute("weeklyBudget", profile.getSchedule().getDailyFoodBudget() * 7);
        model.addAttribute("weeklyCost", weeklyPlan.weeklyCost);
        model.addAttribute("weeklyPlan", weeklyPlan);
        model.addAttribute("shoppingList", shoppingList);

        return "result";
    }
}
