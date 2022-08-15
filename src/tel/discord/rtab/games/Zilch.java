package tel.discord.rtab.games;

import java.util.LinkedList;

import tel.discord.rtab.Achievement;
import tel.discord.rtab.games.objs.Dice;

public class Zilch extends MiniGameWrapper {
	static final String NAME = "Zilch";
	static final String SHORT_NAME = "Zilch";
	static final boolean BONUS = false;
	static final int NUM_DICE = 6;
	static final int WINNING_SCORE = 10_000;
	static final int MONEY_PER_POINT = 1_000;
	
	static final String[] ORDINALS = new String[] {"a","two","three","four","FIVE","**SIX**"};

	static final int[] SINGLE_DICE_SCORE = new int[] {100, 0, 0, 0, 50, 0};
	static final int[] ENHANCED_SINGLE_DICE_SCORE = new int[] {100, 20, 0, 0, 50, 0};
	static final int[] TRIPLE_DICE_SCORE = new int[] {1000, 200, 300, 400, 500, 600};
	static final int[] BASE_TRIPLE_MULTIPLIER = new int[] {1, 2, 4, 8};
	
	static final int NO_SCORING_DICE_SCORE = 500;
	static final int THREE_PAIRS_SCORE = 1500;
	static final int STRAIGHT_SCORE = 2500;
	
	Dice dice;
	String diceScoreString;
	boolean isAlive;
	int score;
	int diceToRoll;

	@Override
	void startGame() {
		LinkedList<String> output = new LinkedList<>();
		isAlive = true;
		score = 0;
		diceToRoll = NUM_DICE;
		
		output.add("In Zilch, you will be given six 6-sided dice with " +
				"which you can win over " + String.format("$%,d",
				convertToDollars(WINNING_SCORE)) + " by scoring dice combinations.");
		output.add("For each three-of-a-kind, you will earn 100 points times " +
				"the tripled die face. For example, three 2s are worth " +
				String.format("%,d", TRIPLE_DICE_SCORE[1]) + " points, " + 
				"three 3s are worth " + 
				String.format("%,d", TRIPLE_DICE_SCORE[2]) + " points, " +
				"and so on. The exception is that three 1s are worth " +
				String.format("%,d", applyBaseMultiplier(TRIPLE_DICE_SCORE[0])) +
				" points.");	
		output.add("A four-, five-, or six-of-a-kind is respectively worth " +
				"two, four, or eight times the corresponding three-of-a-kind " +
				"score.");
		output.add("Three pairs are worth " + String.format("%,d", THREE_PAIRS_SCORE) +
				" points, and a full 1-2-3-4-5-6 straight is worth " +
				String.format("%,d", STRAIGHT_SCORE) + " points.");
		output.add("In addition, each 1 not otherwise part of a " +
				"scoring combination is worth " +
				String.format("%,d", SINGLE_DICE_SCORE[0]) + " points, and " +
				"each 5 not part of a scoring combination is worth " +
				String.format("%,d", SINGLE_DICE_SCORE[4]) + " points.");
		if(enhanced)
			output.add("ENHANCE BONUS: Each 2 will also be worth " +
				String.format("%,d", ENHANCED_SINGLE_DICE_SCORE[1]) + " points.");
		output.add("Each combination must be scored in a single throw. Each " +
				"scored die will be taken away. If you score all your " +
				"remaining dice, you get **HOT DICE**, which means a fresh " +
				"set of six dice.");
		output.add("You may stop at any time. If you wouldn't otherwise have " +
				"any scoring dice from the first roll of the game, you get " +
				String.format("%,d", NO_SCORING_DICE_SCORE) + " points and " +
				"hot dice. If you cannot score any of your dice after that, " +
				"however, you get **ZILCH** and lose everything.");
		output.add(String.format("Each point is worth $%,d upon cashing out.",
				applyBaseMultiplier(MONEY_PER_POINT)));
		output.add("You will keep rolling until you choose to stop, zilch out, " +
				"or win the game by accumulating a total of " +
				String.format("%,d", WINNING_SCORE) + " points or more. " +
				"If you exceed " + String.format("%,d", WINNING_SCORE) +
				" points, you still get to keep the money from the excess points.");
		output.add("Good luck! Type ROLL when you're ready.");
		sendSkippableMessages(output);
		getInput();
	}

	@Override
	void playNextTurn(String input) {
		LinkedList<String> output = new LinkedList<>();

		if (input.equalsIgnoreCase("STOP")) {
			if (score == 0) {
				String message = "There's no risk yet, so ROLL!";
				output.add(message);
			} else {
				isAlive = false;
				output.add("Very well!");
				dice = new Dice(diceToRoll);
				dice.rollDice();
				output.add("You would have rolled: " + dice.toString());
				int pointsMissedOutOn = scoreDice(dice.getDice());
				output.add("... and that would have been worth " + (pointsMissedOutOn == 0
						? "**ZILCH**! Good move!"
						: String.format("%,d", pointsMissedOutOn) + " points."));
			}
		} else if (input.equalsIgnoreCase("ROLL")) {
			dice = new Dice(diceToRoll);
			dice.rollDice();
			output.add("You rolled: " + dice.toString());
			int rollValue = scoreDice(dice.getDice());

			String s1 = "...which scores ";
			if (rollValue == 0)
			{
				s1 += "**ZILCH**. Sorry.";
				score = 0;
				isAlive = false;
			} else {
				s1 += String.format("%s, worth %,d points", diceScoreString, rollValue);
				s1 += diceToRoll == 0 ? " and **HOT DICE**!" : "!";
				score += rollValue;
			}
			output.add(s1);

			if (isAlive)
			{
				String s2 = String.format("You now have %,d points (worth $%,d)",
						score, convertToDollars(score));

				if (score >= WINNING_SCORE) {
					s2 += "... which is enough to win! Congratulations! :smile:";
					output.add(s2);
					isAlive = false;
					Achievement.ZILCH_JACKPOT.check(getCurrentPlayer());
				} else {
					if(diceToRoll == 0)
					{
						diceToRoll = 6;
						s2 += " and 6 new dice to roll!";
					}
					else
						s2 += (" and " + diceToRoll + " di" + (diceToRoll == 1 ? "" : "c") + "e to roll.");
					output.add(s2);
					output.add("ROLL again if you dare, or type STOP to stop with your total.");
				}
			}
		}
		
		sendMessages(output);
		if(!isAlive)
			awardMoneyWon(convertToDollars(score));
		else
			getInput();
	}

