package com.project.planner.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.project.planner.model.UserProfile;

/**
 * PlannerEngine
 * - Standalone engine that generates a WeeklyPlan from a UserProfile.
 * - Contains inner classes for Exercise, Meal, DayPlan, WeeklyPlan and a small Database loader.
 *
 * Note: This is a mostly direct port of the earlier single-file logic to a serviceable class.
 */
public class PlannerEngine {

    private final Random rng = new Random(42);
    private final List<Exercise> dbExercises = Database.exercises();
    private final List<Meal> dbMeals = Database.meals();

    public WeeklyPlan generateWeeklyPlan(UserProfile u) {
        WeeklyPlan wp = new WeeklyPlan();
        int targetKcal = targetCalories(u);
        wp.weeklyTargetCalories = targetKcal * 7;
        wp.weeklyBudget = u.getSchedule().getDailyFoodBudget() * 7.0;

        boolean[] workoutDays = pickWorkoutDays(u.getSchedule().getWorkoutDaysPerWeek());

        for (int day = 0; day < 7; day++) {
            DayPlan dp = new DayPlan();
            dp.targetCalories = targetKcal;
            dp.restDay = !workoutDays[day];
            if (!dp.restDay) {
                dp.workout = planWorkoutForDay(u);
            } else {
                dp.workout = new ArrayList<>();
            }
            planMealsForDay(u, dp);
            wp.days.add(dp);
        }

        wp.weeklyCost = wp.days.stream().mapToDouble(d -> d.cost).sum();
        return wp;
    }

    // ---------- core helpers ----------

    private int targetCalories(UserProfile u) {
        // Mifflin–St Jeor
        double s = ("FEMALE".equalsIgnoreCase(u.getSex())) ? -161 : 5;
        double bmr = 10 * u.getWeightKg() + 6.25 * u.getHeightCm() - 5 * u.getAge() + s;
        double activity = switch (u.getActivityLevel() == null ? "SEDENTARY" : u.getActivityLevel().toUpperCase()) {
            case "LIGHT" -> 1.375;
            case "MODERATE" -> 1.55;
            case "ACTIVE" -> 1.725;
            case "VERY_ACTIVE" -> 1.9;
            default -> 1.2;
        };
        double tdee = bmr * activity;
        double adj = switch (u.getGoal() == null ? "LOSE_FAT" : u.getGoal().toUpperCase()) {
            case "LOSE_FAT" -> -400;
            case "GAIN_MUSCLE" -> 300;
            default -> 0;
        };
        int target = (int) Math.round(tdee + adj);
        return Math.max(1400, Math.min(3500, target));
    }

    private boolean[] pickWorkoutDays(int n) {
        boolean[] arr = new boolean[7];
        int[][] patterns = {
                {1, 0, 1, 0, 1, 0, 0}, // 3
                {1, 0, 1, 0, 1, 0, 1}, // 4
                {1, 0, 1, 1, 0, 1, 0}, // 5
                {1, 1, 1, 0, 1, 1, 0}, // 6
        };
        if (n <= 3) arr = from(patterns[0]);
        else if (n == 4) arr = from(patterns[1]);
        else if (n == 5) arr = from(patterns[2]);
        else arr = from(patterns[3]);
        return arr;
    }

    private boolean[] from(int[] p) {
        boolean[] b = new boolean[7];
        for (int i = 0; i < 7; i++) b[i] = p[i] == 1;
        return b;
    }

