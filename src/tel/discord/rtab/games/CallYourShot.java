package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import tel.discord.rtab.Achievement;
import tel.discord.rtab.games.objs.Jackpots;

public class CallYourShot extends MiniGameWrapper
{
	static final String NAME = "Call Your Shot";
	static final String SHORT_NAME = "Call";
	static final boolean BONUS = false; 
	
	int stageAmount;
	int roundNumber;
	int colorPicked;
	int total;
	
	List<Integer> colorNumber = Arrays.asList(0, 1,1, 2,2,2, 3,3,3,3, 4,4,4,4,4, 5,5,5,5,5,5);
	List<String> colorNames = Arrays.asList("Gold", "Green", "Purple", "Blue", "Orange", "Red");
	List<Integer> values = Arrays.asList(5_000_000, 1_200_000, 750_000, 550_000, 420_000, 327_680); //Gold value is overwritten by jackpot
	boolean alive; //Player still alive?
	boolean stop; //Has the player called it quits?
	boolean[] pickedSpaces;
	int lastSpace;
	int lastPick;
	
	
	/**
	 * Initialises the variables used in the minigame and prints the starting messages.
	 * @return A list of messages to send to the player.
	 */
	@Override
	void startGame()
	{
		//Get gold jackpot
		values.set(0, applyBaseMultiplier(Jackpots.CYS_GOLD.getJackpot(channel)));
		//and multiply other values
		for(int i=1; i<values.size(); i++)
			values.set(i, applyBaseMultiplier(values.get(i)));
		stageAmount = 0;
		roundNumber = -1;
		colorPicked = 9;
		alive = true; 
		stop = false;

		pickedSpaces = new boolean[colorNumber.size()];
		Collections.shuffle(colorNumber);
			
		LinkedList<String> output = new LinkedList<>();

		/*for(int i=0; i<21; i++)
		{
			if (colorNumber.get(i) == 0)
			{

				output.add("Gold is " + (i + 1));
			}
		} // debug */
		//Give instructions
		output.add("Welcome to Call Your Shot!");
		output.add("There are six colors of balls on this 21 space board. "
				+ "Six reds, five oranges, four blues, three purples, two greens, and one gold.");
		output.add("You're going to pick one of the colors, then try to pick a ball of that color.");
		output.add("If you pick the color you chose on your first try, you win that color's initial value!");
		output.add("If you didn't pick your color, the value is cut in half, and you can pick again with the chosen ball removed.");
		output.add("With two exceptions, you can make mistakes equal to the number of balls of the color you picked.");
		output.add("If you picked red, you have as many chances as you need to pick a red.");
		output.add("If you picked gold you only get a single chance, but if you strike it lucky on that one chance "
				+ String.format("you will win a jackpot which currently stands at **$%,d**!",values.get(0)));
		output.add("Of course, if you run out of chances, the game is over and you don't win anything.");
		output.add("The other initial values: "
				+ String.format("$%,d for green, $%,d for purple, $%,d for blue, $%,d for orange, and $%,d for red.",
						values.get(1),values.get(2),values.get(3),values.get(4),values.get(5)));
		if(enhanced)
			output.add("ENHANCE BONUS: The gold ball will always count as a win, no matter what colour you pick.");
		sendSkippableMessages(output);
		sendMessage("With that said, what color will you try to pick?");
		getInput();
	}