	int convertToDollars(int points) {
		return points * applyBaseMultiplier(MONEY_PER_POINT);
	}

	int scoreDice(int[] diceFaces) {
		int diceScore = 0;
		diceScoreString = "";
		/*
		 * As with the columns on the scoring table, diceCount's index
		 * is one less than the number of pips on the face represented.
		 */
		int[] diceCount = new int[dice.getNumFaces()];

        for (int diceFace : diceFaces) {
            diceCount[diceFace - 1]++;
        }

		/*
		 * Combinations that only score with a fresh set of six dice go
		 * in this outer if block.
		 */
		if (diceToRoll == NUM_DICE) {
			if (isStraight(diceCount))
			{
				diceScoreString = "a straight";
				diceToRoll = 0;
				return STRAIGHT_SCORE;
			}
		
			if (countPairs(diceCount) == 3)
			{
				diceScoreString = "three pairs";
				diceToRoll = 0;
				return THREE_PAIRS_SCORE;
			}
		}

		int scoringFaces = 0;
		//Descending array so we can find the proper place for 'and' in the string
		for (int i = (diceCount.length-1); i >= 0; i--)
		{
			int faceScore = 0;
			if(diceCount[i] >= 3)
				faceScore = TRIPLE_DICE_SCORE[i] * BASE_TRIPLE_MULTIPLIER[diceCount[i]-3];
			else
				faceScore = (enhanced ? ENHANCED_SINGLE_DICE_SCORE[i] : SINGLE_DICE_SCORE[i]) * diceCount[i];
			if(faceScore != 0)
			{
				scoringFaces ++;
				String joiningString;
				switch(scoringFaces)
				{
				case 1: joiningString = ""; break;
				case 2: joiningString = "%s and "; break;
				default: joiningString = ", "; break;
				}
				diceScoreString = String.format("%s %d%s%s%s",
						ORDINALS[diceCount[i]-1], i+1, (diceCount[i] != 1 ? "s" : ""), joiningString, diceScoreString);
				diceScore += faceScore;
				diceToRoll -= diceCount[i];
			}
		}
		
		//Finally, decide whether or not to put a comma before the and
		switch(scoringFaces)
		{
		case 0:
		case 1:
			break;
		case 2:
			diceScoreString = String.format(diceScoreString, "");
			break;
		default:
			diceScoreString = String.format(diceScoreString, ",");
		}
		
		if(diceScore == 0 && score == 0) //first roll and no score, that's kind of impressive actually
		{
			diceScoreString = "NOTHING";
			diceToRoll = 0;
			return NO_SCORING_DICE_SCORE;
		}

		return diceScore;
	}

	boolean isStraight(int[] frequencyCount) {
		if (frequencyCount.length != dice.getNumFaces()) {
			throw new IllegalArgumentException("Each die has " +
					dice.getNumFaces() + " faces, but frequencyCount is of " +
					"length " + frequencyCount.length + ".");
		}

		int minFrequency = Integer.MAX_VALUE;
		int maxFrequency = Integer.MIN_VALUE;

        for (int j : frequencyCount) {
            if (j < minFrequency) {
                minFrequency = j;
            }
            if (j > maxFrequency) {
                maxFrequency = j;
            }
        }

		return minFrequency == 1 && maxFrequency == 1;
	}

	int countPairs(int[] frequencyCount) {
		int pairs = 0;
        for (int j : frequencyCount) {
            if (j == 2) {
                pairs++;
            }
        }

		return pairs;
	}

	@Override
	String getBotPick() {
		// The bot will always try to get at least the no-scoring-dice total
		if (score < NO_SCORING_DICE_SCORE) {
			return "ROLL";
		}
		//Otherwise, do a trial run and stop if we don't get any 1s or 5s
		Dice testDice = new Dice(diceToRoll);
		testDice.rollDice();
		int[] dice = testDice.getDice();
		for(int next : dice)
			if((enhanced ? ENHANCED_SINGLE_DICE_SCORE[next-1] : SINGLE_DICE_SCORE[next-1]) > 0)
				return "ROLL";
		return "STOP";
	}

	@Override
	void abortGame() {
		//Auto-stop, as it is a push-your-luck style game.
		awardMoneyWon(convertToDollars(score));
	}

	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	@Override public String getEnhanceText() {
		return "2s will now score 20 points.";
	}
	
}
