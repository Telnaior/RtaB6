package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import tel.discord.rtab.MoneyMultipliersToUse;
import tel.discord.rtab.RtaBMath;

public class ItsBananaTime extends MiniGameWrapper
{
	static final String NAME = "\uD83C\uDF4C";
	static final String SHORT_NAME = "\uD83C\uDF4C";
	static final boolean BONUS = true;
	
	@Override
	void startGame()
	{
		//Roll effect magnitudes (-40 to -20, -15 to -5, 5 to 15, 20 to 40)
		List<Double> effects = new ArrayList<>();
		effects.add((RtaBMath.random()*20)-40);
		effects.add((RtaBMath.random()*10)-15);
		effects.add((RtaBMath.random()*10)+5);
		effects.add((RtaBMath.random()*20)+20);
		//and randomly assign which effect gets which magnitude
		Collections.shuffle(effects);
		//Instructions? more like memes
		List<String> output = new LinkedList<>();
		output.add("\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C"
				+ "\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C\uD83C\uDF4C");
		//and now scramble everything
		output.add(scrambleBoost(effects.get(0)));
		output.add(scrambleStreak(effects.get(1)));
		output.add(scrambleAnnuity(effects.get(2)));
		output.add("...");
		output.add(scrambleCash(effects.get(3)));
		output.add("\uD83C\uDF4C");
		sendMessages(output);
		gameOver();
	}
	
	String scrambleBoost(double magnitude)
	{
		//First of all, the boost change is larger the more boost you already have
		magnitude *= getPlayer().booster;
		magnitude /= 100;
		//This one has a positive bias, so we double it if negative but quadruple if positive
		//At 100% booster, this results in -80% to -10% negative, or +20% to +160% positive
		magnitude *= (magnitude > 0 ? 4 : 2);
		int boostAdded = (int)(Math.round(magnitude));
		getPlayer().addBooster(boostAdded);
		return String.format("Now awarding **%d%%** boost, for a total of %d%%!", boostAdded, getPlayer().booster);
	}
	
	String scrambleStreak(double magnitude)
	{
		//We want -2.0x to +2.0x, so cut the magnitude in half and then round it to nearest(!)
		int winstreak = (int)(Math.round(magnitude / 2));
		getPlayer().addWinstreak(winstreak);
		boolean negative = (winstreak < 0);
		if(negative)
			winstreak *= -1;
		int newStreak = getPlayer().winstreak;
		return String.format("Now awarding **%s%d.%d** streak, for a total of x%d.%d!",
				(magnitude > 0 ? "+" : "-"), winstreak/10, winstreak%10, newStreak/10, newStreak%10);
	}
	
	String scrambleAnnuity(double magnitude)
	{
		//Figure out if it's negative and just flip it over if so
		boolean negative = magnitude < 0;
		if(negative)
			magnitude *= -1;
		//Set our annuity amount based on the sign of the magnitude and its modulus
		int annuityAmount = (int)(applyBaseMultiplier(100_000) * (magnitude % 1));
		if(negative)
			annuityAmount *= -1;
		//and the turn count based on the integer magnitude
		int turnCount = (int)magnitude;
		//then award the annuity and tell them what they've won
		getPlayer().addAnnuity(annuityAmount, turnCount);
		return String.format("Now awarding **$%,d** for **%d turns**!", annuityAmount, turnCount);
	}
	
	String scrambleCash(double magnitude)
	{
		//Get a multiplier based on their existing bank (the more they have, the more damage it can do)
		int multiplier = (Math.max(0, getPlayer().money) / 10_000_000) + 25; //25 - 125
		int moneyGained = (int)(magnitude * multiplier * RtaBMath.applyBankPercentBaseMultiplier(10_000, baseNumerator, baseDenominator));
		getPlayer().addMoney(moneyGained, MoneyMultipliersToUse.NOTHING);
		return String.format("Congratulations, your new score is **$%,d**!", getPlayer().money);
	}

	//at the farmer's market with my so-called "input handler"
	@Override void playNextTurn(String input) { /*imagine thinking you get to have input over the loser wheel lmfao*/ }
	@Override String getBotPick() {	/*don't worry, the AI has just as much input over the loser wheel as you do*/ return ""; }
	@Override void abortGame() { /*????????????????*/ }
	
	//Getters
	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	@Override public boolean isNegativeMinigame() { return false; }
	
}
