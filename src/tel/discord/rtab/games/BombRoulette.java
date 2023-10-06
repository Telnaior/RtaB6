package tel.discord.rtab.games;

import java.util.LinkedList;
import java.util.Random;

import net.dv8tion.jda.api.entities.Message;
import tel.discord.rtab.Achievement;

import static tel.discord.rtab.RaceToABillionBot.rng;

public class BombRoulette extends MiniGameWrapper {
    static final String NAME = "Bomb Roulette";
    static final String SHORT_NAME = "Roulette";
    static final boolean BONUS = false;
    boolean isAlive; 
    int score;
    boolean hasJoker;
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
        spaceValues = new int[] {50_000, 1_000_000, 10_000, 0, 100_000, 350_000, 200_000, 0,
        						100_000, 500_000, 50_000, 0, 10_000, 750_000, 100_000, 0,
        						200_000, 350_000, 50_000, 0, 10_000, 500_000, 200_000, 0};
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
            spaceTypes[7] = WheelSpace.TRIPLE;

        for (int i = 0; i < spaceTypes.length; i++)
        {
	        switch (spaceTypes[i]) {
		        case CASH -> {
			        spaceValues[i] = applyBaseMultiplier(spaceValues[i]);
			        if (spaceValues[i] < bottomDollar)
				        bottomDollar = spaceValues[i];
			        if (spaceValues[i] > topDollar)
				        topDollar = spaceValues[i];
			        cashLeft += spaceValues[i];
			        cashSpaces++;
		        }
		        case DOUBLE -> doubleSpaces++;
		        case TRIPLE -> tripleSpaces++;
		        case HALVE -> halveSpaces++;
		        case JOKER -> jokerSpaces++;
		        case BANKRUPT -> bankruptSpaces++;
		        case BOMB -> bombSpaces++;
	        }
        }
                
