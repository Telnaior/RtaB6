package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

import tel.discord.rtab.Achievement;

public class PunchABunch extends MiniGameWrapper {
	static final String NAME = "Punch a Bunch";
	static final String SHORT_NAME = "Punch";
	static final boolean BONUS = false;
	static final int BOARD_SIZE = 50;
	static final int MAX_TURNS = 4;
	//Zeroes in cash values represent bombs
	static final int[] CASH_VALUES = new int[] {0, 10_000, 50_000, 100_000, 200_000, 300_000, 500_000, 1_000_000, 5_000_000};
	static final int[] CASH_FREQUENCY = new int[] {5,5,5,10,10,8,4,2,1};
	boolean isAlive;
	int score;
	int bombCount;
	ArrayList<Integer> board = new ArrayList<>(BOARD_SIZE);
	boolean[] pickedSpaces = new boolean[BOARD_SIZE];
	int turnsTaken;

	@Override
	void startGame() {
		isAlive = true;
		score = 0;
		// initialize game variables
		board.clear();
		//cash frequency table tells us how many of each value to add
		for(int i=0; i<CASH_VALUES.length; i++)
			for(int j=0; j<CASH_FREQUENCY[i]; j++)
				board.add(CASH_VALUES[i]);
		//modify for enhancement if necessary
		if(enhanced) {
			for (int i = 5; i < 10; i++) {
				board.set(i, CASH_VALUES[3]);
			}	
		}
		//base multiplier everything
        board.replaceAll(this::applyBaseMultiplier);
		//and shuffle
		Collections.shuffle(board);
		turnsTaken = 0;
		bombCount = CASH_FREQUENCY[0];
		
		LinkedList<String> output = new LinkedList<>();
		//Display instructions
		output.add("In Punch a Bunch, you will be making up to four punches on "
				+ "a 50-space board. Each space contains either a bomb or an "
				+ "amount of cash.");
		output.add("Five spaces contain bombs, "
				+ String.format("five spaces contain $%,d, ", applyBaseMultiplier(CASH_VALUES[1]))
				+ String.format("five spaces contain $%,d, ", applyBaseMultiplier(CASH_VALUES[2]))
				+ String.format("ten spaces contain $%,d, ", applyBaseMultiplier(CASH_VALUES[3]))
				+ String.format("ten spaces contain $%,d, ", applyBaseMultiplier(CASH_VALUES[4]))
				+ String.format("eight spaces contain $%,d, ", applyBaseMultiplier(CASH_VALUES[5]))
				+ String.format("four spaces contain $%,d, ", applyBaseMultiplier(CASH_VALUES[6]))
				+ String.format("two spaces contain $%,d, ", applyBaseMultiplier(CASH_VALUES[7]))
				+ String.format("and one space contains $%,d!", applyBaseMultiplier(CASH_VALUES[8])));
		output.add("After you make each punch, we'll add its value to your total, but we'll also convert the lowest remaining cash value to bombs.");
		if(enhanced) {
			output.add(String.format("ENHANCE BONUS: All the $%,d spaces have been ", applyBaseMultiplier(CASH_VALUES[1]))
					+ String.format("increased to $%,d spaces (so won't convert to bombs until the last punch).", applyBaseMultiplier(CASH_VALUES[3])));  
		}
		output.add("You may stop after any punch, but if you draw a bomb, the "
				+ "game is over and you win nothing.");
		output.add("Good luck! Make your first punch when ready.");
		sendSkippableMessages(output);
		sendMessage(displayBoard(false));
		getInput();
	}

	@Override
	void playNextTurn(String input) {
		LinkedList<String> output = new LinkedList<>();
		if (isNumber(input)) {
			int pick = Integer.parseInt(input);
			if (!checkValidNumber(pick)) {
				output.add("Invalid pick.");
			} else if (pickedSpaces[pick-1]) {
				output.add("That space has already been picked.");
			} else {
				turnsTaken++;
				pickedSpaces[pick-1] = true;
				int lastPicked = board.get(pick-1);
				output.add(String.format("Space %d selected...",pick));
				if (lastPicked == 0) {
					output.add("It's a **BOMB**.");
					score = 0;
					isAlive = false;
				} else {
					output.add(String.format("**$%,d!**", lastPicked));
					score += lastPicked;
					if (turnsTaken == MAX_TURNS)
					{
						if(lastPicked == applyBaseMultiplier(CASH_VALUES[8]))
						{
							sendMessages(output);
							output.clear();
							Achievement.PUNCH_JACKPOT.check(getCurrentPlayer());
						}
						output.add("That's all the punches!");
						isAlive = false;
					}
				}
				if (isAlive)
				{
					for(int i=0; i<BOARD_SIZE; i++)
					{
						//turn the lowest cash value to bombs each round
						if(!pickedSpaces[i] && board.get(i) == applyBaseMultiplier(CASH_VALUES[turnsTaken]))
						{
							board.set(i, 0);
							bombCount++;
						}
					}
					if(!enhanced || turnsTaken != 1) //Let's not print this if we know it doesn't mean anything
						output.add(String.format("All $%,d spaces have been converted to bombs (for a total of %d bombs...)",
								applyBaseMultiplier(CASH_VALUES[turnsTaken]), bombCount));
					output.add("Punch another space if you dare, or type STOP to stop with your current total.");
				}
				output.add(displayBoard(!isAlive));
			}
		} else if (input.equalsIgnoreCase("STOP")) {
			if (turnsTaken == 0) {
				output.add("You have nothing to lose yet, so why not pick a space?");
			} else {
				isAlive = false;
				output.add(displayBoard(true));
			}
		}
		sendMessages(output);
		if(isGameOver())
			awardMoneyWon(score);
		else
			getInput();
	}

