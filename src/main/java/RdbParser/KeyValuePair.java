package RdbParser;

import java.sql.Timestamp;
import java.time.temporal.ValueRange;

public class KeyValuePair {
    String key;
    ValueType type;
    Object value;
    Timestamp expiryTime;

    public KeyValuePair() {}

//    public KeyValuePair(String key, Object value, ValueType type, String id) {
//        this.key = key;
//        this.value = value;
//        this.type = type;
//    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public ValueType getType() {
        return type;
    }

    public void setType(ValueType type) {
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Timestamp getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(Timestamp expiryTime) {
        this.expiryTime = expiryTime;
    }

    public boolean isNumerical() {
        try {
            Integer.parseInt(value.toString());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

/*
* To support your muscle-building goals with the items you already have, I'll structure a diet for both pre- and post-workout meals that maximizes muscle gains while incorporating some nutrient-dense additions. The focus will be on providing protein for muscle repair, carbohydrates for energy, and healthy fats to sustain energy levels.

Pre-Workout Meal
Your pre-workout meal should provide a balance of carbs and protein for energy and muscle preservation during workouts.

Option 1 (1-2 hours before workout):
Oats/Porridge with milk and banana (complex carbs and quick sugars)
Peanut butter (healthy fats)
Yoghurt (protein)
Option 2 (1 hour before workout):
Rice with chicken (lean protein and complex carbs)
Peas (fiber and protein)
Peanut butter (for sustained energy)
Post-Workout Meal
After your workout, it's important to replenish glycogen stores and support muscle recovery with protein and carbohydrates.

Option 1 (Within 30 minutes post-workout):
Banana and a glass of milk (quick carbs and protein)
Eggs (high-quality protein for muscle recovery)
Option 2:
Beans with rice (complete plant-based protein and carbs)
Grilled beef or chicken (lean protein)
Peas or Irish potatoes (more complex carbs)
Supplementary Items You Can Add:
Sweet potatoes (great carb source for energy)
Spinach or leafy greens (vitamins and minerals)
Nuts or seeds (healthy fats and protein)
Additional Notes:
Hydration: Drink plenty of water throughout the day, especially before and after workouts.
Protein Shakes: You can add a whey protein shake post-workout for convenience.
This meal structure will give you the necessary nutrients for energy, muscle growth, and recovery over the next 2 months.
* */