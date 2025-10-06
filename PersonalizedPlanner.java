import java.util.*;
import java.util.stream.*;

/**
 * Personalized Workout & Diet Planner with AI (rule-based heuristic)
 * ---------------------------------------------------------------
 * - Single-file Java 17 console app (no external libraries).
 * - Generates a 7‑day workout & meal plan personalized to the user.
 * - Considers goals, budget, cuisine & dietary prefs, equipment, schedule, and fitness level.
 * - Uses BMR (Mifflin–St Jeor) -> TDEE -> target calories, and greedy/knapsack-like selection for meals.
 * - Workout planner balances muscle groups, intensity, rest days, and equipment availability.
 *
 * HOW TO RUN (Java 17+):
 *   javac PersonalizedPlanner.java && java PersonalizedPlanner
 *
 * TIP: Start with the interactive prompts. You can also adapt `exampleProfiles()` for unit tests.
 */
public class PersonalizedPlanner {

    // ======== ENTRY POINT ========
    public static void main(String[] args) {
        System.out.println("\n=== Personalized Workout & Diet Planner (AI Heuristic) ===\n");
        Scanner sc = new Scanner(System.in);
        UserProfile profile = Prompter.promptUser(sc);

        PlannerEngine engine = new PlannerEngine();
        WeeklyPlan plan = engine.generateWeeklyPlan(profile);

        Printer.printSummary(profile, plan);
        Printer.printWeeklyWorkout(plan);
        Printer.printWeeklyMeals(plan);
        Printer.printShoppingList(plan);
    }

    // ======== DATA MODELS ========
    enum Goal { LOSE_FAT, MAINTAIN, GAIN_MUSCLE }
    enum Sex { MALE, FEMALE }
    enum ActivityLevel { SEDENTARY, LIGHT, MODERATE, ACTIVE, VERY_ACTIVE }
    enum Experience { BEGINNER, INTERMEDIATE, ADVANCED }

    static class DietPreference {
        boolean vegetarian;
        boolean vegan;
        boolean lactoseFree;
        boolean glutenFree;
        boolean halal;
        Set<String> allergies = new HashSet<>(); // e.g., "peanut", "egg"
        Set<String> dislikedIngredients = new HashSet<>();
        Set<String> preferredCuisines = new HashSet<>(); // e.g., "Indian", "South Indian", "Western"

        @Override public String toString() {
            return String.format("veg=%s, vegan=%s, LF=%s, GF=%s, halal=%s, allergies=%s, dislikes=%s, cuisines=%s",
                    vegetarian, vegan, lactoseFree, glutenFree, halal, allergies, dislikedIngredients, preferredCuisines);
        }
    }

    static class Equipment {
        boolean hasGym;
        boolean hasDumbbells;
        boolean hasResistanceBands;
        boolean hasYogaMat;
        boolean canRunOutside;
        @Override public String toString(){
            return String.format("gym=%s, DB=%s, bands=%s, mat=%s, runOutside=%s",
                    hasGym, hasDumbbells, hasResistanceBands, hasYogaMat, canRunOutside);
        }
    }

    static class Schedule {
        int workoutDaysPerWeek; // e.g., 3-6
        int minutesPerWorkout;  // e.g., 30-60
        double dailyFoodBudget; // in local currency (assume INR by default)
        @Override public String toString(){
            return String.format("%d days/wk, %d min/session, budget=%.2f/day",
                    workoutDaysPerWeek, minutesPerWorkout, dailyFoodBudget);
        }
    }

    static class UserProfile {
        String name;
        int age;
        Sex sex;
        double heightCm;
        double weightKg;
        ActivityLevel activityLevel;
        Experience experience;
        Goal goal;
        DietPreference diet = new DietPreference();
        Equipment equipment = new Equipment();
        Schedule schedule = new Schedule();
        String region; // e.g., "India" to bias ingredients
    }

    static class Exercise {
        String name;
        String muscleGroup; // e.g., "Full Body", "Push", "Pull", "Legs", "Core", "Cardio"
        String required;    // e.g., "none", "dumbbells", "bands", "gym"
        String intensity;   // "low", "moderate", "high"
        int estMinutes;     // default time per set scheme
        String level;       // BEGINNER/INTERMEDIATE/ADVANCED
        boolean outdoors;