	@Override
	String getBotPick() {
		if (score == 0 || Math.random()*(BOARD_SIZE - turnsTaken) >= bombCount) {
			ArrayList<Integer> openSpaces = new ArrayList<>(BOARD_SIZE);
			for(int i=0; i<BOARD_SIZE; i++)
				if(!pickedSpaces[i])
					openSpaces.add(i+1);
			return String.valueOf(openSpaces.get((int)(Math.random()*openSpaces.size())));
		}
		return "STOP";
	}

	@Override
	void abortGame() {
		//Auto-stop, as it is a push-your-luck style game.
		awardMoneyWon(score);
	}

	private boolean checkValidNumber(int number)
	{
		return (number > 0 && number <= BOARD_SIZE);
	}

	boolean isNumber(String message) {
		try {
			//If this doesn't throw an exception we're good
			Integer.parseInt(message);
			return true;
		} catch(NumberFormatException e1) {
			return false;
		}
	}

	boolean isGameOver() {
		return !isAlive || turnsTaken == MAX_TURNS;
	}

	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	@Override public String getEnhanceText() {
		return "The lowest five cash spaces will be increased tenfold (and won't convert to bombs until the last punch).";
	}

	private String displayBoard(boolean reveal) {
		StringBuilder display = new StringBuilder();
		switch(turnsTaken)
		{
		case 0: display.append("```\n        PUNCH-A-BUNCH        \n");	break;
		case 1:	display.append("```\n  X     PUNCH-A-BUNCH        \n");	break;
		case 2:	display.append("```\n  X  X  PUNCH-A-BUNCH        \n");	break;
		case 3:	display.append("```\n  X  X  PUNCH-A-BUNCH  X     \n");	break;
		case 4:	display.append("```\n  X  X  PUNCH-A-BUNCH  X  X  \n");	break;
		}
		display.append(String.format("         $%,10d\n", score));
		for (int i = 0; i < pickedSpaces.length; i++) {
			if (reveal) {
				if (pickedSpaces[i]) {
					display.append("    ");
				} else if (board.get(i) == 0) {
					display.append("BOMB");
				} else if (board.get(i) < 1_000) {
					display.append(String.format("$%3d", board.get(i)));
				} else if (board.get(i) < 10_000 && board.get(i) % 1_000 == 500) {
					display.append(String.format("$%1d\u00BDK", board.get(i) / 1_000));
				} else if (board.get(i) < 100_000) {
					display.append(String.format("$%2dK", board.get(i) / 1_000));
				} else if (board.get(i) < 1_000_000) {
					display.append(String.format("%3dK", board.get(i) / 1_000));
				} else if (board.get(i) < 10_000_000 && board.get(i) % 1_000_000 == 500_000) {
					display.append(String.format("$%1d\u00BDM", board.get(i) / 1_000_000));
				} else if (board.get(i) < 100_000_000) {
					display.append(String.format("$%2dM", board.get(i) / 1_000_000));
				} else {
					display.append(String.format("%3dM", board.get(i) / 1_000_000));
				}
			} else {
				if (pickedSpaces[i]) {
					display.append("  ");
				} else {
					display.append(String.format("%02d",(i+1)));
				}
			}
			display.append(" ");
			if ((i+1) % 10 == 0) {
				display.append("\n");
			}
		}
		display.append("```");
		return display.toString();
	}
	
	/* uncomment this if we want to use it in future
	private int getExpectedValue()
	{
		int sum = 0;
		for (int i = 0; i < BOARD_SIZE; i++) {
			if (pickedSpaces[i]) {
				continue;
			} else if (board.get(i) == 0) {
				sum -= score;
			} else {
				sum += board.get(i);
			}
		}
		return sum/(BOARD_SIZE - turnsTaken);
	}
	*/
}
