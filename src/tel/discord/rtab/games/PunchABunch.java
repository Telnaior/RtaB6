package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

public class PunchABunch extends MiniGameWrapper {
	static final String NAME = "Punch a Bunch";
	static final String SHORT_NAME = "Punch";
	static final boolean BONUS = false;
	static final int BOARD_SIZE = 50;
	static final int MAX_TURNS = 4;
	boolean isAlive;
	int score;
	ArrayList<Integer> board = new ArrayList<Integer>(BOARD_SIZE);
	boolean[] pickedSpaces = new boolean[BOARD_SIZE];
	int turnsTaken;

	@Override
	void startGame() {
		isAlive = true;
		score = 0;
		// initialize game variables
		board.clear();
		board.addAll(Arrays.asList(
			0, 0, 0, 0, 0, // zeroes represent bombs
			10_000, 10_000, 10_000, 10_000, 10_000,
			25_000, 25_000, 25_000, 25_000, 25_000,
			50_000, 50_000, 50_000, 50_000, 50_000,
			50_000, 50_000, 50_000, 50_000, 50_000,
			100_000, 100_000, 100_000, 100_000, 100_000,
			100_000, 100_000, 100_000, 100_000, 100_000,
			250_000, 250_000, 250_000, 250_000, 250_000,
			250_000, 250_000, 250_000, 500_000, 500_000,
			500_000, 500_000, 1_000_000, 1_000_000, 2_500_000
		));
		if(enhanced) {
			for (int i = 5; i < 10; i++) {
				board.set(i, 25_000);
			}	
		}
		for (int i = 0; i < board.size(); i++) {
			board.set(i, applyBaseMultiplier(board.get(i)));
		}
		Collections.shuffle(board);
		turnsTaken = 0;
		
		LinkedList<String> output = new LinkedList<>();
		//Display instructions
		output.add("In Punch a Bunch, you will be making up to four punches on "
				+ "a 50-space board. Each space contains either a bomb or an "
				+ "amount of cash.");
		output.add("Five spaces contain bombs, "
				+ String.format("five spaces contain $%,d, ", applyBaseMultiplier(10_000))
				+ String.format("five spaces contain $%,d, ", applyBaseMultiplier(25_000))
				+ String.format("ten spaces contain $%,d, ", applyBaseMultiplier(50_000))
				+ String.format("ten spaces contain $%,d, ", applyBaseMultiplier(100_000))
				+ String.format("eight spaces contain $%,d, ", applyBaseMultiplier(250_000))
				+ String.format("four spaces contain $%,d, ", applyBaseMultiplier(500_000))
				+ String.format("two spaces contain $%,d, ", applyBaseMultiplier(1_000_000))
				+ String.format("and one space contains $%,d!", applyBaseMultiplier(2_500_000)));
		if(enhanced) {
			output.add(String.format("ENHANCE BONUS: All the $%,d spaces have been ", applyBaseMultiplier(10_000))
					+ String.format("increased to $%,d spaces.", applyBaseMultiplier(25_000)));  
		}
		output.add("After you make each punch, we'll add its value to your total.");
		output.add("You may stop after any punch, but if you draw a bomb, the "
				+ "game is over and you win nothing.");
		output.add("Good luck! Make your first punch when ready.");
		sendSkippableMessages(output);
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
					output.add("**" + lastPicked + "**!");
					score += lastPicked;
					if (turnsTaken == MAX_TURNS) {
						isAlive = false;
					}
				}
				output.add(displayBoard(!isAlive));
				if (isAlive) {
					output.add("Pick another space if you dare, or type STOP to stop with your current total.");
				}
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
		getInput();
	}

	@Override
	String getBotPick() {
		ArrayList<Integer> openSpaces = new ArrayList<>(BOARD_SIZE);
		for(int i=0; i<BOARD_SIZE; i++)
			if(!pickedSpaces[i])
				openSpaces.add(i+1);
		return String.valueOf(openSpaces.get((int)(Math.random()*openSpaces.size())));
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
		return "The lowest five cash spaces will be increased from $10,000 to "
				+ "$25,000.";
	}

	private String displayBoard(boolean reveal) {
		StringBuilder display = new StringBuilder();
		display.append("```\n        PUNCH-A-BUNCH\n         ");
		display.append(String.format("%,10d\n", score));
		for (int i = 0; i < pickedSpaces.length; i++) {
			if (reveal) {
				if (pickedSpaces[i]) {
					display.append("	");
				} else if (board.get(i) == 0) {
					display.append("BOMB");
				} else if (board.get(i) < 1_000) {
					display.append(String.format("$%3d", board.get(i)));
				} else if (board.get(i) < 10_000 && board.get(i) % 1_000 == 500) {
					display.append(String.format("%1d\u00BDK", board.get(i) / 1_000));
				} else if (board.get(i) < 100_000) {
					display.append(String.format("$%2dK", board.get(i) / 1_000));
				} else if (board.get(i) < 1_000_000) {
					display.append(String.format("%3dK", board.get(i) / 1_000));
				} else if (board.get(i) < 10_000_000 && board.get(i) % 1_000_000 == 500_000) {
					display.append(String.format("%1d\u00BDM", board.get(i) / 1_000_000));
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
		}
		display.append("\n```");
		return display.toString();
	}
}
