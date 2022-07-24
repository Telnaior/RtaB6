package tel.discord.rtab.games;

import java.util.LinkedList;

import tel.discord.rtab.Achievement;
import tel.discord.rtab.games.objs.Dice;

public class ShutTheBox extends MiniGameWrapper {
	static final String NAME = "Shut the Box";
	static final String SHORT_NAME = "Box";
	static final boolean BONUS = false;
	static final int BOARD_SIZE = 9;
	static final int MAX_SCORE = BOARD_SIZE * (BOARD_SIZE+1) / 2;
	int possibleRolls;
	boolean[] closedSpaces;
	Dice dice;
	String[] strategy;
	boolean allNumbersGood;
	int[] waysToClose;
	boolean insurance;
	boolean isAlive;
	boolean isClosing;
	byte totalShut;
	
	@Override
	void startGame() {
		LinkedList<String> output = new LinkedList<>();
		//Initialise board
		closedSpaces = new boolean[BOARD_SIZE];
		allNumbersGood = true;
		dice = new Dice();
		possibleRolls = dice.getDice().length * dice.getNumFaces() - (dice.getDice().length - 1);
		refreshGood();
		isAlive = true;
		insurance = enhanced;
		isClosing = false;
		totalShut = 0;
		
		//Display instructions
		output.add("In Shut the Box, you will be given a pair of six-sided dice "
				+ "and a box with the numbers 1 through " + BOARD_SIZE + " on it.");
		output.add("Your objective is to close all nine numbers.");
		output.add("Each time you roll the dice, you must close one or more " +
				"numbers that total *exactly* the amount thrown.");
		output.add("If you can do this, you will earn money depending on "
				+ "the roll. Higher rolls are more valuable, and all " +
				"rolls become more valuable as the game progresses.");
		output.add("If you shut the box completely, we'll augment your " +
				   "winnings to "+String.format("$%,d!",applyBaseMultiplier(1500000)));
		output.add("You are free to stop after any roll, but if you can't " +
				"exactly close the number thrown, you lose everything.");
		if(enhanced)
			output.add("ENHANCE BONUS: You can reroll one bad roll before losing.");
		output.add("Good luck! Type ROLL when you're ready.");
		sendSkippableMessages(output);
        sendMessage(generateBoard());
        getInput();
	}

	@Override
	void playNextTurn(String pick)
	{
	LinkedList<String> output = new LinkedList<>();
		
		if (!isClosing) {
			if (pick.equalsIgnoreCase("STOP")) {
				if (allNumbersGood) {
					String message = "There's no risk yet, so ROLL";
					if (totalShut != 0)
						message += " again";
					message += "!";
					output.add(message);
				} else {
					isAlive = false;
					output.add("Very well!");
					dice.rollDice();
					output.add("You would have rolled: " + dice.toString() + " (Total: " + dice.getDiceTotal() + ")");
				}
			}
			else if (pick.equalsIgnoreCase("ROLL")) {
				dice.rollDice();
				output.add("You rolled: " + dice.toString() + " (Total: " + dice.getDiceTotal() + ")");
				if(insurance && waysToClose[dice.getDiceTotal() - 2] == 0)
				{
					output.add("That is a bad roll, but we can reroll that once...");
					insurance = false;
					dice.rollDice();
					output.add("You rolled: " + dice.toString() + " (Total: " + dice.getDiceTotal() + ")");
				}
				if (waysToClose[dice.getDiceTotal() - 2] != 0) {
					if (totalShut + dice.getDiceTotal() == MAX_SCORE) {
						output.add("Congratulations, you shut the box!");
						totalShut = MAX_SCORE; // essentially closes the remaining numbers automatically
						Achievement.BOX_JACKPOT.check(getCurrentPlayer());
						isAlive = false;
					}
					else if (totalShut + dice.getDiceTotal() == MAX_SCORE - 1) { // ARGH!!!
						output.add("Oh, so close, yet you couldn't shut the " + 
								"1 :frowning2: We'll close that for you, then "
								+ "we'll give you your consolation prize.");
						totalShut = MAX_SCORE - 1; // essentially closes the remaining numbers except 1 automatically
						isAlive = false;
					}
                    else if (waysToClose[dice.getDiceTotal() - 2] == 1) {
                            output.add("There is only one way to close that number, so " +
                                    "we'll do it automatically for you.");
                            closeNumbers(strategy[dice.getDiceTotal() - 2].split("\\s"));
                            output.add(generateBoard());
                            output.add("ROLL again if you dare, or type STOP to stop " +
                                "with your total.");
                    }
					else {
						isClosing = true;
						output.add("What number(s) totaling " + dice.getDiceTotal()
								+ " would you like to close?");
					}
				}
				else {
					output.add("That is unfortunately a bad roll. Sorry.");
					totalShut = 0;
					isAlive = false;
				}
			}
		}
		else {
			String[] tokens = pick.split("\\s");
            for (String value : tokens) {
                if (!isNumber(value)) {
                    getInput();
                    return;
                }
            }
				
			// Make sure the numbers are actually in range and open
            for (String s : tokens) {
                if (Integer.parseInt(s) < 1 ||
                        Integer.parseInt(s) > 9 ||
                        closedSpaces[Integer.parseInt(s) - 1]) {
                    output.add("Invalid number(s).");
                    getInput();
                    return;
                }
            }
				
			// Duplicates are not allowed, so check for those
			for (int i = 0; i < tokens.length; i++)
				for (int j = i + 1; j < tokens.length; j++)
					if (tokens[i].equals(tokens[j])) {
						output.add("You can't duplicate a number.");
						getInput();
						return;
					}
				
			// Now we can sum everything and make sure it actually matches the roll
			int totalTryingToClose = 0;
            for (String token : tokens) totalTryingToClose += Integer.parseInt(token);
			
			if (totalTryingToClose == dice.getDiceTotal()) {
				closeNumbers(tokens);
				isClosing = false;
				output.add("Numbers closed.");
				output.add(generateBoard());
				if(allNumbersGood)
					output.add("There's no risk yet, so ROLL again!");
				else
					output.add("ROLL again if you dare, or type STOP to stop " +
						"with your total.");
			}
			else {
				output.add("That does not total the amount thrown.");
			}
		}
		sendMessages(output);
		if(!isAlive)
			awardMoneyWon(getMoneyWon());
		else
			getInput();
	}
	
