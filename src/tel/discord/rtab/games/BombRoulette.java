package tel.discord.rtab.games;

import java.util.LinkedList;
import java.util.Random;

import net.dv8tion.jda.api.entities.Message;
import tel.discord.rtab.Achievement;

public class BombRoulette extends MiniGameWrapper {
    static final String NAME = "Bomb Roulette";
    static final String SHORT_NAME = "Roulette";
    static final boolean BONUS = false;
    boolean isAlive; 
    int score;
    boolean hasJoker;
    boolean quickspin = false;
    enum WheelSpace {CASH, DOUBLE, TRIPLE, HALVE, JOKER, BANKRUPT, BOMB}

	int bottomDollar; // only needed for intro
	int topDollar; // only needed for intro
    int[] spaceValues;
    WheelSpace[] spaceTypes;
    int pointer;
    int cashLeft;
    int cashSpaces;
    int doubleSpaces;
    int tripleSpaces;
    int halveSpaces;
    int jokerSpaces;
    int bankruptSpaces;
    int bombSpaces;
    Random r = new Random();
        
    void startGame()
    {
        LinkedList<String> output = new LinkedList<>();
        //Initialise wheel
        isAlive = true;
        score = 0;
        bottomDollar = Integer.MAX_VALUE;
        topDollar = Integer.MIN_VALUE;
        hasJoker = false;
        /* It might not initially make sense to assign non-cash space a cash 
         * value; but the bot uses this information to determine its strategy.
         */
        spaceValues = new int[] {90000, 25000, 40000, 0, 45000, 70000,
                100000, 0, 30000, 200000, 35000, 0, 150000, 25000, 50000,
                0, 75000, 250000, 80000, 0, 50000, 60000, 125000, 0};
        spaceTypes = new WheelSpace[] {WheelSpace.CASH, WheelSpace.CASH,
                WheelSpace.CASH, WheelSpace.DOUBLE, WheelSpace.CASH,
                WheelSpace.CASH, WheelSpace.CASH, WheelSpace.HALVE,
                WheelSpace.CASH, WheelSpace.CASH, WheelSpace.CASH,
                WheelSpace.DOUBLE, WheelSpace.CASH, WheelSpace.CASH,
                WheelSpace.CASH, WheelSpace.HALVE, WheelSpace.CASH,
                WheelSpace.CASH, WheelSpace.CASH, WheelSpace.DOUBLE,
                WheelSpace.CASH, WheelSpace.CASH, WheelSpace.CASH,
                WheelSpace.JOKER};
        if(enhanced)
            spaceTypes[11] = WheelSpace.TRIPLE;

        for (int i = 0; i < spaceTypes.length; i++)
        {
            switch (spaceTypes[i])
            {
            case CASH:
            	spaceValues[i] = applyBaseMultiplier(spaceValues[i]);
				if (spaceValues[i] < bottomDollar)
					bottomDollar = spaceValues[i];
				if (spaceValues[i] > topDollar)
					topDollar = spaceValues[i];
	            cashLeft += spaceValues[i];
	            cashSpaces++;
	            break;
            case DOUBLE:
                doubleSpaces++;
                break;
            case TRIPLE:
                tripleSpaces++;
                break;
            case HALVE:
                halveSpaces++;
                break;
            case JOKER:
                jokerSpaces++;
                break;
            case BANKRUPT:
                bankruptSpaces++;
                break;
            case BOMB:
                bombSpaces++;
                break;
            }
        }
                
        //Display instructions
        output.add("In Bomb Roulette, you will be spinning a 24-space wheel "
                + "trying to collect as much cash as possible.");
        output.add("Eighteen of those spaces have various amounts of cash "
                + String.format("ranging from $%,d to $%,d. The total amount on the "
                		,bottomDollar,topDollar)
                + String.format("wheel at the beginning of the game is $%,d.",cashLeft));
        output.add("Three are **Double** spaces, which will double your score up " 
                + "to that point.");
        if(enhanced)
            output.add("ENHANCE BONUS: One of the double spaces has been upgraded to a **Triple** space, which will triple your score up to that point.");
        output.add("Two are **Halve** spaces, which will halve your score up "
                + "to that point.");
        output.add("One is a **Joker** space, which will save you in the event "
                + "that you hit...");
        output.add("...a **BOMB** space, which costs you all your winnings and "
                + "the game. There aren't any on the wheel right now, but each "
                + "space you hit will be replaced with a BOMB space.");
        output.add("After you've spun the wheel at least once, you are free to "
                + "walk away with your winnings if you wish.");
        output.add("Good luck! Type SPIN (or QUICKSPIN) when you're ready.");
        sendSkippableMessages(output);
        sendMessage(generateBoard());
        getInput();
    }
    
