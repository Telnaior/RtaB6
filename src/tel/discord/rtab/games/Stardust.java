package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import tel.discord.rtab.Achievement;
import tel.discord.rtab.RtaBMath;

public class Stardust extends MiniGameWrapper
{
	static final String NAME = "Stardust";
	static final String SHORT_NAME = "Star";
	static final boolean BONUS = false;
	static final int STAGES = 5;
	static final int BASE_OPTIONS_PER_STAGE = 2;
	Galaxy currentStage;
	List<Galaxy> stageOptions;
	List<Galaxy> previousStages;
	int total;
	int picksRemaining;
	int stage;
	int optionsPerStage;
	List<Integer> numbers;
	boolean alive;
	boolean starHit;
	boolean canStop;
	boolean[] pickedSpaces;
	int lastSpace;
	int lastPick;
	int clusterNumber;
	
	enum Galaxy
	{
		//board will look bad if board size isn't divisible by 5, everything else is fair game
		HOME("Home Nebula", new boolean[] {true,false,false,false,false}, 25, 10, 0, 75_000, 25_000, null),
		BASE_S2("Galactic Arm", new boolean[] {false,true,false,false,false}, -1, -1, -1, -1, -1, null),
		BASE_S3("Medium Supercluster", new boolean[] {false,false,true,false,false}, -1, -1, -1, -1, -1, null),
		BASE_S4("Intergalactic Expanse", new boolean[] {false,false,false,true,false}, -1, -1, -1, -1, -1, null),
		BASE_S5("Edge of the Universe", new boolean[] {false,false,false,false,true}, -1, -1, -1, -1, -1, null),
		GLOAMING("Gloaming Galaxy", new boolean[] {false,true,false,false,false}, 25, 5, 0, 100_000, 50_000, null),
		SUPERDENSE("Superdense System", new boolean[] {false,true,true,false,false}, 10, 3, 1, 250_000, 50_000, null),
		SOLAR("Solar Sector", new boolean[] {false,true,true,false,false}, 25, 4, 1, 250_000, 50_000,
				"SPECIAL: If you find a star, your picks will be restored!"),
		DARKMATTER("Dark Matter Deluge", new boolean[] {false,true,true,false,false}, 25, 4, 0, 750_000, -50_000, null),
		CELESTIA("Celestia's Fate", new boolean[] {false,true,true,true,false}, 25, 8, 4, 1_000_000, 50_000,
				"You sense this memorial is the end of your journey..."),
		ABELL("Abell %d", new boolean[] {false,false,true,true,false}, 25, 3, 2, 100_000, 0, null)
			{ int getBaseValue(int stage, int clusterNumber) { return 100 * clusterNumber; } },
		ALLOR("Vast Allor Nothingness", new boolean[] {false,false,false,true,true}, 30, 5, 8, 3_000_000, 0, null),
		BLASTEROID("Blasteroid Belt", new boolean[] {false,false,true,true,true}, 25, 5, 10, 3_500_000, 100_000, null),
		CHESSBOARD("Celestial Chessboard", new boolean[] {false,false,false,true,true}, 20, 10, 10, 5_000_000, 0, null),
		GRANDMASTER("Grandmaster Galaxy", new boolean[] {false,false,false,false,true}, 25, 3, 3, 2_500_000, 50_000,
				"SPECIAL: You have unlimited fuel, and you can stop and take your winnings at any time!"),
		HYPERLOOP("Apollo's Hyperloop", new boolean[] {false,false,true,true,true}, -1, -1, -1, -1, -1,
				"You feel a strange sense of deja vu..."),
		WORMHOLE("Apollo's Wormhole", new boolean[] {false,true,true,false,false}, -1, -1, -1, -1, -1,
				"You tumble through the wormhole into the future!"),
		VOID("Void", new boolean[] {false,true,true,true,false}, 50, 0, 0, 0, 100_000, null);
		
