package tel.discord.rtab.games;

import tel.discord.rtab.RtaBMath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

public class FiftyYardDash extends MiniGameWrapper {
	static final String NAME = "50-Yard Dash";
	static final String SHORT_NAME = "Dash";
	static final boolean BONUS = false;
	static final int STAGES = 5;
	// enhanced is not static; therefore, spacesPerStage cannot be. It shouldn't be mutable, though.
	private int spacesPerStage;
	// Since spacesPerStage cannot be static, neither can boardSize.
	private int boardSize;
	boolean isAlive;
	boolean isGameOver;
	byte stage;
	int[] stageMoney;
	boolean[] isSafe;
	boolean[] pickedSpaces;

	@Override
	void startGame() {
		LinkedList<String> output = new LinkedList<>();
		// initialize game variables
		// spacesPerStage and boardSize are initialized here to keep SonarLint happy.
		spacesPerStage = enhanced ? 10 : 9;
		boardSize = STAGES * spacesPerStage;
		isAlive = true;
		isGameOver = false;
		stage = 0;
		stageMoney = new int[]{
				applyBaseMultiplier(50_000),
				applyBaseMultiplier(150_000),
				applyBaseMultiplier(350_000),
				applyBaseMultiplier(750_000),
				applyBaseMultiplier(2_000_000),
				applyBaseMultiplier(5_000_000)
		};
		isSafe = new boolean[boardSize];
		pickedSpaces = new boolean[isSafe.length];

		//Randomize board
		for (int i = 0; i < STAGES; i++) {
			ArrayList<Boolean> thisRow = new ArrayList<>(10);
			for (int j = 0; j < spacesPerStage; j++) {
				thisRow.add(j >= STAGES - i);
			}
			Collections.shuffle(thisRow);

			for (int j = 0; j < spacesPerStage; j++) {
				isSafe[spacesPerStage * i + j] = thisRow.get(j);
			}
		}

		//Display instructions
		output.add("In 50-Yard Dash, you are a player on an American football field trying to make your way to the " +
				"goal line to make a touchdown.");
		output.add(String.format("You will start at midfield, on the 50-yard line, with a bank of $%,d.",
				stageMoney[0]));
		output.add(String.format("There are nine spaces at that yard line. Eight of them are safe; one contains a " +
				"blocker. If you pick a safe space, you will advance 10 yards closer to the goal line and your bank " +
				"will increase to $%,d. If you pick a blocker, however, your run ends and you win nothing.",
				stageMoney[1]));
		output.add("At the 40-yard line, you will have nine new spaces, seven safe and two hiding blockers. Each " +
				"yard line after that will have one less safe space and one more blocker than the last.");
		if(enhanced)
			output.add("ENHANCE BONUS: One additional safe space has been added to each line.");
		output.add(String.format("Reaching the 30-yard line is worth $%,d, reaching the 20-yard line is worth $%,d, " +
				"and reaching the 10-yard line is worth $%,d.", stageMoney[2], stageMoney[3], stageMoney[4]));
		output.add(String.format("If you reach the goal line and make a touchdown, you will win $%,d!", stageMoney[5]));
		output.add("Good luck!");
		sendSkippableMessages(output);
		output.clear();
		output.add(generateBoard(false));
		String s = showBankAndSpaceCount();
		s += (".\nChoose a space if you dare, or type STOP to stop with your current total.");
		output.add(s);
		sendMessages(output);
		getInput();
	}

	@Override
	void playNextTurn(String input) {
		LinkedList<String> output = new LinkedList<>();

		if (input.equalsIgnoreCase("STOP")) {
			output.add("Very well, then.");
			sendMessage(generateBoard(true));
			isGameOver = true;
		}
		else if (isNumber(input)) {
			int pick = Integer.parseInt(input);

			if (pick < 0 || pick > boardSize) {
				output.add("Invalid pick.");
				if (getPlayer().isBot) {
					abortGameDueToBotMalfunction();
					return;
				}
			} else if (pick > boardSize - spacesPerStage * stage) {
				output.add("You have already passed that line.");
				if (getPlayer().isBot) {
					abortGameDueToBotMalfunction();
					return;
				}
			} else if (pick < boardSize - spacesPerStage * (stage+1) + 1) {
				output.add("You have not reached that line yet.");
				if (getPlayer().isBot) {
					abortGameDueToBotMalfunction();
					return;
				}
			} else {
				pickedSpaces[pick-1] = true;
				boolean safe = isSafe[pick-1];

				output.add(String.format("Space %d selected...",pick));
				if (!safe || RtaBMath.random() < (stage+1.0)/spacesPerStage) {
					output.add("...");
				}
				if (safe) {
					output.add("It's **SAFE**!");
					stage++;
					if (stage == STAGES)
						isGameOver = true;
				} else {
					output.add("It's a **BLOCKER**, your run is over.");
					isAlive = false;
					isGameOver = true;
				}
				output.add(generateBoard(isGameOver));

				if (isAlive) {
					if (isGameOver) {
						output.add(String.format("**TOUCHDOWN %s!** Congratulations!",
								getPlayer().getName().toUpperCase()));
					} else {
						String s = showBankAndSpaceCount();
						s += (".\nChoose another space if you dare, or type STOP to stop with your current total.");
						output.add(s);
					}
				}
			}

		}

		sendMessages(output);
		if(isGameOver)
			awardMoneyWon(isAlive ? stageMoney[stage] : 0);
		else
			getInput();
	}

