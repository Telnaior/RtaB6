package tel.discord.rtab.games;

import java.util.LinkedList;

public class SplitWinnings extends MiniGameWrapper {
	static final String NAME = "Money Cards";
	static final String SHORT_NAME = "Cards";
    static final boolean BONUS = false;
	static final int BOARD_SIZE = 16;
    boolean isAlive, isOnFirstStage;
    int[] scores;
    double[][] multipliers;

    @Override
    void startGame() {
        LinkedList<String> output = new LinkedList<>();

        isAlive = isOnFirstStage = true;
        scores = new int[] {5000, 5000};
        multipliers = new double[][] {
            // A multiplier of zero is a bomb
            new double[] {1.5, 1.5, 1.5, 1.5, 1.5, 2, 2, 2, 2, 2.5, 2.5, 2.5, 3, 3, 0, 0}, 
            new double[] {2, 2, 2, 2, 3, 3, 3, 4, 4, 5, 10, 0, 0, 0, 0, 0}
        };
        

        sendSkippableMessages(output);
        getInput();
    }

    @Override
    void playNextTurn(String input) {
        // TODO Auto-generated method stub
        
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