	@Override
	public String getBotPick() {
		if (isClosing) {
			return strategy[dice.getDiceTotal() - 2];
		}
		else {
			Dice testDice = new Dice();
			testDice.rollDice();
			if (waysToClose[testDice.getDiceTotal()-2] != 0)
				return "ROLL";
			else return "STOP";
		}
	}	
	
	@Override
	void abortGame() {
		//Auto-stop, as it is a push-your-luck style game.
		awardMoneyWon(getMoneyWon());
	}

	boolean isNumber(String message)
	{
		try
		{
			//If this doesn't throw an exception we're good
			Integer.parseInt(message);
			return true;
		}
		catch(NumberFormatException e1)
		{
			return false;
		}
	}
	boolean checkValidNumber(String message)
	{
		int location = Integer.parseInt(message)-1;
		return (location >= 0 && location < BOARD_SIZE && !closedSpaces
				[location]);
	}
	
	String generateBoard()
	{
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append("  SHUT THE BOX\n");
		for(int i=0; i<BOARD_SIZE; i++)
		{
			if(closedSpaces[i])
			{
				display.append("  ");
			}
			else
			{
				display.append(i + 1).append(" ");
			}
		}
		display.append("\n Total: $").append(String.format("%,7d", getMoneyWon()));
		display.append("\n\n Possible Rolls:");
		for (int i = 0; i < waysToClose.length; i++) {
			if (waysToClose[i] != 0)
				display.append("\n ").append(String.format("%2d", i + 2)).append(": +$").append(String.format("%,9d", rollValue(i + 2)));
			else
				display.append("\n ").append(String.format("%2d", i + 2)).append(":   BAD ROLL");
		}
		display.append("\n```");
		return display.toString();
	}

        void closeNumbers(String[] numbers) {
            for (String number : numbers) closedSpaces[Integer.parseInt(number) - 1] = true;
            totalShut += dice.getDiceTotal();
            refreshGood();
        }
        
	void refreshGood() {
		strategy = new String[possibleRolls];
                waysToClose = new int[possibleRolls];
		
		// The numbers that are still open besides 1 are obviously still good, so start there.
		for (int i = 1; i < closedSpaces.length; i++)
			if(!closedSpaces[i]) {
				strategy[i-1] = Integer.toString(i+1);
                                waysToClose[i-1]++;
                        }
		
		for (int i = 1; i <= closedSpaces.length; i++) {
			// If the corresponding space is closed, don't bother with it
			if (closedSpaces[i-1])
				continue;
			for (int j = i+1; i+j < possibleRolls + 2 && j <= closedSpaces.length; j++) {
				if (closedSpaces[j-1])
					continue;
				// i-1 and j-1 must both be open; otherwise we wouldn't reach this point in the code
                waysToClose[i+j-2]++;
				if(strategy[i+j-2] == null || strategy[i+j-2].length() > 3)
					strategy[i+j-2] = i + " " + j;
				for (int k = j+1; i+j+k < possibleRolls + 2; k++) {
					if (closedSpaces[k-1])
						continue;
	                waysToClose[i+j+k-2]++;
					if(strategy[i+j+k-2] == null || strategy[i+j+k-2].length() > 5)
						strategy[i+j+k-2] = i + " " + j + " " + k;
					for (int l = k+1; i+j+k+l < possibleRolls + 2; l++) {
						if (closedSpaces[l-1])
							continue;
	                    waysToClose[i+j+k+l-2]++;
						if(strategy[i+j+k+l-2] == null || strategy[i+j+k+l-2].length() > 7)
							strategy[i+j+k+l-2] = i + " " + j + " " + k + " " + l;
					}
				}
			}
		}
		//Finally, check if every number is still good and set the boolean if it isn't
		if(allNumbersGood)
			for(int nextRoll : waysToClose)
				if(nextRoll == 0)
				{
					allNumbersGood = false;
					break;
				}
	}
		
	public int rollValue(int roll) {
		if (waysToClose[roll-2] == 0)
			return getMoneyWon() * -1;
		// The base multiplier should **NOT** be applied to the difference; it is already applied to the operands.
		return getMoneyWon(totalShut+roll) - getMoneyWon();
	}
	
	public int findNthTetrahedralNumber(int n) {
		return n*(n+1)*(n+2)/6;
	}

	public int getMoneyWon()
	{
		return getMoneyWon(totalShut);
	}

	public int getMoneyWon(int score)
	{
		if (score == MAX_SCORE)
			return applyBaseMultiplier(1500000);
		else return applyBaseMultiplier(findNthTetrahedralNumber(score) * 50);
	}

	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	@Override public String getEnhanceText() { return "You will automatically reroll after one bad roll."; }
}