        static Exercise of(String n, String mg, String req, String intn, int min, String lvl, boolean out){
            Exercise e = new Exercise();
            e.name=n; e.muscleGroup=mg; e.required=req; e.intensity=intn; e.estMinutes=min; e.level=lvl; e.outdoors=out; return e;
        }
    }

    static class Meal {
        String name;
        String cuisine; // e.g., "Indian", "South Indian", "North Indian", "Western", "East Asian"
        boolean vegetarian;
        boolean vegan;
        Set<String> ingredients = new HashSet<>();
        int calories;   // approx per serving
        int protein;    // grams
        int carbs;      // grams
        int fat;        // grams
        double cost;    // per serving (assume INR)
        boolean halalFriendly = true;
        boolean lactoseFree = false;
        boolean glutenFree = false;

        static Meal of(String n, String cui, boolean veg, boolean vgn, int kcal, int p, int c, int f, double inr,
                        boolean halal, boolean lf, boolean gf, String... ingr){
            Meal m = new Meal();
            m.name=n; m.cuisine=cui; m.vegetarian=veg; m.vegan=vgn; m.calories=kcal; m.protein=p; m.carbs=c; m.fat=f; m.cost=inr;
            m.halalFriendly=halal; m.lactoseFree=lf; m.glutenFree=gf; m.ingredients.addAll(Arrays.asList(ingr));
            return m;
        }
    }

    static class DayPlan {
        List<Exercise> workout = new ArrayList<>();
        List<Meal> meals = new ArrayList<>();
        int targetCalories;
        int totalCalories;
        int protein;
        int carbs;
        int fat;
        double cost;
        boolean restDay;
    }

    static class WeeklyPlan {
        List<DayPlan> days = new ArrayList<>(); // size 7
        int weeklyTargetCalories;
        double weeklyBudget;
        double weeklyCost;
    }

    // ======== PROMPTER & PRINTER ========
    static class Prompter {
        static UserProfile promptUser(Scanner sc){
            UserProfile u = new UserProfile();
            System.out.print("Your name: "); u.name = orDefault(sc.nextLine(), "Student");
            System.out.print("Age: "); u.age = parseInt(orDefault(sc.nextLine(), "20"), 20);
            System.out.print("Sex (M/F): ");
            String sx = sc.nextLine().trim().toUpperCase();
            u.sex = sx.startsWith("F") ? Sex.FEMALE : Sex.MALE;
            System.out.print("Height (cm): "); u.heightCm = parseDouble(orDefault(sc.nextLine(), "170"), 170);
            System.out.print("Weight (kg): "); u.weightKg = parseDouble(orDefault(sc.nextLine(), "65"), 65);
            System.out.print("Activity (sedentary/light/moderate/active/very): ");
            String act = sc.nextLine().trim().toLowerCase();
            u.activityLevel = switch (act) {
                case "light" -> ActivityLevel.LIGHT;
                case "moderate" -> ActivityLevel.MODERATE;
                case "active" -> ActivityLevel.ACTIVE;
                case "very" -> ActivityLevel.VERY_ACTIVE;
                default -> ActivityLevel.SEDENTARY;
            };
            System.out.print("Experience (beginner/intermediate/advanced): ");
            String ex = sc.nextLine().trim().toLowerCase();
            u.experience = switch (ex) {
                case "intermediate" -> Experience.INTERMEDIATE;
                case "advanced" -> Experience.ADVANCED;
                default -> Experience.BEGINNER;
            };
            System.out.print("Goal (lose/maintain/gain): ");
            String g = sc.nextLine().trim().toLowerCase();
            u.goal = switch (g) { case "gain" -> Goal.GAIN_MUSCLE; case "maintain" -> Goal.MAINTAIN; default -> Goal.LOSE_FAT; };

            // Diet prefs
            System.out.print("Vegetarian? (y/n): "); u.diet.vegetarian = yes(sc.nextLine());
            System.out.print("Vegan? (y/n): "); u.diet.vegan = yes(sc.nextLine());
            System.out.print("Lactose-free? (y/n): "); u.diet.lactoseFree = yes(sc.nextLine());
            System.out.print("Gluten-free? (y/n): "); u.diet.glutenFree = yes(sc.nextLine());
            System.out.print("Halal? (y/n): "); u.diet.halal = yes(sc.nextLine());
            System.out.print("Allergies (comma-separated, blank for none): "); addCSV(sc.nextLine(), u.diet.allergies);
            System.out.print("Disliked ingredients (comma-separated): "); addCSV(sc.nextLine(), u.diet.dislikedIngredients);
            System.out.print("Preferred cuisines (comma-separated, e.g., Indian,South Indian,Western): "); addCSV(sc.nextLine(), u.diet.preferredCuisines);

            // Equipment & schedule
            System.out.print("Has gym access? (y/n): "); u.equipment.hasGym = yes(sc.nextLine());
            System.out.print("Has dumbbells? (y/n): "); u.equipment.hasDumbbells = yes(sc.nextLine());
            System.out.print("Has resistance bands? (y/n): "); u.equipment.hasResistanceBands = yes(sc.nextLine());
            System.out.print("Has yoga mat? (y/n): "); u.equipment.hasYogaMat = yes(sc.nextLine());
            System.out.print("Can run outside? (y/n): "); u.equipment.canRunOutside = yes(sc.nextLine());
            System.out.print("Workout days per week (3-6): "); u.schedule.workoutDaysPerWeek = clamp(parseInt(orDefault(sc.nextLine(), "4"), 4), 2, 6);
            System.out.print("Minutes per workout (30-60): "); u.schedule.minutesPerWorkout = clamp(parseInt(orDefault(sc.nextLine(), "45"), 45), 20, 90);
            System.out.print("Daily food budget (INR): "); u.schedule.dailyFoodBudget = parseDouble(orDefault(sc.nextLine(), "250"), 250);

            System.out.print("Region (e.g., India): "); u.region = orDefault(sc.nextLine(), "India");
            System.out.println();
            return u;
        }