    private List<Exercise> planWorkoutForDay(UserProfile u) {
        List<Exercise> pool = dbExercises.stream().filter(e -> equipOk(u, e)).collect(Collectors.toList());
        List<Exercise> plan = new ArrayList<>();

        addIfExists(pool, plan, "Jumping Jacks");
        addIfExists(pool, plan, "Plank");

        String[] groups = {"Push", "Pull", "Legs", "Full Body", "Cardio"};
        for (String g : groups) {
            pickByGroup(pool, plan, g, u.getExperience());
        }

        int targetMin = Math.max(10, u.getSchedule().getMinutesPerWorkout());
        int total = plan.stream().mapToInt(e -> e.estMinutes).sum();

        if (total > targetMin) {
            plan.sort(Comparator.comparingInt(e -> e.estMinutes)); // drop shortest first
            while (total > targetMin && plan.size() > 3) {
                Exercise rem = plan.remove(0);
                total -= rem.estMinutes;
            }
        }

        while (total < targetMin) {
            Optional<Exercise> add = pool.stream().filter(e -> e.muscleGroup.equals("Cardio") || e.muscleGroup.equals("Core"))
                    .skip(rng.nextInt(Math.max(1, Math.max(1, pool.size() / 4)))).findAny();
            if (add.isEmpty()) break;
            plan.add(add.get());
            total += add.get().estMinutes;
            if (plan.size() > 10) break;
        }

        return plan;
    }

    private boolean equipOk(UserProfile u, Exercise e) {
        switch (e.required) {
            case "gym":
                return u.getEquipment().isHasGym();
            case "dumbbells":
                return u.getEquipment().isHasDumbbells() || u.getEquipment().isHasGym();
            case "bands":
                return u.getEquipment().isHasResistanceBands() || u.getEquipment().isHasGym();
            default:
                return true;
        }
    }

    private void addIfExists(List<Exercise> pool, List<Exercise> plan, String name) {
        pool.stream().filter(ex -> ex.name.equals(name)).findFirst().ifPresent(plan::add);
    }

    private void pickByGroup(List<Exercise> pool, List<Exercise> plan, String group, String exp) {
        List<Exercise> g = pool.stream().filter(e -> e.muscleGroup.equals(group)).collect(Collectors.toList());
        if (g.isEmpty()) return;
        List<Exercise> lvl = g.stream().filter(e -> e.level.equalsIgnoreCase(exp == null ? "BEGINNER" : exp)).collect(Collectors.toList());
        List<Exercise> src = lvl.isEmpty() ? g : lvl;
        plan.add(src.get(rng.nextInt(src.size())));
    }

    private void planMealsForDay(UserProfile u, DayPlan d) {
        List<Meal> pool = dbMeals.stream()
                .filter(m -> dietOk(u, m))
                .filter(m -> cuisineOk(u, m))
                .collect(Collectors.toList());

        List<Meal> dayMeals = new ArrayList<>();

        pickTopBy(pool, dayMeals, m -> m.protein / (double) Math.max(1, m.calories), 1);
        pickTopBy(pool, dayMeals, m -> m.carbs / (double) Math.max(1, m.calories), 1);
        pickBreakfast(pool, dayMeals);
        pickSnack(pool, dayMeals);

        int target = d.targetCalories;
        double budget = u.getSchedule().getDailyFoodBudget();

        int kcal = dayMeals.stream().mapToInt(m -> m.calories).sum();
        double cost = dayMeals.stream().mapToDouble(m -> m.cost).sum();

        List<Meal> affordable = pool.stream().sorted(Comparator.comparingDouble(m -> m.cost)).collect(Collectors.toList());
        int safety = 0;
        while (kcal < target - 150 && cost <= budget && safety < 20) {
            Meal add = pickAffordable(affordable, target - kcal, budget - cost);
            if (add == null) break;
            dayMeals.add(add);
            kcal += add.calories;
            cost += add.cost;
            safety++;
        }

        if (cost > budget || kcal > target + 200) {
            dayMeals.sort(Comparator.comparingDouble(m -> scoreRemovePenalty(m)));
            while ((cost > budget || kcal > target + 200) && dayMeals.size() > 3) {
                Meal rem = dayMeals.remove(dayMeals.size() - 1);
                kcal -= rem.calories;
                cost -= rem.cost;
            }
        }

        d.meals = dayMeals;
        d.totalCalories = kcal;
        d.cost = round2(cost);
        d.protein = dayMeals.stream().mapToInt(m -> m.protein).sum();
        d.carbs = dayMeals.stream().mapToInt(m -> m.carbs).sum();
        d.fat = dayMeals.stream().mapToInt(m -> m.fat).sum();
    }

