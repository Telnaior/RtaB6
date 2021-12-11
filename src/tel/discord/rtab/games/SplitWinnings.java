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
    static final int BASE_STARTING_BANK = 5_000;
    boolean isAlive;
    int stage;
    int[] scores;
    double[] multiplierSum;
    ArrayList<ArrayList<Double>> multipliers;
    boolean[] pickedSpaces;
    int[] numSpacesPicked;

    @Override
    void startGame() {
        LinkedList<String> output = new LinkedList<>();
        int startBank = applyBaseMultiplier(5_000);
        isAlive = true;
        stage = 0;
        scores = new int[] {startBank, startBank};
        multiplierSum = new double[2];
        numSpacesPicked = new int[2];

        multipliers = new ArrayList<>(Arrays.asList(
            // A multiplier of zero is a bomb
            new ArrayList<>(Arrays.asList(1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 2.0,
					2.0, 2.0, 2.0, 2.0, 2.0, 2.5, 0.0, 0.0, 0.0)),
            new ArrayList<>(Arrays.asList(1.5, 1.5, 1.5, 2.0, 2.0, 2.0, 2.5,
					2.5, 3.0, 3.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0))
        ));
        pickedSpaces = new boolean[BOARD_SIZE * 2];

        if(enhanced)
        	multipliers.get(0).set(BOARD_SIZE - 1, 3.0);
        	
        for (int i = 0; i < multipliers.size(); i++) {
            for (int j = 0; j < multipliers.get(i).size(); j++) {
                multiplierSum[i] += multipliers.get(i).get(j);
            }
            Collections.shuffle(multipliers.get(i));
        }

        output.add(String.format("In Split Winnings, you will be given two " +
				"starting banks of $%,d. The objective is to increase those " +
				"banks as high as possible by picking multipliers from each " + 
				"bank's associated %d-square board.", startBank,
				BOARD_SIZE));
        output.add("The first board has six 1.5x multipliers, six 2x " + 
				"multipliers, one 2.5x multiplier, and three bombs. " +
				"If you pick a bomb, you lose all the money in that bank.");
        if(enhanced)
        	output.add("ENHANCE BONUS: One of the bombs in the first board has been replaced with a 3x multiplier.");
        output.add("Once you bomb or decide to stop on the first board, you " +
				"will move on to the second board, which has three 1.5x " +
				"multipliers, three 2x multipliers, two 2.5x multipliers, two " +
				"3x multipliers, and one 5x multiplier, but five bombs.");
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

        display.append("```\n      SPLIT WINNINGS\n");
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
                double thisMultiplier = multipliers.get(i/BOARD_SIZE).get(i%BOARD_SIZE);
                if (thisMultiplier == 0.0) {
                    display.append("XX");
                } else {
                    display.append((int)Math.floor(thisMultiplier));
                    if (thisMultiplier < 10.0) {
                        double remainder = thisMultiplier -
								Math.floor(thisMultiplier);
                        if (remainder == 0.5) {
                            display.append("\u00BD");
                        } else {
                            display.append("x");
                        }
                    }
                }
                display.append(" ");
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
            output.add("Very well, we'll end this stage.");
            stage++;
            endValidTurn(output);
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
                    output.add("It's a **BOMB**. Unfortunately, you win nothing with this stage.");
                }

                if (selectedMultiplier == 0.0 || multiplierSum[stage] == 0.0)
                    stage++;
                
                endValidTurn(output);
            }
        }
		endTurn(output);
    }

    private void endValidTurn(LinkedList<String> output) {
        if (stage == 2)
            isAlive = false;

        output.add(generateBoard(!isAlive));
        if (isAlive) {
            output.add("Pick another space, or type STOP to end the stage.");
        }
        // The regular endTurn() method is always called thereafter
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
        if (getExpectedValue() > scores[stage])
        {
        	ArrayList<Integer> openSpaces = new ArrayList<>(BOARD_SIZE);
    		for(int i=0; i<BOARD_SIZE; i++)
    			if(!pickedSpaces[i+(stage*BOARD_SIZE)])
    				openSpaces.add(i+(stage*BOARD_SIZE)+1);
    		return String.valueOf(openSpaces.get((int)(Math.random()*openSpaces.size())));
        }
        else
        	return "STOP";
    }

    @Override
    void abortGame() {
        //Auto-stop, as it is a push-your-luck style game.
        awardMoneyWon(Math.max(scores[0], scores[1]));
    }
    
    private int getExpectedValue() {
        int expectedValue = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (!pickedSpaces[stage*BOARD_SIZE + i]) {
            	{
            		int thisSpaceValue = (int)(scores[stage] * multipliers.get(stage).get(i));
            		if(stage == 1)
            			thisSpaceValue = Math.max(scores[0], thisSpaceValue); //If we have guaranteed money from stage 1, factor that in
            		expectedValue += thisSpaceValue;
            	}
            }
        }
        expectedValue /= (BOARD_SIZE - numSpacesPicked[stage]);

        return expectedValue;
    }

	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	@Override public String getEnhanceText() { return "One bomb on the first board will become a 3x multiplier."; }
}
