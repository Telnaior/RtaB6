package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

public class SplitWinnings extends MiniGameWrapper {
	static final String NAME = "Split Winnings";
	static final String SHORT_NAME = "Split";
    static final boolean BONUS = false;
	static final int BOARD_SIZE = 16;
    static final int STARTING_BANK = 5000;
    boolean isAlive;
    int stage;
    int[] scores;
    double[] multiplierSum;
    ArrayList<ArrayList<Double>> multipliers;
    boolean[] pickedSpaces;
    int[] numSpacesPicked;
    ArrayList<ArrayList<Integer>> botPick;

    @Override
    void startGame() {
        LinkedList<String> output = new LinkedList<>();

        isAlive = true;
        stage = 0;
        scores = new int[] {STARTING_BANK, STARTING_BANK};
        multiplierSum = new double[2];
        numSpacesPicked = new int[2];

        multipliers = new ArrayList<>(Arrays.asList(
            // A multiplier of zero is a bomb
            new ArrayList<>(Arrays.asList(1.5, 1.5, 1.5, 1.5, 1.5, 2.0, 2.0,
					2.0, 2.0, 2.5, 2.5, 2.5, 3.0, 3.0, 0.0, 0.0)),
            new ArrayList<>(Arrays.asList(2.0, 2.0, 2.0, 2.0, 3.0, 3.0, 3.0,
					4.0, 4.0, 5.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0))
        ));
        pickedSpaces = new boolean[BOARD_SIZE * 2];

        for (int i = 0; i < multipliers.size(); i++) {
            for (int j = 0; j < multipliers.get(i).size(); j++) {
                multiplierSum[i] += multipliers.get(i).get(j);
            }
            Collections.shuffle(multipliers.get(i));
        }

        if (super.getCurrentPlayer().isBot) {
            botPick = new ArrayList<>(Arrays.asList(new ArrayList<>(BOARD_SIZE), new ArrayList<>(BOARD_SIZE)));
            for (int i = 0; i < botPick.size(); i++) {
                botPick.get(i/BOARD_SIZE).add(i + 1);
            }

            for (int i = 0; i < BOARD_SIZE; i++) {
                Collections.shuffle(botPick.get(i));
            }
        }

        output.add(String.format("In Split Decision, you will be given two " +
				"starting banks of $%,d. The objective is to increase those " +
				"banks as high as possible by picking multipliers from each " + 
				"bank's associated %d-square board.", STARTING_BANK,
				BOARD_SIZE));
        output.add("The first board has five 1.5x multipliers, four 2x " + 
				"multipliers, three 2.5x multipliers, two 3x multipliers, " +
				"and two bombs. If you pick a bomb, you lose all the money " +
				"in that bank.");
        output.add("Once you bomb or decide to stop on the first board, you " +
				"will move on to the second board, which has four 2x " +
				"multipliers, three 3x multipliers, two 4x multipliers, one " +
				"5x multiplier, and one 10x multiplier, but five bombs.");
        output.add("Once you bomb or decide to stop on the second board, " +
				"the game ends and you will win whichever of the two banks " +
				"is higher.");
        output.add("Good luck! You will begin on the first board for now.");

        sendSkippableMessages(output);
        sendMessage(generateBoard());
        getInput();
    }

    private String generateBoard() {
        return generateBoard(false);
    }

