package com.PokeMeng.OldManGO.Challenge;

public class ChallengeHistoryStep {
    private int stepNumber;
    @SuppressWarnings("unused")
    public ChallengeHistoryStep() {} // No-argument constructor
    // Parameterized constructor
    public ChallengeHistoryStep(int stepNumber) { this.stepNumber = stepNumber;}
    public int getStepNumber() { return stepNumber;}
}