        static String orDefault(String s, String d){ return (s==null||s.isBlank())? d : s.trim(); }
        static int parseInt(String s, int d){ try{ return Integer.parseInt(s.trim()); }catch(Exception e){ return d; } }
        static double parseDouble(String s, double d){ try{ return Double.parseDouble(s.trim()); }catch(Exception e){ return d; } }
        static boolean yes(String s){ return s!=null && s.trim().toLowerCase().startsWith("y"); }
        static void addCSV(String s, Set<String> set){ if(s==null||s.isBlank()) return; for(String p: s.split(",")) set.add(p.trim().toLowerCase()); }
        static int clamp(int v, int lo, int hi){ return Math.max(lo, Math.min(hi, v)); }
    }

    static class Printer {
        static void printSummary(UserProfile u, WeeklyPlan plan){
            System.out.println("================ SUMMARY ================");
            System.out.printf(Locale.US, "Name: %s\nAge: %d  Sex: %s  Height: %.1f cm  Weight: %.1f kg\n",
                    u.name, u.age, u.sex, u.heightCm, u.weightKg);
            System.out.printf("Goal: %s  Activity: %s  Experience: %s\n", u.goal, u.activityLevel, u.experience);
            System.out.printf("Diet: %s\n", u.diet);
            System.out.printf("Equip: %s\n", u.equipment);
            System.out.printf("Schedule: %s\nRegion: %s\n", u.schedule, u.region);
            System.out.printf(Locale.US, "\nTarget calories/day: %d  (weekly target: %d)\nDaily budget: %.2f  Weekly budget: %.2f  Weekly cost: %.2f\n\n",
                    plan.days.get(0).targetCalories, plan.weeklyTargetCalories, u.schedule.dailyFoodBudget,
                    plan.weeklyBudget, plan.weeklyCost);
        }

        static void printWeeklyWorkout(WeeklyPlan plan){
            System.out.println("=============== WORKOUT PLAN (7 days) ===============");
            for(int i=0;i<7;i++){
                DayPlan d = plan.days.get(i);
                System.out.printf("Day %d: %s\n", i+1, d.restDay?"REST":"WORKOUT");
                if(!d.restDay){
                    for(Exercise e: d.workout){
                        System.out.printf("  - %-20s | Group: %-10s | %s | %s | ~%d min\n",
                                e.name, e.muscleGroup, e.required, e.intensity, e.estMinutes);
                    }
                }
            }
            System.out.println();
        }