    void playNextTurn(String pick)
    {
        LinkedList<String> output = new LinkedList<>();
                
        if (pick.equalsIgnoreCase("STOP")) {
            // Prevent accidentally stopping with nothing if the player hasn't spun yet
            if (bombSpaces != 0)
            {
	            isAlive = false;
	            if(doubleSpaces == 0 && tripleSpaces == 0 && score >= cashLeft)
	            	Achievement.ROULETTE_JACKPOT.check(getCurrentPlayer());
            }
            else
            	output.add("You don't have anything to lose yet, give the wheel a **SPIN**!");
        }
                
        else if (pick.equalsIgnoreCase("SPIN") || pick.equalsIgnoreCase("QUICKSPIN"))
        {
            sendMessage("Spinning wheel...");
            quickspin = pick.equalsIgnoreCase("QUICKSPIN");
            pointer = spinWheel();
            
            switch (spaceTypes[pointer])
            {
                case CASH:
                    output.add(String.format("**$%,d**!", spaceValues[pointer]));
                    score += spaceValues[pointer];
                    break;
                case DOUBLE:
                    output.add("It's a **Double**!");
                    score *= 2;
                    break;
                case TRIPLE:
                    output.add("It's a **Triple**!");
                    score *= 3;
                    break;
                case JOKER:
                    output.add("It's the **Joker**!");
                    hasJoker = true;
                    break;
                case HALVE:
                    output.add("It's a **HALVE**.");
                    score /= 2;
                    break;
                case BANKRUPT:
                    output.add("Oh no, you've gone **BANKRUPT**!");
                    score = 0;
                    
                    if (cashSpaces == 0)
                    {
                        output.add("...which normally wouldn't eliminate you, "
                                + "but unfortunately, it's mathematically "
                                + "impossible to make any of your money back. "
                                + "Better luck next time.");
                        isAlive = false;
                    }
                    break;
                case BOMB:
                    if (hasJoker)
                    {
                        output.add("It's a **BOMB**, but since you have a joker, we'll take that "
                                + "instead of your money!");
                        hasJoker = false;
                    }
                    else
                    {
                        output.add("It's a **BOMB**.");
                        score = 0;
                        isAlive = false;
                    }
            }
                    
            if (isAlive) {
                bombSpace(pointer);
                        
                if (cashSpaces == 0 && doubleSpaces == 0 && tripleSpaces == 0) {
                    output.add("You've earned everything you can!");
                    isAlive = false;
                }
                else {
                    output.add(generateBoard());
                    output.add("SPIN again (or QUICKSPIN) if you dare, or type STOP to stop with your current total.");
                }
            }
        }
		sendMessages(output);
		if(!isAlive)
			awardMoneyWon(score);
		else
			getInput();
    }
        
    private String generateBoard()
    {
        StringBuilder display = new StringBuilder();
        display.append("```\n");
        display.append("  B O M B   R O U L E T T E\n");
                display.append("  Total: $" + String.format("%,9d", score));
                if (hasJoker)
                    display.append(" + Joker");
                display.append("\n\n");
                if (cashSpaces > 0)
                    display.append(String.format("%,2dx Cash", cashSpaces) +
                            String.format(" ($%,9d Remaining)%n", cashLeft));
                if (doubleSpaces > 0)
                    display.append(String.format("%,2dx Double%n",
                            doubleSpaces));
                if (tripleSpaces > 0)
                    display.append(String.format("%,2dx Triple%n",
                            tripleSpaces));
                if (halveSpaces > 0)
                    display.append(String.format("%,2dx Halve%n", halveSpaces));
                if (jokerSpaces > 0)
                    display.append(String.format("%,2dx Joker%n", jokerSpaces));
                if (bankruptSpaces > 0)
                    display.append(String.format("%,2dx Bankrupt%n",
                            bankruptSpaces));
				if (bombSpaces > 0)
					display.append(String.format("%,2dx Bomb%n", bombSpaces));
        display.append("```");
        return display.toString();
    }
    
