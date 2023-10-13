package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import tel.discord.rtab.RtaBMath;

public class SplitWinnings extends MiniGameWrapper {
	static final String NAME = "Split Winnings";
	static final String SHORT_NAME = "Split";
    static final boolean BONUS = false;
	static final int BOARD_SIZE = 16;
	static final int MAX_STAGES = 3;
	static final List<Integer> BASE_CASH = Arrays.asList(5_000, 5_000, 10_000, 10_000, 15_000, 15_000, 20_000, 25_000,
			30_000, 35_000, 40_000, 45_000, 50_000, 0, 0, 0);
	static final List<Double> BASE_MULTIPLIERS = Arrays.asList(1.5, 1.5, 1.5, 2.0, 2.0, 2.0, 2.5, 2.5, 2.5,
			3.0, 4.0, 5.0, 0.0, 0.0, 0.0, 0.0);
    boolean isAlive;
    int stage;
    int[] scores;
    ArrayList<Integer> cashAmounts;
    ArrayList<Double> multipliers;
    boolean[] pickedSpaces;
    int[] numBombsLeft;
    int[] numSpacesLeft;

    @Override
    void startGame() {
        LinkedList<String> output = new LinkedList<>();
        isAlive = true;
        stage = 0;
        scores = new int[MAX_STAGES];
        numBombsLeft = new int[] {3, 4};
        numSpacesLeft = new int[] {BOARD_SIZE, BOARD_SIZE};
        // A multiplier of zero is a bomb
        cashAmounts = new ArrayList<>();
        cashAmounts.addAll(BASE_CASH);
        cashAmounts.replaceAll(this::applyBaseMultiplier);
        Collections.shuffle(cashAmounts);
        multipliers = new ArrayList<>();
        multipliers.addAll(BASE_MULTIPLIERS);
        Collections.shuffle(multipliers);
        pickedSpaces = new boolean[BOARD_SIZE * 2];

        output.add(String.format("In Split Winnings, you will be given %d " +
				"banks. Your objective is to increase those " +
				"banks as high as possible by picking cash values and multipliers "
				+ "from two %d-space boards.", MAX_STAGES, BOARD_SIZE));
        output.add(String.format("The first board has cash values ranging from $%,d to $%,d, and three bombs.",
				applyBaseMultiplier(20_000), applyBaseMultiplier(50_000)));
        output.add("The second board has cash multipliers ranging from 1.5x to 5x, and four bombs.");
        output.add("You can STOP at any time to secure your current bank and move on to the next, "
        		+ "but if you bomb at any point then you lose your current bank and all the money it contains.");
        output.add("Once you have played all three banks, you will win the largest of the three.");
        if(enhanced)
        	output.add("ENHANCE BONUS: You will win the contents of every bank that does not bomb.");
        output.add("Good luck! Start picking spaces to build your first bank when you are ready.");

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
        if(stage < MAX_STAGES)
        {
            if(scores[stage] < 1_000_000)
            	display.append(String.format("  Current Bank: $%,7d%n", scores[stage]));
            else
            	display.append(String.format(" Current Bank: $%,9d%n", scores[stage]));
        }
        if(getPrize() > 0)
        {
            if(getPrize() < 1_000_000)
            	display.append(String.format("  Secured Bank: $%,7d%n", getPrize()));
            else
            	display.append(String.format(" Secured Bank: $%,9d%n", getPrize()));
        }
        display.append(String.format("       %d Banks Left%n", MAX_STAGES-stage));
        display.append("\n   MONEY       MULTIPLIERS\n");
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
            	if(i < BOARD_SIZE)
            	{
            		int thisCash = cashAmounts.get(i);
            		if(thisCash == 0)
            			display.append("XX");
            		else
            		{
            			display.append(thisCash / 1_000); //this'll do for now I'm not great at fancy reveal boards
            		}
            	}
            	else
            	{
                    /* At present the reveal only supports multipliers that
    				 * are multiples of 0.5 up to 10x, above which point only
    				 * integer multipliers are supported.
    				 */
                    double thisMultiplier = multipliers.get(i%BOARD_SIZE);
                    if (thisMultiplier == 0.0)
                        display.append("XX");
                    else 
                    {
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
            	}
                display.append(" ");
            } else {
                display.append(String.format("%02d ",(i+1)));
            }
        }
        display.append(String.format("%n  %d Bombs        %d Bombs",numBombsLeft[0],numBombsLeft[1]));
        display.append("\n```");
		return display.toString();
    }

    @Override
    void playNextTurn(String pick) {
        LinkedList<String> output = new LinkedList<>();

        if (pick.equalsIgnoreCase("STOP"))
        {
        	if(scores[stage] > 0 && (enhanced || scores[stage] >= getMaxBank()))
        	{
                output.add("Very well, we'll secure this bank.");
                stage++;
                endValidTurn(output);
        	}
        	else if(scores[stage] == 0)
        		output.add("You can't secure a bank without any money in it!");
        	else
        		output.add("You've already secured a larger bank than this!");
        }
        else if (isNumber(pick))
        {
            if (Integer.parseInt(pick) < 1 || Integer.parseInt(pick) > pickedSpaces.length)
            {
                output.add("Invalid selection.");
            }
            else if (pickedSpaces[Integer.parseInt(pick) - 1])
            {
                output.add("That space has already been selected.");
            }
            else if (scores[stage] == 0 && Integer.parseInt(pick) > BOARD_SIZE)
            {
            	output.add("You can't pick from the multiplier board until you have some cash to multiply.");
            }
            else
            {
                output.add("Space " + pick + " selected...");
                int selection = Integer.parseInt(pick) - 1;
                pickedSpaces[selection] = true;
                if(selection < BOARD_SIZE)
                {
                	int selectedCash = cashAmounts.get(selection);
                	numSpacesLeft[0] --;
                	if(selectedCash > 0)
                	{
                		scores[stage] += selectedCash;
                		output.add(String.format("**$%,d**!", selectedCash));
                        output.add(String.format("That brings your bank to $%,d!", scores[stage]));
                	}
                	else
                	{
                        output.add("It's a **BOMB**. Unfortunately, you've lost this bank.");
                        numBombsLeft[0] --;
                        scores[stage] = 0;
                        stage++;
                	}
                }
                else
                {
                    double selectedMultiplier = multipliers.get(selection % BOARD_SIZE);
                    numSpacesLeft[1] --;
                    scores[stage] *= selectedMultiplier;
                    if (selectedMultiplier > 0.0) {
                        output.add("**" + selectedMultiplier + "x**!");
                        output.add(String.format("That brings your bank to $%,d!", scores[stage]));
                    }
                    else
                    {
                        output.add("It's a **BOMB**. Unfortunately, you've lost this bank.");
                        numBombsLeft[1] --;
                        stage++;
                    }
                }
                
                endValidTurn(output);
            }
        }
		endTurn(output);
    }

    private void endValidTurn(LinkedList<String> output) {
        if (stage >= MAX_STAGES)
            isAlive = false;
        if (isAlive && scores[stage] > 0) {
            output.add("Pick another space, or type STOP to secure this bank and move to the next.");
        }
        
        output.add(generateBoard(!isAlive));
        // The regular endTurn() method is always called thereafter
    }

	private void endTurn(LinkedList<String> output)
	{
		sendMessages(output);
		if(!isAlive)
            awardMoneyWon(getPrize());
		else
			getInput();
	}
	
	private int getMaxBank()
	{
		int maxBank = 0;
		for(int i=0; i<stage; i++)
			if(scores[i] > maxBank)
				maxBank = scores[i];
		return maxBank;
	}
	
	private int getPrize()
	{
		if(enhanced)
		{
			int bankSum = 0;
			for(int i=0; i<stage; i++)
				bankSum += scores[i];
			return bankSum;
		}
		else
			return getMaxBank();
	}

    @Override
    String getBotPick()
    {
    	//Check for picking a cash value
    	//(We don't use rng here so we can be stateless and avoid having the bot go 'back' to cash after multipliers
        if (scores[stage] < Math.max(getMaxBank()/10, applyBaseMultiplier(50_000)) && numSpacesLeft[0] > numBombsLeft[0])
        {
        	ArrayList<Integer> openSpaces = new ArrayList<>(BOARD_SIZE);
    		for(int i=0; i<BOARD_SIZE; i++)
    			if(!pickedSpaces[i])
    				openSpaces.add(i+1);
    		return String.valueOf(openSpaces.get((int)(RtaBMath.random()*openSpaces.size())));
        }
        //Check for picking a multiplier
        else
        {
        	ArrayList<Integer> openSpaces = new ArrayList<>(BOARD_SIZE);
    		for(int i=BOARD_SIZE; i<2*BOARD_SIZE; i++)
    			if(!pickedSpaces[i])
    				openSpaces.add(i+1);
        	//Trial run if this is our biggest bank, or autostop if there are no multipliers left
        	if((numSpacesLeft[1] <= numBombsLeft[1]) || (scores[stage] >= getMaxBank()
        			&& multipliers.get((int)((RtaBMath.random()*BOARD_SIZE)-1) - BOARD_SIZE) <= 0.0))
    			return "STOP";
        	else
        		return String.valueOf(openSpaces.get((int)(RtaBMath.random()*openSpaces.size())));
        }
    }

    @Override
    void abortGame()
    {
        //Auto-stop, as it is a push-your-luck style game.
    	stage = MAX_STAGES;
        awardMoneyWon(getPrize());
    }

	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	@Override public String getEnhanceText() { return "You will win the sum of all your secured banks."; }
}