        static void printWeeklyMeals(WeeklyPlan plan){
            System.out.println("================ MEAL PLAN (7 days) ================");
            for(int i=0;i<7;i++){
                DayPlan d = plan.days.get(i);
                System.out.printf("Day %d: kcal %d/%d | P:%dg C:%dg F:%dg | Cost: %.2f\n",
                        i+1, d.totalCalories, d.targetCalories, d.protein, d.carbs, d.fat, d.cost);
                int idx=1;
                for(Meal m: d.meals){
                    System.out.printf("  %d) %-28s | %-12s | %4dkcal | P:%2dg C:%3dg F:%2dg | ₹%.0f\n",
                            idx++, m.name, m.cuisine, m.calories, m.protein, m.carbs, m.fat, m.cost);
                }
            }
            System.out.println();
        }

        static void printShoppingList(WeeklyPlan plan){
            System.out.println("================ SHOPPING LIST (aggregated) ================");
            Map<String,Integer> counts = new TreeMap<>();
            for(DayPlan d: plan.days) for(Meal m: d.meals)
                for(String ing: m.ingredients) counts.merge(ing, 1, Integer::sum);
            counts.forEach((k,v)-> System.out.printf("- %s x%d\n", k, v));
            System.out.println();
        }
    }

    // ======== ENGINE ========
    static class PlannerEngine {
        Random rng = new Random(42);
        List<Exercise> dbExercises = Database.exercises();
        List<Meal> dbMeals = Database.meals();

        WeeklyPlan generateWeeklyPlan(UserProfile u){
            WeeklyPlan wp = new WeeklyPlan();
            int targetKcal = targetCalories(u);
            wp.weeklyTargetCalories = targetKcal * 7;
            wp.weeklyBudget = u.schedule.dailyFoodBudget * 7.0;

            // Decide rest days evenly spaced based on workoutDaysPerWeek
            boolean[] workoutDays = pickWorkoutDays(u.schedule.workoutDaysPerWeek);

            for(int day=0; day<7; day++){
                DayPlan dp = new DayPlan();
                dp.targetCalories = targetKcal;
                dp.restDay = !workoutDays[day];
                if(!dp.restDay){
                    dp.workout = planWorkoutForDay(u);
                }
                planMealsForDay(u, dp);
                wp.days.add(dp);
            }
            wp.weeklyCost = wp.days.stream().mapToDouble(d->d.cost).sum();
            return wp;
        }

        int targetCalories(UserProfile u){
            // Mifflin–St Jeor
            double s = (u.sex==Sex.MALE)?5:-161;
            double bmr = 10*u.weightKg + 6.25*u.heightCm - 5*u.age + s;
            double activity = switch(u.activityLevel){
                case SEDENTARY -> 1.2; case LIGHT -> 1.375; case MODERATE -> 1.55; case ACTIVE -> 1.725; default -> 1.9; };
            double tdee = bmr * activity;
            double adj = switch(u.goal){ case LOSE_FAT -> -400; case GAIN_MUSCLE -> +300; default -> 0; };
            int target = (int)Math.round(tdee + adj);
            return Math.max(1400, Math.min(3500, target));
        }

        boolean[] pickWorkoutDays(int n){
            boolean[] arr = new boolean[7];
            // simple spacing heuristic preset patterns
            int[][] patterns = {
                {1,0,1,0,1,0,0}, // 3 days (Mon, Wed, Fri)
                {1,0,1,0,1,0,1}, // 4 days (Mon, Wed, Fri, Sun)
                {1,0,1,1,0,1,0}, // 5 days
                {1,1,1,0,1,1,0}, // 6 days
            };
            if(n<=3) arr = from(patterns[0]);
            else if(n==4) arr = from(patterns[1]);
            else if(n==5) arr = from(patterns[2]);
            else arr = from(patterns[3]);
            return arr;
        }
        boolean[] from(int[] p){ boolean[] b=new boolean[7]; for(int i=0;i<7;i++) b[i]=p[i]==1; return b; }