	/**
	 * Takes the next player input and uses it to play the next "turn" - up until the next input is required.
	 * @param pick The next input sent by the player.
	 * @return A list of messages to send to the player.
	 */
	@Override
	void playNextTurn(String pick)
	{
		LinkedList<String> output = new LinkedList<>();
		

		String choice = pick.toUpperCase();
		choice = choice.replaceAll("\\s","");
		if (roundNumber == -1 && colorPicked == 9 &&
		(choice.equals("RED") || choice.equals("ORANGE") || choice.equals("BLUE") || choice.equals("PURPLE") || choice.equals("GREEN") || choice.equals("GOLD")))
		{
			roundNumber = 0;
			
			if (choice.equals("RED"))
			{
				output.add("You picked red. "
						+ String.format("You're playing for $%,d to start and you have as many chances as you need. ",values.get(5))
						+ "Good luck!");
				colorPicked = 5;
			}
			else if (choice.equals("ORANGE"))
			{
				output.add("You picked orange. "
						+ String.format("You're playing for $%,d and you can make five mistakes. ",values.get(4))
						+ "Good luck!");
				colorPicked = 4;
			}			
			else if (choice.equals("BLUE"))
			{
				output.add("You picked blue. "
						+ String.format("You're playing for $%,d and you can make four mistakes. ",values.get(3))
						+ "Good luck!");
				colorPicked = 3;
			}			
			else if (choice.equals("PURPLE"))
			{
				output.add("You picked purple. "
						+ String.format("You're playing for $%,d and you can make three mistakes. ",values.get(2))
						+ "Good luck!");
				colorPicked = 2;
			}			
			else if (choice.equals("GREEN"))
			{
				output.add("You picked green. "
						+ String.format("You're playing for $%,d and you can make two mistakes. ",values.get(1))
						+ "Good luck!");
				colorPicked = 1;
			}			
			else if (choice.equals("GOLD"))
			{
				output.add("Ooh, risky~ You picked gold. "
						+ String.format("You only get one chance, but if you strike gold, you win **$%,d**. ",values.get(0))
						+ "Good luck!");
				colorPicked = 0;
			}
			stageAmount = values.get(colorPicked);
			total = stageAmount;
			output.add(generateBoard());
			if(colorPicked != 0)
				Jackpots.CYS_GOLD.addToJackpot(channel, 10_000); //Small increment whenever they don't pick gold
			//You picked a color and didn't pick one before
		}
		else if(!isNumber(choice))
		{
			//Absolutely still don't say anything for random strings
		}
		else if(!checkValidNumber(choice))
		{
			output.add("Invalid pick.");
		}
		else if(colorPicked != 9)
		{
			lastSpace = Integer.parseInt(pick)-1;
			pickedSpaces[lastSpace] = true;
			lastPick = colorNumber.get(lastSpace);
			//Start printing output
			output.add(String.format("Space %d selected...",lastSpace+1));
			output.add("..."); //suspense dots
			if (colorNumber.get(lastSpace).equals(colorPicked) || (enhanced && colorNumber.get(lastSpace) == 0))
			{
				if (colorPicked == 0) //Special message for if they go for gold and get it
				{
					output.add("It's **Gold**! Incredible!!!");
					Achievement.CYS_JACKPOT.check(getCurrentPlayer());
					Jackpots.CYS_GOLD.resetJackpot(channel);
				}
				else //If they get the right color.
				{
					output.add("It's **" + colorNames.get(colorNumber.get(lastSpace)) + "**!");
				}
				output.add("Congratulations, you win!");
				stop = true;
			}
			else
			{
				roundNumber++;
				output.add("It's **" + colorNames.get(colorNumber.get(lastSpace)) + "**.");
				if (colorPicked == 0) //Tried gold and lost. Too bad :(
				{
					output.add("Sorry, your gamble didn't pay off this time.");
					Jackpots.CYS_GOLD.addToJackpot(channel, 50_000); //Large increment when they go for gold and miss
					output.add(generateRevealBoard());
					total = 0;
					alive = false;
				}
				else if (colorPicked == 5 || roundNumber - 2 != colorPicked) //Picked red (and thus has infinite tries) or hasn't lost yet
				{
					total = total / 2;
					output.add(String.format("We cut the bank in half; it is now $%,d. Please try again.",total));
					output.add(generateBoard());
				}
				else //No more tries
				{
					output.add("Sorry, you ran out of mistakes, you lose.");
					output.add(generateRevealBoard());
					total = 0;
					alive = false;
				}
				
			}
		}
		sendMessages(output);
		if(isGameOver())
			awardMoneyWon(total);
		else
			getInput();
	}
	
	private boolean checkValidNumber(String message)
	{
		int location = Integer.parseInt(message)-1;
		return (location >= 0 && location < colorNumber.size() && !pickedSpaces[location]);
	}

	private String generateBoard()
	{
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append("  CALL YOUR SHOT   \n");
		for(int i=0; i<colorNumber.size(); i++)
		{
			if(pickedSpaces[i])
			{
				display.append(colorNames.get(colorNumber.get(i)).substring(0,2));
			}
			else
			{
				display.append(String.format("%02d",(i+1)));
			}
			if(i%7 == 6)
				display.append("\n");
			else
				display.append(" ");
		}
		display.append("\n");
		
		display.append(String.format("Bank: $%,d\n",total));
		display.append(String.format("Your color: %s\n",colorNames.get(colorPicked)));
		//If they picked red or gold, don't display the counter at all
		if(colorPicked > 0 && colorPicked < 5)
			display.append(String.format("Mistakes left: %d\n",colorPicked + 1 - roundNumber));
		display.append("```");
		return display.toString();
	}

	private String generateRevealBoard()
	{
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append("  CALL YOUR SHOT   \n");
		for(int i=0; i<colorNumber.size(); i++)
		{
			display.append(colorNames.get(colorNumber.get(i)).substring(0,2));
			if(i%7 == 6)
				display.append("\n");
			else
				display.append(" ");
		}
		display.append("```");
		return display.toString();
	}

	
	/**
	 * Returns true if the minigame has ended
	 */
	private boolean isGameOver()
	{
		return !alive || stop;
	}
	
	@Override
	String getBotPick()
	{
		if (roundNumber == -1 && colorPicked == 9) //Let's let the computer pick a random color
		{
			return colorNames.get(colorNumber.get((int)(Math.random()*21)));
		}
		else //No stopping this train!
		{
			ArrayList<Integer> openSpaces = new ArrayList<>(colorNumber.size());
			for(int i=0; i<colorNumber.size(); i++)
				if(!pickedSpaces[i])
					openSpaces.add(i+1);
			return String.valueOf(openSpaces.get((int)(Math.random()*openSpaces.size())));
		}
	}

	@Override
	void abortGame()
	{
		//No mercy here, sorry
		awardMoneyWon(0);
	}

	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	@Override public String getEnhanceText() { return "The gold ball always counts as a win, regardless of which colour you pick."; }
}