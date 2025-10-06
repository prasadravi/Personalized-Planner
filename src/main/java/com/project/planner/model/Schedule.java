package com.project.planner.model;

public class Schedule {
    private int workoutDaysPerWeek;
    private int minutesPerWorkout;
    private double dailyFoodBudget;

    public int getWorkoutDaysPerWeek() { return workoutDaysPerWeek; }
    public void setWorkoutDaysPerWeek(int workoutDaysPerWeek) { this.workoutDaysPerWeek = workoutDaysPerWeek; }

    public int getMinutesPerWorkout() { return minutesPerWorkout; }
    public void setMinutesPerWorkout(int minutesPerWorkout) { this.minutesPerWorkout = minutesPerWorkout; }

    public double getDailyFoodBudget() { return dailyFoodBudget; }
    public void setDailyFoodBudget(double dailyFoodBudget) { this.dailyFoodBudget = dailyFoodBudget; }
}