        List<Exercise> planWorkoutForDay(UserProfile u){
            // Filter by equipment and level
            List<Exercise> pool = dbExercises.stream().filter(e->equipOk(u.equipment, e)).collect(Collectors.toList());
            // Balance template based on experience
            int targetMin = u.schedule.minutesPerWorkout;
            List<Exercise> plan = new ArrayList<>();

            // Always warm-up and core
            addIfExists(pool, plan, "Jumping Jacks");
            addIfExists(pool, plan, "Plank");

            // Add main lifts / movements across groups
            String[] groups = {"Push", "Pull", "Legs", "Full Body", "Cardio"};
            for(String g: groups){ pickByGroup(pool, plan, g, u.experience); }

            // Trim or extend to fit time
            int total = plan.stream().mapToInt(e->e.estMinutes).sum();
            // If over time, drop lowest intensity first
            if(total > targetMin){
                plan.sort(Comparator.comparing(e->e.intensity));
                while(total>targetMin && plan.size()>3){
                    Exercise rem = plan.remove(0);
                    total -= rem.estMinutes;
                }
            }
            // If under time, add cardio/core fillers
            while(total < targetMin){
                Exercise add = pool.stream().filter(e-> e.muscleGroup.equals("Cardio") || e.muscleGroup.equals("Core") )
                        .skip(rng.nextInt(Math.max(1, pool.size()/4))).findAny().orElse(null);
                if(add==null) break; plan.add(add); total += add.estMinutes;
                if(plan.size()>10) break;
            }
            return plan;
        }

        boolean equipOk(Equipment eq, Exercise e){
            switch(e.required){
                case "gym": return eq.hasGym;
                case "dumbbells": return eq.hasDumbbells || eq.hasGym;
                case "bands": return eq.hasResistanceBands || eq.hasGym;
                case "none": default: return true;
            }
        }

        void addIfExists(List<Exercise> pool, List<Exercise> plan, String name){
            pool.stream().filter(e->e.name.equals(name)).findFirst().ifPresent(plan::add);
        }

        void pickByGroup(List<Exercise> pool, List<Exercise> plan, String group, Experience exp){
            List<Exercise> g = pool.stream().filter(e->e.muscleGroup.equals(group)).collect(Collectors.toList());
            if(g.isEmpty()) return;
            // prefer level-appropriate
            List<Exercise> lvl = g.stream().filter(e->e.level.equalsIgnoreCase(exp.name())).collect(Collectors.toList());
            List<Exercise> src = lvl.isEmpty()? g : lvl;
            Exercise choice = src.get(rng.nextInt(src.size()));
            plan.add(choice);
        }

        void planMealsForDay(UserProfile u, DayPlan d){
            List<Meal> pool = dbMeals.stream()
                    .filter(m-> dietOk(u.diet, m))
                    .filter(m-> cuisineOk(u.diet, m, u.region))
                    .collect(Collectors.toList());
            // Aim for 3 meals + 1 snack
            int target = d.targetCalories;
            double budget = u.schedule.dailyFoodBudget;
            List<Meal> dayMeals = new ArrayList<>();

            // Greedy passes: pick protein-dense within budget first
            pickTopBy(pool, dayMeals, m-> m.protein/(double)Math.max(1,m.calories), 1);
            // Add staple carbs meal (e.g., rice/chapati bowls)
            pickTopBy(pool, dayMeals, m-> m.carbs/(double)Math.max(1,m.calories), 1);
            // Add breakfast-friendly option
            pickBreakfast(pool, dayMeals);
            // Add snack
            pickSnack(pool, dayMeals);

            // Adjust to hit calories and budget
            int kcal = sum(dayMeals, m->m.calories);
            double cost = sumD(dayMeals, m->m.cost);
            // If under calories, add affordable items until near target
            List<Meal> affordable = pool.stream().sorted(Comparator.comparingDouble(m->m.cost)).collect(Collectors.toList());
            int safety = 0;
            while(kcal < target-150 && cost <= budget && safety < 20){
                Meal add = pickAffordable(affordable, target-kcal, budget-cost);
                if(add==null) break; dayMeals.add(add); kcal += add.calories; cost += add.cost; safety++;
            }
            // If over budget or too high calories, drop least efficient items
            if(cost>budget || kcal>target+200){
                dayMeals.sort(Comparator.comparingDouble(m-> scoreRemovePenalty(m)));
                while((cost>budget || kcal>target+200) && dayMeals.size()>3){
                    Meal rem = dayMeals.remove(dayMeals.size()-1);
                    kcal -= rem.calories; cost -= rem.cost;
                }
            }

            // finalize
            d.meals = dayMeals;
            d.totalCalories = kcal;
            d.cost = round2(cost);
            d.protein = sum(dayMeals, m->m.protein);
            d.carbs = sum(dayMeals, m->m.carbs);
            d.fat = sum(dayMeals, m->m.fat);
        }