	@Override
	String getBotPick() {
		if (RtaBMath.random() > (stage+1.0)/8.0) {
			return "STOP";
		}
		return String.valueOf(boardSize - spacesPerStage *(stage+1)
				+ (int)(RtaBMath.random()* spacesPerStage) + 1);
	}

	private String generateBoard(boolean reveal)
	{
		StringBuilder display = new StringBuilder();

		display.append("```\n");
		display.append("   +-");
		display.append("---".repeat(Math.max(0, spacesPerStage)));
		display.append("+\n");

		/* TODO: Printing the end zone only works correctly when the regular board width is 9 and the enhanced
		 * board width is 10. Decouple from the board width and the enhanced flag.
		 */
		display.append(stage==STAGES ? "->" : "  ");
		display.append(enhanced?" |    5 0 - Y A R D   D A S H    | ":" |  5 0 - Y A R D    D A S H  | ");
		display.append(stage==STAGES ? "<-\n" : "\n");

		display.append(printGoalLine());

		for (int i = 0; i < STAGES; i++) {
			display.append(stage == STAGES - i - 1 ? "->" : "  ");
			display.append(" | ");
			for (int j = spacesPerStage * i; j < spacesPerStage * (i+1); j++) {
				display.append(printSpace(reveal, j));
			}
			display.append("| ");
			display.append(stage == STAGES - i - 1 ? "<-\n" : "\n");

			display.append(printYardLine(Integer.toString((i+1) * 10)));
		}
		display.append("```");
		return display.toString();
	}

	private String printSpace(boolean reveal, int space) {
		if (pickedSpaces[space]) {
			return isSafe[space] ? "   " : "XX ";
		} else {
			return reveal && !isSafe[space] ? "XX " : String.format("%02d ", (space+1));
		}
	}

	private String printGoalLine() {
		String goalLine = " G |=";
		goalLine += "===".repeat(Math.max(0, spacesPerStage));
		goalLine += "| G\n";
		return goalLine;
	}

	private String printYardLine(String marker) {
		String yardLine = marker + " |-";
		yardLine += "---".repeat(Math.max(0, spacesPerStage));
		yardLine += "| " + marker + "\n";
		return yardLine;
	}

	/**
	 * @return the number of safe spaces between start and end inclusive
	 */
	private int countSafeSpaces (int start, int end) {
		int safeSpacesFound = 0;
		for (int i = start-1; i <= end-1; i++) {
			if (isSafe[i]) {
				safeSpacesFound++;
			}
		}
		return safeSpacesFound;
	}

	private String showBankAndSpaceCount() {
		int safeSpaces = countSafeSpaces(boardSize - spacesPerStage * (stage+1) + 1,
				boardSize - spacesPerStage * stage);
		int bombSpaces = spacesPerStage - safeSpaces;

		String s = String.format("You have $%,d. Standing between you and the next yard line are "
				, stageMoney[stage]);
		s += (safeSpaces + " safe space");
		if (safeSpaces != 1)
			s += "s";
		s += (" and " + bombSpaces + " blocker");
		if (bombSpaces != 1)
			s += "s";
		return s;
	}

	void abortGameDueToBotMalfunction() {
		sendMessage("AI broke");
		abortGame();
	}

	@Override
	void abortGame() {
		awardMoneyWon(stageMoney[stage]);
	}

	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	@Override public String getEnhanceText() {
		return "One safe space will be added to each line, for a total of ten spaces per line instead of nine.";
	}
}