		private static final int[] DEFAULT_STARS = new int[] {5, 4, 3, 2, 1};
		private static final int[] DEFAULT_BOMBS = new int[] {0, 1, 2, 4, 8};
		private static final int[] DEFAULT_STAR_VALUE = new int[] {100_000, 250_000, 500_000, 1_000_000, 5_000_000};
		private static final int[] DEFAULT_BASE_VALUE = new int[] {25_000, 25_000, 25_000, 25_000, 25_000};
		private static final int DEFAULT_BOARD_SIZE = 25;
		private final String name, special;
		final boolean[] eligibleStages;
		private final int boardSize, starCount, bombCount, starValue, baseValue;
		Galaxy(String name, boolean[] eligibleStages, int boardSize, int starCount, int bombCount, int starValue, int baseValue, String special)
		{
			this.name = name;
			this.eligibleStages = eligibleStages;
			this.boardSize = boardSize;
			this.starCount = starCount;
			this.bombCount = bombCount;
			this.starValue = starValue;
			this.baseValue = baseValue;
			this.special = special;
		}
		String getStageName(int clusterNumber)
		{
			return String.format(name, clusterNumber);
		}
		String getSpecial()
		{
			return special;
		}
		int getBoardSize(int stage)
		{
			if(boardSize == -1)
				return DEFAULT_BOARD_SIZE;
			return boardSize;
		}
		int getStars(int stage)
		{
			if(starCount == -1)
				return DEFAULT_STARS[stage];
			return starCount;
		}
		int getBombs(int stage)
		{
			if(bombCount == -1)
				return DEFAULT_BOMBS[stage];
			return bombCount;
		}
		int getStarValue(int stage, int clusterNumber)
		{
			if(starValue == -1)
				return DEFAULT_STAR_VALUE[stage];
			return starValue;
		}
		int getBaseValue(int stage, int clusterNumber)
		{
			if(baseValue == -1)
				return DEFAULT_BASE_VALUE[stage];
			return baseValue;
		}
	}
	
	int getStarValue()
	{
		return applyBaseMultiplier(currentStage.getStarValue(stage, clusterNumber));
	}
	int getBaseValue()
	{
		return applyBaseMultiplier(currentStage.getBaseValue(stage, clusterNumber));
	}
	int getStars()
	{
		return currentStage.getStars(stage);
	}
	int getBombs()
	{
		return currentStage.getBombs(stage);
	}
	String getStageName()
	{
		return currentStage.getStageName(clusterNumber);
	}
	
	/**
	 * Initializes the variables used in the minigame and prints the starting messages.
	 */
	@Override
	void startGame()
	{
		alive = true;
		clusterNumber = (int)(RtaBMath.random()*10000);
		total = 0;
		stage = 0;
		previousStages = new ArrayList<>();
		optionsPerStage = BASE_OPTIONS_PER_STAGE;
		prepareStage(Galaxy.HOME);
		// Le Rules
		LinkedList<String> output = new LinkedList<>();
		output.add("In Stardust, you have a chance to win millions of dollars on a rocket trip through five zones!");
		output.add("Each zone will present you with a board filled with stars, black holes, and filler spaces.");
		output.add("In Zone 1 (the Home Nebula), you will have enough fuel to make 5 picks from the 25-space board, "
				+ "which contains 10 stars and no black holes.");
		output.add(String.format("Each star you find will award you the star bonus, which for the Home Nebula is $%,d, "
				+ "and each filler space will award you $%,d.", getStarValue(), getBaseValue()));
		output.add("Any cash you win will be added to your bank. If you find multiple stars, then you win multiple star bonuses!");
		output.add("Once you run out of fuel, if you found at least one star, you may choose your next zone from two options, "
				+ "or stop and collect your winnings.");
		if(enhanced)
		{
			output.add("ENHANCE BONUS: You will be given a third option when choosing your next stage.");
			optionsPerStage ++;
		}
		output.add("Each future zone will have 1 less fuel than the previous, and will also contain some black holes. "
				+ "Hit one and you lose everything.");
		output.add("The game ends when you finish a zone without finding a star, choose to stop, "
				+ "fall into a black hole, or complete Zone 5.");
		output.add("Good luck! Choose a space to begin.");
		sendSkippableMessages(output);
		sendMessage(generateBoard());
		getInput();
	}