        boolean dietOk(DietPreference dp, Meal m){
            if(dp.vegan && !m.vegan) return false;
            if(dp.vegetarian && !m.vegetarian) return false;
            if(dp.halal && !m.halalFriendly) return false;
            if(dp.lactoseFree && !m.lactoseFree) return false;
            if(dp.glutenFree && !m.glutenFree) return false;
            for(String a: dp.allergies) if(m.ingredients.stream().anyMatch(ing->ing.equalsIgnoreCase(a))) return false;
            for(String d: dp.dislikedIngredients) if(m.ingredients.stream().anyMatch(ing->ing.equalsIgnoreCase(d))) return false;
            return true;
        }
        boolean cuisineOk(DietPreference dp, Meal m, String region){
            if(dp.preferredCuisines.isEmpty()) return true; // no preference
            for(String c: dp.preferredCuisines) if(m.cuisine.equalsIgnoreCase(c)) return true;
            // region fallback bias: allow regional meals even if not listed
            if(region!=null && region.equalsIgnoreCase("India")) return m.cuisine.toLowerCase().contains("indian");
            return false;
        }

        void pickTopBy(List<Meal> pool, List<Meal> target, java.util.function.ToDoubleFunction<Meal> key, int count){
            List<Meal> sorted = pool.stream().sorted(Comparator.comparingDouble(key).reversed()).collect(Collectors.toList());
            for(Meal m: sorted){ if(!target.contains(m)){ target.add(m); if(--count==0) break; } }
        }
        void pickBreakfast(List<Meal> pool, List<Meal> target){
            String[] breakfastHints = {"oats", "poha", "upma", "dosa", "idli", "paratha", "omelette", "smoothie"};
            Optional<Meal> pick = pool.stream().filter(m->
                    Arrays.stream(breakfastHints).anyMatch(h-> m.name.toLowerCase().contains(h))
            ).findFirst();
            pick.ifPresent(m->{ if(!target.contains(m)) target.add(m); });
        }
        void pickSnack(List<Meal> pool, List<Meal> target){
            String[] snackHints = {"chana", "sprouts", "nuts", "curd", "yogurt", "fruit", "salad"};
            Optional<Meal> pick = pool.stream().filter(m->
                    Arrays.stream(snackHints).anyMatch(h-> m.name.toLowerCase().contains(h))
            ).findFirst();
            pick.ifPresent(m->{ if(!target.contains(m)) target.add(m); });
        }
        Meal pickAffordable(List<Meal> affordable, int kcalGap, double budgetLeft){
            for(Meal m: affordable){
                if(m.cost <= budgetLeft && m.calories <= kcalGap + 250) return m;
            }
            return null;
        }
        double scoreRemovePenalty(Meal m){
            // Higher score = more likely to remove
            return (m.calories*0.002) - (m.protein*0.05) + (m.cost*0.02);
        }

        static int sum(List<Meal> list, java.util.function.ToIntFunction<Meal> f){ return list.stream().mapToInt(f).sum(); }
        static double sumD(List<Meal> list, java.util.function.ToDoubleFunction<Meal> f){ return list.stream().mapToDouble(f).sum(); }
        static double round2(double v){ return Math.round(v*100.0)/100.0; }
    }

