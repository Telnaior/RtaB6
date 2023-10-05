package tel.discord.rtab.games;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

import tel.discord.rtab.Achievement;

public class BoosterSmash extends MiniGameWrapper {
	static final String NAME = "Booster Smash";
	static final String SHORT_NAME = "Smash";
	static final boolean BONUS = false;
	static final int BOARD_SIZE = 15;
	static final int[] VALUES = {2,5,5,3};
	static final int[] TOKENS_NEEDED = {0,3,6,9,12,16,20,24};
	static final int[] PAYTABLE = {0,10,20,40,75,125,250,500};
	int tokensPicked, bombsPicked;
	int startingBoost;
	ArrayList<Integer> board = new ArrayList<>(BOARD_SIZE);
	int lastSpace;
	int lastPicked;
	boolean[] pickedSpaces = new boolean[BOARD_SIZE];
	boolean bailChance;
	boolean quit;
	
	@Override
	void startGame()
	{
		LinkedList<String> output = new LinkedList<>();
		//Initialise board
		board.clear();
		for(int i=0; i<VALUES.length; i++)
			for(int j=0; j<VALUES[i]; j++)
				board.add(i);
		Collections.shuffle(board);
		pickedSpaces = new boolean[BOARD_SIZE];
		//Prep other variables
		startingBoost = getPlayer().booster;
		quit = false;
		lastSpace = -1;
		lastPicked = 0;
		//Display instructions
		output.add("In Booster Smash, you are not playing for money.");
		output.add("Instead, you are playing for tokens which can be exchanged for boost!");
		output.add("Most spaces on the fifteen-space board contain 1-3 tokens, but there are also two bombs.");
		if(startingBoost <= 100)
			output.add("If you find both bombs, you will win nothing.");
		else
			output.add("If you find both bombs, not only will you win nothing but *your current booster will be cut in half as well*.");
		output.add("Here is the exchange rate from tokens to boost:");
		output.add(generatePaytable());
		output.add("Oh, and one more thing - you can only choose to bail when you reach a new milestone on the ladder.");
		output.add("With that in mind, will you STOP now, or play Booster Smash for your first 3 tokens?");
		bailChance = true;
		sendSkippableMessages(output);
		sendMessage(generateBoard(false));
		getInput();
	}
	
	@Override
	public void playNextTurn(String pick)
	{
		LinkedList<String> output = new LinkedList<>();
		if(pick.equalsIgnoreCase("STOP"))
		{
			//Check if they can quit or not
			if(bailChance)
			{
				output.add(generateBoard(true));
				quit = true;
			}
			else
			{
				output.add("You cannot bail now, you need to play on to the next milestone!");
			}
		}
		else if(!isNumber(pick))
		{
			//Random unrelated non-number doesn't need feedback
			//Do nothing and let the return at the bottom catch it
		}
		else if(!checkValidNumber(pick))
		{
			output.add("Invalid pick.");
		}
		else
		{
			//If they just rejected an offer to quit, comment on it and rescind the opportunity
			if(bailChance)
			{
				if(tokensPicked > 0)
					output.add("We play on!");
				else
					output.add("Let's play!");
				bailChance = false;
			}
			lastSpace = Integer.parseInt(pick)-1;
			pickedSpaces[lastSpace] = true;
			lastPicked = board.get(lastSpace);
			output.add(String.format("Space %d selected...",lastSpace+1));
			if(bombsPicked > 0)
				output.add("...");
			//If we find tokens, add them to the bank and check for bail point
			if(lastPicked > 0)
			{
				output.add(String.format("It's **%d token"+(lastPicked>1?"s":"")+"!**",lastPicked));
				//If we now have All Of The Tokens, celebrate and quit
				if(tokensPicked+lastPicked >= TOKENS_NEEDED[TOKENS_NEEDED.length-1])
				{
					output.add("Congratulations, you have reached the ultimate goal!");
					quit = true;
					output.add(generateBoard(true));
				}
				//Otherwise, if we just crossed a lesser milestone, allow an opportunity to bail
				else if(convertTokensToBoost(tokensPicked) != convertTokensToBoost(tokensPicked+lastPicked))
				{
					output.add("Congratulations, you have reached the next milestone!");
					output.add(String.format("You can STOP now and take a **+%d%%** booster,",
							convertTokensToBoost(tokensPicked+lastPicked))
							+" or you can play on to earn more tokens.");
					output.add(generatePaytable());
					bailChance = true;
				}
				tokensPicked += lastPicked;
				output.add(generateBoard(false));
			}
			//Otherwise it's kaboom time
			else
			{
				output.add("It's a **BOMB**.");
				bombsPicked ++;
				if(bombsPicked == 1)
				{
					output.add("One more and you'll be sorry...");
					output.add(generateBoard(false));
				}
				else
				{
					output.add("You're outta here!");
					output.add(generateBoard(true));
				}
			}
		}
		sendMessages(output);
		if(quit)
			awardBoost(convertTokensToBoost(tokensPicked));
		else if(bombsPicked > 1)
			lostTheGame();
		else
			getInput();
	}
	