    private int spinWheel()
    {
    	//Aw yeah! This is happenin'! (Only took several seasons and a complete code rewrite)
    	int index = r.nextInt(spaceTypes.length);
    	if(sendMessages && !quickspin)
    	{
    		Message wheelMessage = channel.sendMessage(displayRoulette(index)).complete();
    		//Start with a 0.5-second delay
    		int delay = 500 + r.nextInt(500);
    		try { Thread.sleep(delay); } catch (InterruptedException e) { e.printStackTrace(); }
    		do
    		{
    			//Move along one space on the wheel
    			index ++;
    			index %= spaceTypes.length;
    			//Update the roulette display
    			wheelMessage.editMessage(displayRoulette(index)).queue();
    			//Then increase the delay randomly, and wait for that amount of time
    			delay += r.nextInt(500);
    			try { Thread.sleep(delay); } catch (InterruptedException e) { e.printStackTrace(); }
    		}
    		//Stop once we reach a 2.5-second delay
    		while(delay < 2500);
    	}
    	else
    	{
    		//Just simulate the spin quickly and quietly
    		int delay = 500 + r.nextInt(500);
    		do
    		{
    			index ++;
    			index %= spaceTypes.length;
    			delay += r.nextInt(500);
    		}
    		while(delay < 2500);
    	}
		return index;
    }
    
    private String displayRoulette(int index)
    {
		StringBuilder board = new StringBuilder().append("```\n");
		//Iterate through each space on the board
		for(int i=-2; i<=2; i++)
		{
			if(i == 0)
				board.append("> ");
			else
				board.append("  ");
			int wheelPosition = (index+i) % spaceTypes.length;
			if(wheelPosition < 0)
				wheelPosition += spaceTypes.length;
			switch(spaceTypes[wheelPosition])
			{
			case CASH:
				board.append(String.format("$%,d", spaceValues[wheelPosition]));
				break;
			default:
				board.append(spaceTypes[wheelPosition]);
			}
			board.append("\n");
		}
		board.append("```");
		return board.toString();
    }
    
    private void bombSpace(int space)
    {
        // check what kind space it originally was, then update the stats to
        // match
        switch(spaceTypes[space]) {
            case CASH:
                cashSpaces--;
                cashLeft -= spaceValues[space];
                break;
            case DOUBLE:
                doubleSpaces--;
                break;
            case TRIPLE:
                tripleSpaces--;
                break;
            case HALVE:
                halveSpaces--;
                break;
            case BANKRUPT:
                bankruptSpaces--;
                break;
            case JOKER:
                jokerSpaces--;
                hasJoker = true;
                break;
            case BOMB:
                bombSpaces--; // it'll go back up, but deleting this line creates a bug
                break;
        }
        
        spaceTypes[space] = WheelSpace.BOMB;
        bombSpaces++;
        
        for (int i = 0; i < spaceTypes.length; i++) {
            switch (spaceTypes[i]) {
                case DOUBLE:
                    spaceValues[i] = score;
                    break;
                case TRIPLE:
                    spaceValues[i] = score * 2;
                    break;
                case HALVE:
                    spaceValues[i] = score * -1 / 2;
                    break;
                case BANKRUPT:
                    spaceValues[i] = score * -1;
                    break;
                case BOMB:
                    if (hasJoker)
                        spaceValues[i] = 0;
                    else spaceValues[i] = score * -1;
                    break;
                default: // do nothing
                    break;
            }
        }
    }
        
    private int getExpectedValue()
    {
        int sum = 0;
        for (int i = 0; i < spaceValues.length; i++)
            sum += spaceValues[i];
        return sum/spaceValues.length;
    }

    @Override
    String getBotPick() {
        if (score == 0 || getExpectedValue() > 0)
            return "QUICKSPIN";
            
        int testSpin = r.nextInt(24);
        if (spaceTypes[testSpin] == WheelSpace.BANKRUPT || (!hasJoker &&
                spaceTypes[testSpin] == WheelSpace.BOMB))
            return "STOP";
        return "QUICKSPIN";
    }

	@Override
	void abortGame()
	{
		//Auto-stop, as it is a push-your-luck style game.
		awardMoneyWon(score);
	}

	@Override
	public String getName()
	{
		return NAME;
	}
	@Override
	public String getShortName()
	{
		return SHORT_NAME;
	}
	@Override
	public boolean isBonus()
	{
		return BONUS;
	}
	@Override public String getEnhanceText() {
        return "One Double space is upgraded to a Triple space.";
    }
}
