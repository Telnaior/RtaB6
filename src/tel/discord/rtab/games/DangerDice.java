package tel.discord.rtab.games;

import java.util.LinkedList;

import tel.discord.rtab.games.objs.Dice;

public class DangerDice extends MiniGameWrapper
{
	static final String NAME = "Danger Dice";
	static final String SHORT_NAME = "Danger";
	static final boolean BONUS = false;
	static final int DANGER_VALUE = 1;
	static final int[] MONEY_LADDER = {0,5_000,10_000,25_000,50_000,75_000,100_000,150_000,200_000,250_000,300_000,
			375_000,450_000,525_000,600_000,700_000,800_000,900_000,1_000_000};
	static final int OVERCAP_INCREMENT = 200_000;
	int diceLeft, score, bonusesAwarded;
	boolean isAlive;

	@Override
	void startGame()
	{
		isAlive = true;
		diceLeft = 8;
		score = 0;
		bonusesAwarded = 0;
		
		//Instructions
		LinkedList<String> output = new LinkedList<>();
		output.add("In Danger Dice, your objective is to avoid rolling 1s!");
		output.add("You start with eight dice, but every time you roll you lose any dice showing a 1.");
		output.add("However, the rest of your dice score you points equal to the value rolled...");
		output.add("...and those points drive you up a limitless money ladder!");
		output.add("Every ten points earned will boost your cash award should you choose to stop...");
		output.add("...but if you lose your last dice, you will leave with nothing.");
		if(enhanced)
			output.add("ENHANCE BONUS: Additionally, every 100 points you score will earn you an extra die to roll!");
		output.add("Best of luck, type ROLL to begin.");
		sendSkippableMessages(output);
		sendMessage(generateBoard());
		getInput();
	}

	@Override
	void playNextTurn(String input)
	{
		LinkedList<String> output = new LinkedList<>();

		if (input.equalsIgnoreCase("STOP")) {
			if (score < 10) {
				String message = "You haven't won any money yet!";
				output.add(message);
			} else {
				isAlive = false;
				output.add("Very well!");
				Dice dice = new Dice(diceLeft);
				dice.rollDice();
				output.add("You would have rolled: " + dice.toString());
			}
		} else if (input.equalsIgnoreCase("ROLL")) {
			Dice dice = new Dice(diceLeft);
			dice.rollDice();
			output.add("You rolled: " + dice.toString());
			//Count up the outcome
			int diceLost = 0;
			int scoreGained = 0;
			for(int nextDie : dice.getDice())
			{
				if(nextDie == DANGER_VALUE)
					diceLost ++;
				else
					scoreGained += nextDie;
			}
			//Tell them about it
			if(diceLost == diceLeft)
			{
				output.add("...which loses you your last die. Sorry.");
				score = 0;
				isAlive = false;
			}
			else
			{
				if(diceLeft == 1)
					output.add("...which scores you "+scoreGained+" points and keeps you alive!");
				else if(diceLost == 0)
					output.add("...which scores you "+scoreGained+" points and keeps all your dice!");
				else
					output.add("...which costs you "+diceLost+(diceLost==1?" die":" dice")+" and scores you "+scoreGained+" points.");
				diceLeft -= diceLost;
				score += scoreGained;
				while(enhanced && score/100 > bonusesAwarded)
				{
					output.add("That earns you an EXTRA DIE!");
					diceLeft++;
					bonusesAwarded++;
				}
				output.add(String.format("ROLL again if you dare, or type STOP to stop with $%,d.",convertScoreToCash(score)));
				output.add(generateBoard());
			}
		}
		
		sendMessages(output);
		if(!isAlive)
			awardMoneyWon(convertScoreToCash(score));
		else
			getInput();
	}
	
	String generateBoard()
	{
		StringBuilder result = new StringBuilder();
		result.append("```\n   DANGER  DICE\n");
		result.append("   Dice Left: "+diceLeft+"\n\n");
		result.append(String.format("    Score: %3d%n",score));
		result.append(String.format("    $%,9d%n%n",convertScoreToCash(score)));
		result.append("     COMING UP\n");
		//Display next five milestones
		for(int i=0; i<5; i++)
		{
			int nextMilestone = score + (10*(i+1)) - (score%10);
			result.append(String.format("%3dpts: $%,9d%n", nextMilestone, convertScoreToCash(nextMilestone)));
		}
		result.append("```");
		return result.toString();
	}

	@Override
	String getBotPick()
	{
		//Stop if we're too far away from the next multiple of ten points to be worth the risk
		return Math.random()*Math.pow(10,diceLeft) < 10 - score%10 ? "STOP" : "ROLL";
	}
	
	static int convertScoreToCash(int score)
	{
		if(score < (MONEY_LADDER.length*10))
			return MONEY_LADDER[score/10];
		else
		{
			int overdamage = (score/10) - MONEY_LADDER.length + 1;
			return MONEY_LADDER[MONEY_LADDER.length-1] + OVERCAP_INCREMENT * overdamage;
		}
	}

	@Override
	void abortGame()
	{
		//Auto-stop, as it is a push-your-luck style game.
		awardMoneyWon(convertScoreToCash(score));
	}

	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	@Override public String getEnhanceText() {
		return "You will earn an extra die for every 100 points scored.";
	}

}