	boolean checkValidNumber(String message)
	{
		int location = Integer.parseInt(message)-1;
		return (location >= 0 && location < BOARD_SIZE && !pickedSpaces[location]);
	}

	String generateBoard(boolean reveal)
	{
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append("BOOSTER  SMASH\n");
		for(int i=0; i<BOARD_SIZE; i++)
		{
			if(pickedSpaces[i])
			{
				display.append("  ");
			}
			else
			{
				if(reveal)
				{
					if(board.get(i) > 0)
						display.append(String.format("%dT", board.get(i)));
					else
						display.append("XX");
				}
				//Otherwise show the number
				else
					display.append(String.format("%02d",(i+1)));
			}
			if(i%5 == 4)
				display.append("\n");
			else
				display.append(" ");
		}
		display.append("\n");
		//Next display our token and bomb counts, if the game isn't over
		if(!reveal)
		{
			display.append(String.format("You have %d token", tokensPicked)).append(tokensPicked != 1 ? "s" : "").append(bombsPicked == 1 ? " and one bomb.\n" : ".\n");
			display.append(String.format("Current value: +%3d%%%n",convertTokensToBoost(tokensPicked)));
		}
		display.append("```");
		return display.toString();
	}
	
	String generatePaytable()
	{
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append("BOOSTER SMASH PRIZES\n");
		display.append(String.format(" YOU HAVE %02d TOKENS %n",tokensPicked+lastPicked));
		//Show a line for each milestone
		for(int i=1; i<TOKENS_NEEDED.length; i++)
		{
			if(TOKENS_NEEDED[i] > tokensPicked+lastPicked)
				display.append(String.format("  %2d tokens: +%3d%%  %n",
						TOKENS_NEEDED[i],convertTokensToBoost(TOKENS_NEEDED[i])));	
		}
		//Show a line for bombing out if there's anything at risk
		if(startingBoost > 100)
		{
			display.append(String.format("\n   2 BOMBs:  -%3d%%  \n", 
					(getPlayer().booster + (getPlayer().booster%2) - 100) / 2));
		}
		display.append("```");
		return display.toString();
	}
	
	int convertTokensToBoost(int tokens)
	{
		//Search through the paytable until we get beyond where we are, then return the last amount we had enough for
		int boostEarned = -1;
		for(int i=0; i<TOKENS_NEEDED.length; i++)
		{
			if(tokens >= TOKENS_NEEDED[i])
				boostEarned = PAYTABLE[i] * gameMultiplier;
			else
				break;
		}
		return boostEarned;
	}
	
	@Override
	public String getBotPick()
	{
		//Bail if they've hit a bomb
		if(bailChance && bombsPicked > 0)
			return "STOP";
		ArrayList<Integer> openSpaces = new ArrayList<>(BOARD_SIZE);
		SecureRandom r = new SecureRandom();
		for(int i=0; i<BOARD_SIZE; i++)
			if(!pickedSpaces[i])
				openSpaces.add(i+1);
		return String.valueOf(openSpaces.get(r.nextInt(openSpaces.size())));
	}

	@Override
	void abortGame()
	{
		//If they timed out when they had the chance to stop, treat it as a stop
		if(bailChance)
			awardBoost(convertTokensToBoost(tokensPicked));
		//Otherwise, we have to hurt them
		else
			lostTheGame();
	}
	
	private void awardBoost(int boostWon)
	{
		StringBuilder resultString = new StringBuilder();
		if(getPlayer().isBot)
			resultString.append(getPlayer().getName()).append(" won ");
		else
			resultString.append("Game Over. You won ");
		resultString.append(String.format("**+%d%%** from ",boostWon));
		if(gameMultiplier > 1)
			resultString.append(String.format("%d copies of ",gameMultiplier));
		resultString.append(getName()).append(".");
		if(getPlayer().booster < 999 && getPlayer().booster + boostWon >= 999)
			Achievement.BOOSTER_JACKPOT.check(getPlayer());
		getPlayer().addBooster(boostWon);
		sendMessages = true;
		sendMessage(resultString.toString());
		gameOver();
	}
	
	private void lostTheGame()
	{
		if(startingBoost > 100) //Do they have boost to lose?
		{
			if(getPlayer().isBot) //Third-person for bot
			{
				sendMessage(getPlayer().getName() + " lost half their boost in Booster Smash...");
			}
			else //Second-person for human
			{
				sendMessage("You lost half your boost in Booster Smash...");
			}
			//Then dock their boost and end the game
			getPlayer().booster = (startingBoost+100)/2;
			gameOver();
		}
		else
			awardBoost(0);
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
}