    private boolean dietOk(UserProfile dp, Meal m) {
        if (dp.getDiet().isVegan() && !m.vegan) return false;
        if (dp.getDiet().isVegetarian() && !m.vegetarian) return false;
        if (dp.getDiet().isHalal() && !m.halalFriendly) return false;
        if (dp.getDiet().isLactoseFree() && !m.lactoseFree) return false;
        if (dp.getDiet().isGlutenFree() && !m.glutenFree) return false;
        for (String a : dp.getDiet().getAllergies())
            if (m.ingredients.stream().anyMatch(ing -> ing.equalsIgnoreCase(a))) return false;
        for (String d : dp.getDiet().getDislikedIngredients())
            if (m.ingredients.stream().anyMatch(ing -> ing.equalsIgnoreCase(d))) return false;
        return true;
    }

    private boolean cuisineOk(UserProfile dp, Meal m) {
        if (dp.getDiet().getPreferredCuisines().isEmpty()) return true;
        for (String c : dp.getDiet().getPreferredCuisines()) if (m.cuisine.equalsIgnoreCase(c)) return true;
        if (dp.getRegion() != null && dp.getRegion().equalsIgnoreCase("India"))
            return m.cuisine.toLowerCase().contains("indian");
        return false;
    }

    private void pickTopBy(List<Meal> pool, List<Meal> target, ToDoubleFunction<Meal> key, int count) {
    // Sort descending by the double value produced by the provided key
    List<Meal> sorted = pool.stream()
            .sorted((a, b) -> Double.compare(key.applyAsDouble(b), key.applyAsDouble(a)))
            .collect(Collectors.toList());
    for (Meal m : sorted) {
        if (!target.contains(m)) {
            target.add(m);
            if (--count == 0) break;
        }
    }
}


    private void pickBreakfast(List<Meal> pool, List<Meal> target) {
        String[] breakfastHints = {"oats", "poha", "upma", "dosa", "idli", "paratha", "omelette", "smoothie"};
        Optional<Meal> pick = pool.stream().filter(m ->
                Arrays.stream(breakfastHints).anyMatch(h -> m.name.toLowerCase().contains(h))
        ).findFirst();
        pick.ifPresent(m -> { if (!target.contains(m)) target.add(m); });
    }

    private void pickSnack(List<Meal> pool, List<Meal> target) {
        String[] snackHints = {"chana", "sprouts", "nuts", "curd", "yogurt", "fruit", "salad"};
        Optional<Meal> pick = pool.stream().filter(m ->
                Arrays.stream(snackHints).anyMatch(h -> m.name.toLowerCase().contains(h))
        ).findFirst();
        pick.ifPresent(m -> { if (!target.contains(m)) target.add(m); });
    }

    private Meal pickAffordable(List<Meal> affordable, int kcalGap, double budgetLeft) {
        for (Meal m : affordable) {
            if (m.cost <= budgetLeft && m.calories <= kcalGap + 250) return m;
        }
        return null;
    }

    private double scoreRemovePenalty(Meal m) {
        return (m.calories * 0.002) - (m.protein * 0.05) + (m.cost * 0.02);
    }