    private String generateBoard(boolean reveal) {
        StringBuilder display = new StringBuilder();
        final int BOARD_WIDTH = 4;

        display.append("```\n     SPLIT WINNINGS\n");
        for (int i = 0; i < BOARD_SIZE * 2; i++) {
            /* Order is 1, 2, 3, 4, 17, 18, 19, 20, 5, 6, 7, 8, 21, 22,
			 * 23, 24, etc.
			 */
            if (i % BOARD_WIDTH == 0 && i > 0) {
                if (i <= BOARD_SIZE) {
                    display.append("   ");
                    i += (BOARD_SIZE - BOARD_WIDTH);
                } else {
                    display.append("\n");
                    i -= BOARD_SIZE;
                }
            }

            if (pickedSpaces[i]) {
                display.append("   ");
            } else if (reveal) {
                /* At present the reveal only supports multipliers that
				 * are multiples of 0.5 up to 10x, above which point only
				 * integer multipliers are supported.
				 */
                double thisMultiplier = multipliers.get(stage)
						.get(i - stage*BOARD_SIZE);
                if (thisMultiplier == 0.0) {
                    display.append("XX");
                } else {
                    display.append((int)Math.floor(thisMultiplier));
                    if (thisMultiplier < 10.0) {
                        double remainder = thisMultiplier -
								Math.floor(thisMultiplier);
                        if (remainder == 0.5) {
                            display.append("½");
                        } else {
                            display.append("x");
                        }
                    }
                }
            } else {
                display.append(String.format("%02d ",(i+1)));
            }
        }
        display.append(String.format("\n\n$%,10d", scores[0]) + " " +
				(stage == 0 ? "< " : " >") + " " +
				String.format("$%,10d", scores[1]));
        display.append("\n```");
		return display.toString();
    }

    @Override
    void playNextTurn(String pick) {
        LinkedList<String> output = new LinkedList<>();

        if (pick.equalsIgnoreCase("STOP")) {
            stage++;
        }
        else if (isNumber(pick)) {
            if (Integer.parseInt(pick) < stage * BOARD_SIZE + 1 ||
                    Integer.parseInt(pick) > (stage+1) * BOARD_SIZE) {
                output.add("Invalid selection.");
            } else if (pickedSpaces[Integer.parseInt(pick) - 1]) {
                output.add("That space has already been selected.");
            } else {
                numSpacesPicked[stage]++;
                output.add("Space " + pick + " selected...");

                int selection = Integer.parseInt(pick) - 1;
                pickedSpaces[selection] = true;
                double selectedMultiplier = multipliers.get(stage)
                        .get(selection - (stage * BOARD_SIZE));
                scores[stage] = (int)(scores[stage] * selectedMultiplier);

                if (selectedMultiplier == 0.0 || Math.random() <
                        (double)numSpacesPicked[stage]/(double)BOARD_SIZE) {
                    output.add("...");
                }

                if (selectedMultiplier > 0.0) {
                    multiplierSum[stage] -= selectedMultiplier;
                    output.add("**" + selectedMultiplier + "x**!");
                    output.add(String.format("That brings your Stage %d score " + 
                    "to $%,d!", stage + 1, scores[stage]));
                    if (multiplierSum[stage] == 0.0) {
                        output.add("Every other space in this stage is a bomb, " +
                        "so we'll automatically end the stage.");
                    }
                } else {
                    output.add("It's a **BOMB**. It goes **BOOM**.");
                }

                if (selectedMultiplier == 0.0 || multiplierSum[stage] == 0.0)
                    stage++;
            }

            if (stage == 2)
                isAlive = false;

            output.add(generateBoard(!isAlive));
            if (isAlive) {
                output.add("Pick another space, or type STOP to end the stage.");
            }
        }
		endTurn(output);
    }

	private void endTurn(LinkedList<String> output)
	{
		sendMessages(output);
		if(!isAlive)
            awardMoneyWon(Math.max(scores[0], scores[1]));
		else
			getInput();
	}

    @Override
    String getBotPick() {
        if (getExpectedValue() > 0)
            return botPick.get(stage).get(numSpacesPicked[stage]).toString();
        else return "STOP";
    }

    @Override
    void abortGame() {
        //Auto-stop, as it is a push-your-luck style game.
        awardMoneyWon(Math.max(scores[0], scores[1]));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getShortName() {
        return SHORT_NAME;
    }

    @Override
    public boolean isBonus() {
        return BONUS;
    }
    
    private int getExpectedValue() {
        int expectedValue = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (!pickedSpaces[stage*BOARD_SIZE + i]) {
                expectedValue += (scores[stage] * (multipliers.get(stage).get(i) - 1.0));
            }
        }
        expectedValue /= (BOARD_SIZE - numSpacesPicked[stage]);

        return expectedValue;
    }
}