        //Display instructions
        output.add("In Bomb Roulette, you will be spinning a 24-space wheel, "
                + "trying to collect as much cash as possible.");
        output.add("Eighteen of those spaces have various amounts of cash, "
                + String.format("ranging from $%,d to $%,d. The total amount on the "
                		,bottomDollar,topDollar)
                + String.format("wheel at the beginning of the game is $%,d.",cashLeft));
        output.add("Three are **Double** spaces, which will double your score up " 
                + "to that point.");
        if(enhanced)
            output.add("ENHANCE BONUS: One of the halve spaces has been upgraded to a **Triple** space, which will triple your score up to that point.");
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
	            	Achievement.ROULETTE_JACKPOT.check(getPlayer());
	            sendMessage("You would have spun...");
	        	int proveout = spinWheel(true);
	        	switch(spaceTypes[proveout]) {
		        	case CASH -> sendMessage(String.format("$%,d.", spaceValues[proveout]));
		        	case DOUBLE -> sendMessage("A Double.");
		        	case TRIPLE -> sendMessage("A Triple.");
		        	case JOKER -> sendMessage("A Joker.");
		        	case HALVE -> sendMessage("A HALVE!");
		        	case BANKRUPT -> sendMessage("A BANKRUPT!");
		        	case BOMB -> sendMessage("A BOMB!");
	        	}
            }
            else
            	output.add("You don't have anything to lose yet, give the wheel a **SPIN**!");
        }
                
        else if (pick.equalsIgnoreCase("SPIN") || pick.equalsIgnoreCase("QUICKSPIN") || pick.equalsIgnoreCase("QS"))
        {
            sendMessage("Spinning wheel...");
            boolean quickspin = pick.equalsIgnoreCase("QUICKSPIN") || pick.equalsIgnoreCase("QS");
            pointer = spinWheel(quickspin);

	        switch (spaceTypes[pointer]) {
		        case CASH -> {
			        output.add(String.format("**$%,d**!", spaceValues[pointer]));
			        score += spaceValues[pointer];
		        }
		        case DOUBLE -> {
		        	if(score == 0)
		        	{
		        		output.add("It's a **Double**, but you don't have any money... so we'll give you some.");
		        		score += applyBaseMultiplier(100_000);
		        	}
		        	else
		        	{
				        output.add("It's a **Double**!");
				        score *= 2;
		        	}
		        }
		        case TRIPLE -> {
		        	if(score == 0)
		        	{
		        		output.add("It's a **Triple**, but you don't have any money... so we'll give you a lot!");
		        		score += applyBaseMultiplier(1_000_000);
		        	}
		        	else
		        	{
				        output.add("It's a **Triple**!");
				        score *= 3;
		        	}
		        }
		        case JOKER -> {
			        output.add("It's the **Joker**!");
			        hasJoker = true;
		        }
		        case HALVE -> {
		        	if(score == 0)
		        	{
		        		output.add("It's a **HALVE**, but you don't have any money... so we'll take some away :)");
		        		score -= applyBaseMultiplier(100_000);
		        	}
		        	else
		        	{
				        output.add("It's a **HALVE**.");
				        score /= 2;
		        	}
		        }
		        case BANKRUPT -> {
			        output.add("Oh no, you've gone **BANKRUPT**!");
			        score = 0;
			        if (cashSpaces == 0) {
				        output.add("...which normally wouldn't eliminate you, "
						        + "but unfortunately, it's mathematically "
						        + "impossible to make any of your money back. "
						        + "Better luck next time.");
				        isAlive = false;
			        }
		        }
		        case BOMB -> {
			        if (hasJoker) {
				        output.add("It's a **BOMB**, but since you have a joker, we'll take that "
						        + "instead of your money!");
				        hasJoker = false;
			        } else {
				        output.add("It's a **BOMB**.");
				        score = 0;
				        isAlive = false;
			        }
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
                display.append("  Total: $").append(String.format("%,9d", score));
                if (hasJoker)
                    display.append(" + Joker");
                display.append("\n\n");
                if (cashSpaces > 0)
                    display.append(String.format("%,2dx Cash", cashSpaces)).append(String.format(" ($%,9d Remaining)%n", cashLeft));
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
    
    private int spinWheel(boolean quickspin)
    {
    	//Aw yeah! This is happenin'! (Only took several seasons and a complete code rewrite)
    	int index = rng.nextInt(spaceTypes.length);
    	if(sendMessages && !quickspin)
    	{
    		Message wheelMessage = channel.sendMessage(displayRoulette(index)).complete();
    		//Start with a 0.5-second delay
    		int delay = 500 + rng.nextInt(500);
    		try { Thread.sleep(delay); } catch (InterruptedException e) { e.printStackTrace(); }
    		do
    		{
    			//Move along one space on the wheel
    			index ++;
    			index %= spaceTypes.length;
    			//Update the roulette display
    			wheelMessage.editMessage(displayRoulette(index)).queue();
    			//Then increase the delay randomly, and wait for that amount of time
    			delay += rng.nextInt(500);
    			try { Thread.sleep(delay); } catch (InterruptedException e) { e.printStackTrace(); }
    		}
    		//Stop once we reach a 2.5-second delay
    		while(delay < 2500);
    	}
    	else
    	{
    		//Just simulate the spin quickly and quietly
    		int delay = 500 + rng.nextInt(500);
    		do
    		{
    			index ++;
    			index %= spaceTypes.length;
    			delay += rng.nextInt(500);
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
			if (spaceTypes[wheelPosition] == WheelSpace.CASH) {
				board.append(String.format("$%,d", spaceValues[wheelPosition]));
			} else {
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
	    switch (spaceTypes[space]) {
		    case CASH -> {
			    cashSpaces--;
			    cashLeft -= spaceValues[space];
		    }
		    case DOUBLE -> doubleSpaces--;
		    case TRIPLE -> tripleSpaces--;
		    case HALVE -> halveSpaces--;
		    case BANKRUPT -> bankruptSpaces--;
		    case JOKER -> {
			    jokerSpaces--;
			    hasJoker = true;
		    }
		    case BOMB -> bombSpaces--; // it'll go back up, but deleting this line creates a bug
	    }
        
        spaceTypes[space] = WheelSpace.BOMB;
        bombSpaces++;
        
        for (int i = 0; i < spaceTypes.length; i++) {
	        switch (spaceTypes[i]) {
		        case DOUBLE -> spaceValues[i] = score;
		        case TRIPLE -> spaceValues[i] = score * 2;
		        case HALVE -> spaceValues[i] = score * -1 / 2;
		        case BANKRUPT -> spaceValues[i] = score * -1;
		        case BOMB -> {
			        if (hasJoker)
				        spaceValues[i] = 0;
			        else spaceValues[i] = score * -1;
		        }
		        default -> {
		        } // do nothing
	        }
        }
    }
        
    private int getExpectedValue()
    {
        int sum = 0;
        for (int spaceValue : spaceValues) sum += spaceValue;
        return sum/spaceValues.length;
    }

    @Override
    String getBotPick() {
        if (score == 0 || getExpectedValue() > 0)
            return "QUICKSPIN";
            
        int testSpin = rng.nextInt(24);
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
        return "One Halve space is upgraded to a Triple space.";
    }
}