    private int sumProtein(List<Meal> list) {
        return list.stream().mapToInt(m -> m.protein).sum();
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // ---------- Inner types ----------

    public static class DayPlan {
        public List<Exercise> workout = new ArrayList<>();
        public List<Meal> meals = new ArrayList<>();
        public int targetCalories;
        public int totalCalories;
        public int protein;
        public int carbs;
        public int fat;
        public double cost;
        public boolean restDay;
    }

    public static class WeeklyPlan {
        public List<DayPlan> days = new ArrayList<>();
        public int weeklyTargetCalories;
        public double weeklyBudget;
        public double weeklyCost;
    }

    public static class Exercise {
        public String name;
        public String muscleGroup;
        public String required;
        public String intensity;
        public int estMinutes;
        public String level;
        public boolean outdoors;

        public static Exercise of(String n, String mg, String req, String intn, int min, String lvl, boolean out) {
            Exercise e = new Exercise();
            e.name = n; e.muscleGroup = mg; e.required = req; e.intensity = intn; e.estMinutes = min; e.level = lvl; e.outdoors = out; return e;
        }
    }

    public static class Meal {
        public String name;
        public String cuisine;
        public boolean vegetarian;
        public boolean vegan;
        public Set<String> ingredients = new HashSet<>();
        public int calories;
        public int protein;
        public int carbs;
        public int fat;
        public double cost;
        public boolean halalFriendly = true;
        public boolean lactoseFree = false;
        public boolean glutenFree = false;

        public static Meal of(String n, String cui, boolean veg, boolean vgn, int kcal, int p, int c, int f, double inr,
                              boolean halal, boolean lf, boolean gf, String... ingr) {
            Meal m = new Meal(); m.name = n; m.cuisine = cui; m.vegetarian = veg; m.vegan = vgn; m.calories = kcal;
            m.protein = p; m.carbs = c; m.fat = f; m.cost = inr; m.halalFriendly = halal; m.lactoseFree = lf; m.glutenFree = gf;
            m.ingredients.addAll(Arrays.asList(ingr)); return m;
        }
    }

    // ---------- Tiny Database copy ----------
    static class Database {
        static List<Exercise> exercises() {
            List<Exercise> x = new ArrayList<>();
            x.add(Exercise.of("Jumping Jacks","Cardio","none","low",5,"BEGINNER",true));
            x.add(Exercise.of("Bodyweight Squats","Legs","none","moderate",10,"BEGINNER",false));
            x.add(Exercise.of("Push-ups","Push","none","moderate",8,"BEGINNER",false));
            x.add(Exercise.of("Incline Push-ups","Push","none","low",8,"BEGINNER",false));
            x.add(Exercise.of("Plank","Core","none","low",5,"BEGINNER",false));
            x.add(Exercise.of("Mountain Climbers","Cardio","none","moderate",6,"BEGINNER",false));
            x.add(Exercise.of("Burpees","Full Body","none","high",8,"INTERMEDIATE",false));
            x.add(Exercise.of("Pull-ups","Pull","none","high",8,"ADVANCED",false));
            x.add(Exercise.of("DB Goblet Squat","Legs","dumbbells","moderate",10,"BEGINNER",false));
            x.add(Exercise.of("DB Romanian Deadlift","Legs","dumbbells","moderate",10,"INTERMEDIATE",false));
            x.add(Exercise.of("DB Bench Press","Push","dumbbells","moderate",10,"INTERMEDIATE",false));
            x.add(Exercise.of("DB Row","Pull","dumbbells","moderate",10,"BEGINNER",false));
            x.add(Exercise.of("DB Shoulder Press","Push","dumbbells","moderate",8,"INTERMEDIATE",false));
            x.add(Exercise.of("DB Lunges","Legs","dumbbells","moderate",10,"BEGINNER",false));
            x.add(Exercise.of("Band Row","Pull","bands","low",8,"BEGINNER",false));
            x.add(Exercise.of("Band Chest Press","Push","bands","low",8,"BEGINNER",false));
            x.add(Exercise.of("Band Face Pull","Pull","bands","low",6,"INTERMEDIATE",false));
            x.add(Exercise.of("Barbell Squat","Legs","gym","high",12,"INTERMEDIATE",false));
            x.add(Exercise.of("Deadlift","Full Body","gym","high",12,"ADVANCED",false));
            x.add(Exercise.of("Bench Press","Push","gym","high",10,"INTERMEDIATE",false));
            x.add(Exercise.of("Lat Pulldown","Pull","gym","moderate",10,"BEGINNER",false));
            x.add(Exercise.of("Treadmill Run","Cardio","gym","moderate",15,"BEGINNER",false));
            x.add(Exercise.of("Easy Run","Cardio","none","moderate",20,"BEGINNER",true));
            x.add(Exercise.of("Tempo Run","Cardio","none","high",20,"INTERMEDIATE",true));
            return x;
        }

        static List<Meal> meals() {
            List<Meal> m = new ArrayList<>();
            m.add(Meal.of("Veg Khichdi","Indian", true, true, 420, 14, 75, 7, 40, true, true, true, "rice","lentils","turmeric","ghee"));
            m.add(Meal.of("Paneer Bhurji + Chapati","North Indian", true, false, 550, 32, 55, 20, 70, true, false, false, "paneer","wheat","onion","spices"));
            m.add(Meal.of("Dal Tadka + Rice","North Indian", true, true, 520, 20, 90, 6, 50, true, true, true, "lentils","rice","spices"));
            m.add(Meal.of("Chole + Brown Rice","North Indian", true, true, 560, 22, 95, 8, 60, true, true, true, "chickpeas","rice","spices"));
            m.add(Meal.of("Masala Oats","Indian", true, true, 350, 12, 55, 8, 25, true, true, false, "oats","veg","spices"));
            m.add(Meal.of("Upma","South Indian", true, true, 380, 9, 65, 8, 25, true, true, false, "rava","veg","spices"));
            m.add(Meal.of("Idli + Sambar","South Indian", true, true, 420, 14, 70, 6, 35, true, true, true, "rice","lentils","spices"));
            m.add(Meal.of("Dosa + Sambar","South Indian", true, true, 520, 12, 80, 12, 40, true, true, true, "rice","lentils","oil"));
            m.add(Meal.of("Veg Poha","Indian", true, true, 360, 8, 62, 7, 20, true, true, true, "poha","veg","peanut"));
            m.add(Meal.of("Sprouts Chaat","Indian", true, true, 280, 16, 40, 6, 25, true, true, true, "sprouts","onion","tomato"));
            m.add(Meal.of("Curd Rice","South Indian", true, false, 450, 12, 75, 9, 30, true, false, true, "curd","rice","tempering"));
            m.add(Meal.of("Grilled Chicken + Rice","Indian", false, false, 620, 45, 70, 14, 90, true, true, true, "chicken","rice","spices"));
            m.add(Meal.of("Egg Bhurji + Chapati","Indian", false, false, 480, 26, 45, 18, 40, true, true, false, "egg","wheat","onion","spices"));
            m.add(Meal.of("Fish Curry + Rice","Coastal Indian", false, false, 560, 35, 70, 14, 85, true, true, true, "fish","rice","spices"));
            m.add(Meal.of("Oatmeal + Banana","Western", true, true, 380, 10, 70, 6, 30, true, true, false, "oats","banana"));
            m.add(Meal.of("PB Sandwich","Western", true, true, 450, 16, 50, 18, 35, true, true, false, "peanut","bread"));
            m.add(Meal.of("Greek Yogurt + Fruit","Western", true, false, 250, 18, 30, 4, 45, true, false, true, "yogurt","fruit"));
            m.add(Meal.of("Tuna Salad Bowl","Western", false, false, 420, 32, 35, 12, 110, true, true, true, "tuna","veg"));
            m.add(Meal.of("Chicken Wrap","Western", false, false, 520, 40, 55, 12, 95, true, true, false, "chicken","tortilla"));
            m.add(Meal.of("Veg Fried Rice (low oil)","East Asian", true, true, 520, 12, 90, 8, 50, true, true, true, "rice","veg","soy"));
            m.add(Meal.of("Tofu Stir-fry + Rice","East Asian", true, true, 560, 28, 80, 12, 85, true, true, true, "tofu","veg","rice"));
            m.add(Meal.of("Banana + Peanuts","Snack", true, true, 280, 8, 30, 12, 15, true, true, true, "banana","peanut"));
            m.add(Meal.of("Buttermilk (Chaas) + Nuts","Snack", true, false, 180, 7, 12, 9, 15, true, false, true, "curd","spices","nuts"));
            m.add(Meal.of("Fruit Bowl","Snack", true, true, 200, 3, 50, 1, 25, true, true, true, "seasonal fruit"));
            m.add(Meal.of("Roasted Chana","Snack", true, true, 220, 12, 30, 4, 12, true, true, true, "chana"));
            return m;
        }
    }

    // tiny functional wrapper for lambda typing
    @FunctionalInterface
    private interface ToDoubleFunction<T> {
        double applyAsDouble(T value);
    }
}