    // ======== DATABASE (sample, extensible) ========
    static class Database {
        static List<Exercise> exercises(){
            List<Exercise> x = new ArrayList<>();
            // Bodyweight / no equipment
            x.add(Exercise.of("Jumping Jacks","Cardio","none","low",5,"BEGINNER",true));
            x.add(Exercise.of("Bodyweight Squats","Legs","none","moderate",10,"BEGINNER",false));
            x.add(Exercise.of("Push-ups","Push","none","moderate",8,"BEGINNER",false));
            x.add(Exercise.of("Incline Push-ups","Push","none","low",8,"BEGINNER",false));
            x.add(Exercise.of("Plank","Core","none","low",5,"BEGINNER",false));
            x.add(Exercise.of("Mountain Climbers","Cardio","none","moderate",6,"BEGINNER",false));
            x.add(Exercise.of("Burpees","Full Body","none","high",8,"INTERMEDIATE",false));
            x.add(Exercise.of("Pull-ups","Pull","none","high",8,"ADVANCED",false));

            // Dumbbells
            x.add(Exercise.of("DB Goblet Squat","Legs","dumbbells","moderate",10,"BEGINNER",false));
            x.add(Exercise.of("DB Romanian Deadlift","Legs","dumbbells","moderate",10,"INTERMEDIATE",false));
            x.add(Exercise.of("DB Bench Press","Push","dumbbells","moderate",10,"INTERMEDIATE",false));
            x.add(Exercise.of("DB Row","Pull","dumbbells","moderate",10,"BEGINNER",false));
            x.add(Exercise.of("DB Shoulder Press","Push","dumbbells","moderate",8,"INTERMEDIATE",false));
            x.add(Exercise.of("DB Lunges","Legs","dumbbells","moderate",10,"BEGINNER",false));

            // Bands
            x.add(Exercise.of("Band Row","Pull","bands","low",8,"BEGINNER",false));
            x.add(Exercise.of("Band Chest Press","Push","bands","low",8,"BEGINNER",false));
            x.add(Exercise.of("Band Face Pull","Pull","bands","low",6,"INTERMEDIATE",false));

            // Gym
            x.add(Exercise.of("Barbell Squat","Legs","gym","high",12,"INTERMEDIATE",false));
            x.add(Exercise.of("Deadlift","Full Body","gym","high",12,"ADVANCED",false));
            x.add(Exercise.of("Bench Press","Push","gym","high",10,"INTERMEDIATE",false));
            x.add(Exercise.of("Lat Pulldown","Pull","gym","moderate",10,"BEGINNER",false));
            x.add(Exercise.of("Treadmill Run","Cardio","gym","moderate",15,"BEGINNER",false));

            // Outdoor cardio
            x.add(Exercise.of("Easy Run","Cardio","none","moderate",20,"BEGINNER",true));
            x.add(Exercise.of("Tempo Run","Cardio","none","high",20,"INTERMEDIATE",true));
            return x;
        }

        static List<Meal> meals(){
            List<Meal> m = new ArrayList<>();
            // (name, cuisine, vegetarian, vegan, kcal, P, C, F, ₹cost, halal, LF, GF, ingredients...)

            // Indian veg
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

            // Indian non-veg (halal-friendly true by default)
            m.add(Meal.of("Grilled Chicken + Rice","Indian", false, false, 620, 45, 70, 14, 90, true, true, true, "chicken","rice","spices"));
            m.add(Meal.of("Egg Bhurji + Chapati","Indian", false, false, 480, 26, 45, 18, 40, true, true, false, "egg","wheat","onion","spices"));
            m.add(Meal.of("Fish Curry + Rice","Coastal Indian", false, false, 560, 35, 70, 14, 85, true, true, true, "fish","rice","spices"));

            // Western
            m.add(Meal.of("Oatmeal + Banana","Western", true, true, 380, 10, 70, 6, 30, true, true, false, "oats","banana"));
            m.add(Meal.of("PB Sandwich","Western", true, true, 450, 16, 50, 18, 35, true, true, false, "peanut","bread"));
            m.add(Meal.of("Greek Yogurt + Fruit","Western", true, false, 250, 18, 30, 4, 45, true, false, true, "yogurt","fruit"));
            m.add(Meal.of("Tuna Salad Bowl","Western", false, false, 420, 32, 35, 12, 110, true, true, true, "tuna","veg"));
            m.add(Meal.of("Chicken Wrap","Western", false, false, 520, 40, 55, 12, 95, true, true, false, "chicken","tortilla"));

            // East Asian
            m.add(Meal.of("Veg Fried Rice (low oil)","East Asian", true, true, 520, 12, 90, 8, 50, true, true, true, "rice","veg","soy"));
            m.add(Meal.of("Tofu Stir-fry + Rice","East Asian", true, true, 560, 28, 80, 12, 85, true, true, true, "tofu","veg","rice"));

            // Snacks
            m.add(Meal.of("Banana + Peanuts","Snack", true, true, 280, 8, 30, 12, 15, true, true, true, "banana","peanut"));
            m.add(Meal.of("Buttermilk (Chaas) + Nuts","Snack", true, false, 180, 7, 12, 9, 15, true, false, true, "curd","spices","nuts"));
            m.add(Meal.of("Fruit Bowl","Snack", true, true, 200, 3, 50, 1, 25, true, true, true, "seasonal fruit"));
            m.add(Meal.of("Roasted Chana","Snack", true, true, 220, 12, 30, 4, 12, true, true, true, "chana"));

            return m;
        }
    }
}