	/**
	 * Takes the next player input and uses it to play the next "turn" - up until the next input is required.
	 * @param pick  The next input sent by the player.
	 */
	@Override
	void playNextTurn(String pick)
	{
		LinkedList<String> output = new LinkedList<>();
		if(pick.equalsIgnoreCase("STOP"))
		{
			if(canStop)
			{
				alive = false;
				output.add("Very well.");
			}
			else
			{
				output.add("You can only stop between zones!");
			}
		}
		else if(picksRemaining == 0)
		{
			//If we're currently on 0 picks then we're at the galaxy selection screen, so process their choice
			switch(pick.toUpperCase())
			{
			case "A", "1" -> prepareStage(stageOptions.get(0));
			case "B", "2" -> prepareStage(stageOptions.get(1));
			case "C", "3" -> { if(enhanced) prepareStage(stageOptions.get(2)); }
			//We get input at the end of the method, so we just ignore anything else
			}
			if(picksRemaining != 0) //if we successfully picked a galaxy
			{
				output.add(String.format("Welcome to the %s! Here there are %d stars and %d black holes.",getStageName(),getStars(),getBombs()));
				if(currentStage.getSpecial() != null)
					output.add(currentStage.getSpecial());
				output.add(String.format("Your star bonus is $%,d, and filler spaces are worth $%,d.", getStarValue(),getBaseValue()));
				output.add(generateBoard());
			}
		}
		else if(!isNumber(pick))
		{
			//Still don't say anything for random strings
		}
		else if(!checkValidNumber(pick))
		{
			// EASTER EGG! Take the RTaB Challenge!
			// Hit this message 29,998,559,671,349 times in a row
			output.add("Invalid pick.");
			// and you win a free kick from the server
		}
		else
		{	
			lastSpace = Integer.parseInt(pick)-1;
			pickedSpaces[lastSpace] = true;
			lastPick = numbers.get(lastSpace);
			//Start printing output
			output.add(String.format("Space %d selected...",lastSpace+1));
			if(lastPick == 0) // Unlucky...
			{
				alive = false;
						total = 0;
						output.add("...");
						output.add("It's a **BLACK HOLE**.");
						output.add(String.format("Goodbye, %s...", getPlayer().getName()));
			}
			else
			{
				if(lastPick == 1) //A win is a win
				{
					output.add(String.format("**$%,d**!", getBaseValue()));
					total += getBaseValue();
					picksRemaining--;
				}
				else if (lastPick == 2) //A Star!
				{
					output.add("...");
					output.add(String.format("It's a **STAR**! You've won a **$%,d** bonus!", getStarValue()));
					total += applyBaseMultiplier(getStarValue());
					if (!starHit)
					{
						starHit = true;
						if (stage < 4 && currentStage != Galaxy.CELESTIA)
						{
							output.add("Next Zone unlocked!");
						}
						else
						{
							sendMessages(output);
							output.clear();
							Achievement.STARDUST_JACKPOT.check(getPlayer());
						}
					}
					picksRemaining--;
					
					switch(currentStage) //SPECIAL EFFECTS
					{
					case SOLAR ->
						{
							output.add("Fuel restored!");
							picksRemaining = 5-(stage);
						}
					default -> { }
					}
				}
			}
			if(alive)
			{

				if(picksRemaining == 0) //End of stage!
				{
					output.addAll(endOfStage());
				}
				else // Otherwise let 'em pick another space.
				{
					output.add(generateBoard());
				}
			
			}
		}
		sendMessages(output);
		if(!alive)
			awardMoneyWon(total);
		else
			getInput();
	}
	
	void prepareStage(Galaxy next)
	{
		currentStage = next;
		previousStages.add(next);
		switch(currentStage) //SPECIAL EFFECTS
		{
		case WORMHOLE -> stage ++;
		case HYPERLOOP -> stage --;
		default -> { }
		}
		
		ArrayList<Integer> newStage = new ArrayList<>();
		for(int i=0; i<next.getBoardSize(stage); i++)
		{
			if(i < getStars())
				newStage.add(2);
			else if(i + getBombs() < next.getBoardSize(stage))
				newStage.add(1);
			else
				newStage.add(0);
		}
		Collections.shuffle(newStage);
		numbers = newStage;
		pickedSpaces = new boolean[next.getBoardSize(stage)];
		starHit = false;
		picksRemaining = (5-stage);
		canStop = false;
		
		switch(currentStage) //SPECIAL EFFECTS
		{
		case GRANDMASTER ->
		{ //Infinite fuel, stop at any time
			picksRemaining = -1;
			canStop = true;
		}
		default -> { }
		}
	}
	
