package tel.discord.rtab.games;

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
    double[][] multipliers;
    boolean[] pickedSpaces;

    @Override
    void startGame() {
        LinkedList<String> output = new LinkedList<>();

        isAlive = true;
        stage = 0;
        scores = new int[] {STARTING_BANK, STARTING_BANK};
        multipliers = new double[][] {
            // A multiplier of zero is a bomb
            new double[] {1.5, 1.5, 1.5, 1.5, 1.5, 2, 2, 2, 2, 2.5, 2.5, 2.5, 3, 3, 0, 0}, 
            new double[] {2, 2, 2, 2, 3, 3, 3, 4, 4, 5, 10, 0, 0, 0, 0, 0}
        };
        pickedSpaces = new boolean[BOARD_SIZE * 2];

        output.add(String.format("In Split Decision, you will be given two starting banks of $%,d. The objective is to increase those banks as high as possible by picking multipliers from each bank's associated %d-square board.", STARTING_BANK, BOARD_SIZE));
        output.add("The first board has five 1.5x multipliers, four 2x multipliers, three 2.5x multipliers, two 3x multipliers, and two bombs. If you pick a bomb, you lose all the money in that bank.");
        output.add("Once you bomb or decide to stop on the first board, you will move on to the second board, which has four 2x multipliers, three 3x multipliers, two 4x multipliers, one 5x multiplier, and one 10x multiplier, but five bombs.");
        output.add("Once you bomb or decide to stop on the second board, the game ends and you will win whichever of the two banks is higher.");
        output.add("Good luck! You will begin on the first board for now.");

        sendSkippableMessages(output);
        sendMessage(generateBoard());
        getInput();
    }

    private String generateBoard() {
        StringBuilder display = new StringBuilder();
		return display.toString();
    }

    @Override
    void playNextTurn(String pick) {
        LinkedList<String> output = new LinkedList<>();

        if (pick.equalsIgnoreCase("STOP")) {
            stage++;
        }
        else if (isNumber(pick)) {
            int selection = Integer.parseInt(pick);
            double selectedMultiplier = multipliers[stage][selection];
            scores[stage] = (int)(scores[stage] * selectedMultiplier);

            if (selectedMultiplier == 0.0)
                stage++;
        }

        if (stage == 2)
            isAlive = false;

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
        // TODO Auto-generated method stub
        return null;
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
    
}
