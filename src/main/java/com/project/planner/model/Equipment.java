package com.project.planner.model;

public class Equipment {
    private boolean hasGym;
    private boolean hasDumbbells;
    private boolean hasResistanceBands;
    private boolean hasYogaMat;
    private boolean canRunOutside;

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
}