	private LinkedList<String> endOfStage()
	{
		LinkedList<String> output = new LinkedList<>();
		if(starHit) //If at least one star is hit then the player can go to the next stage.
		{
			if (stage >= 4)
			{
				output.add("You have achieved the **PERFECT STARDUST**! Congratulations!");
				alive = false;
			}
			else
			{
				switch(currentStage) //SPECIAL EFFECTS
				{
				case CELESTIA ->
					{
						output.add("Having arrived at Celestia's Fate, your journey is over.");
						alive = false;
						return output;
					}
				default -> { }
				}
				
				stage ++;
				//Get list of eligible options for the next stage
				stageOptions = new ArrayList<Galaxy>();
				stageOptions.addAll(Arrays.asList(Galaxy.values()));
				stageOptions.removeIf((g) -> !g.eligibleStages[stage]);
				stageOptions.removeAll(previousStages);
				Collections.shuffle(stageOptions);
				output.add("You're off to the next zone!");
				output.add("You see the following locations around you...");
				StringBuilder result = new StringBuilder();
				result.append("```\n");
				for(int i=0; i<optionsPerStage; i++)
				{
					result.append(String.format("%c - %s%n", 65+i, stageOptions.get(i).getStageName(clusterNumber)));
				}
				result.append(String.format("STOP - Head home with your $%,d%n```",total));
				output.add(result.toString());
				output.add("Where would you like to go?");
				canStop = true;
			}
		}
		else //No star means the game is over.
		{
			output.add("We're out of fuel without a star, so that's the end of the game!");
			alive = false;
		}
		return output;
	}
	
	private boolean checkValidNumber(String message)
	{
		int location = Integer.parseInt(message)-1;
		return (location >= 0 && location < numbers.size() && !pickedSpaces[location]);
	}
	
	private String generateBoard()
	{
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append(switch(stage + (starHit ? 1 : 0))
		{
		case 5 -> "***STARDUST***\n";
		case 4 -> "* *STARDUST* *\n";
		case 3 -> "* *STARDUST*  \n";
		case 2 -> "* *STARDUST   \n";
		case 1 -> "*  STARDUST   \n";
		default -> "   STARDUST   \n";
		});
		for(int i=0; i<numbers.size(); i++)
		{
			if(pickedSpaces[i])
			{
				display.append("  ");
			}
			else
			{
				display.append(String.format("%02d",(i+1)));
			}
			if(i%5 == 4)
				display.append("\n");
			else
				display.append(" ");
		}
		display.append("\n");
		//Next display our bank and number of picks left
		display.append(String.format("Zone %d: %s", stage+1, getStageName()));
		display.append("\n");
		display.append(String.format("Bank: $%,d%n",total));
		if(picksRemaining > 0)
			display.append(String.format("%d fuel remaining%n",picksRemaining));
		display.append("```");
		return display.toString();
	}
	
	@Override
	String getBotPick()
	{
		//Stop test goes 20% / 40% / 60% / 80%
		if(canStop && (RtaBMath.random() * STAGES) < stage)
			return "STOP";
		else if(picksRemaining == 0)
		{
			//We're between stages but already chose not to stop, so pick a random next stage option
			return String.valueOf((int)(RtaBMath.random()*optionsPerStage)+1);
		}
		else
		{
			//random open space in the usual way
			ArrayList<Integer> openSpaces = new ArrayList<>(pickedSpaces.length);
			for(int i=0; i<pickedSpaces.length; i++)
				if(!pickedSpaces[i])
					openSpaces.add(i+1);
			return String.valueOf(openSpaces.get((int)(RtaBMath.random()*openSpaces.size())));
		}
	}

	@Override
	void abortGame()
	{
		//Just... take the total you've got lol
		awardMoneyWon(total);
	}

	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	@Override public String getEnhanceText() { return "You are given a third option when choosing your next zone."; }

}
