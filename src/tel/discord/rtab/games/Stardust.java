package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import tel.discord.rtab.Achievement;

public class Stardust extends MiniGameWrapper
{
	static final String NAME = "Stardust";
	static final String SHORT_NAME = "Star";
	static final boolean BONUS = false;
	static final int[] BONUS_VALUES = new int[] {100_000, 250_000, 500_000, 1_000_000, 5_000_000};
	static final int BASE_VALUE = 25_000;
	static final int BOARD_SIZE = 25;
	static final int STAGES = 5;
	int total;
	int picksRemaining;
	int stage;
	int[] starCount;
	int[] bombCount;
	ArrayList<List<Integer>> numbers;
	boolean alive;
	boolean starHit;
	boolean[][] pickedSpaces;
	int lastSpace;
	int lastPick;
	int baseValue;
	int clusterNumber;
	
	/**
	 * Initializes the variables used in the minigame and prints the starting messages.
	 */
	@Override
	void startGame()
	{
		baseValue = BASE_VALUE;
		alive = true;
		total = 0;
		stage = 0;
		picksRemaining = 5;
		starHit = false;
		//Generate spaces
		// 0 = Bomb, 1 = Normal Cash, 2 = Star
		starCount = new int[STAGES];
		bombCount = new int[STAGES];
		int stars = 5;
		int bombs = -1;
		int currentStage = 0;
		numbers = new ArrayList<List<Integer>>(STAGES);
		while(currentStage < STAGES)
		{
			ArrayList<Integer> newStage = new ArrayList<Integer>(BOARD_SIZE);
			for(int i=0; i<BOARD_SIZE; i++)
			{
				if(i < stars)
					newStage.add(2);
				else if(i + bombs < BOARD_SIZE)
					newStage.add(1);
				else
					newStage.add(0);
			}
			Collections.shuffle(newStage);
			numbers.add(newStage);
			starCount[currentStage] = stars;
			bombCount[currentStage] = bombs;
			currentStage++;
			//variation in where we end up
			if(Math.random() < 0.5)
			{
				stars -= 1;
				bombs += 2;
			}
			else
			{
				bombs += 4;
			}
		}
		pickedSpaces = new boolean[STAGES][BOARD_SIZE];
		clusterNumber = (int)(Math.random()*10000);
		// Le Rules
		LinkedList<String> output = new LinkedList<>();
		output.add("In Stardust, you have a chance to win millions of dollars on a 5-stage rocket trip!");
		output.add(String.format("There are 25 spaces in each stage. Most spaces contain $%,d, but there are also stars hidden among them.", 
				applyBaseMultiplier(baseValue)));
		output.add("In stage 1 (the Home Nebula), you will have enough fuel to make 5 picks from the board, "
				+ "and your objective is to find at least one of the 5 hidden stars.");
		output.add(String.format("Each star you find will award you the star bonus, which begins at $%,d.", applyBaseMultiplier(BONUS_VALUES[0])));
		output.add("Any cash you won, stars or otherwise, will be added to your bank. If you find multiple stars, then you win multiple bonuses!");
		output.add("After you finish all picks in a stage, if you found at least one star, you may choose to stop and collect your winnings, "
				+ "or continue playing in the next stage with less fuel but a bigger star bonus!");
		output.add("But later stages also have some black holes. Hit one and you lose everything.");
		output.add("The game ends when you finish a stage without finding a star, choose to stop, fall into a black hole, or complete Stage 5.");
		if(enhanced)
		{
			baseValue *= 2;
			output.add(String.format("ENHANCE BONUS: The non-star cash values have been doubled to $%,d!",applyBaseMultiplier(baseValue)));
		}
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
			if(picksRemaining < (5 - stage) || stage == 0) // Stopping is only available between stages.
			{
				output.add("You can only stop between stages!");
			}
			else
			{
				// Player stops at the decision point? Tell 'em what they've won and end the game!
				alive = false;
					output.add("Very well.");
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
			pickedSpaces[stage][lastSpace] = true;
			lastPick = numbers.get(stage).get(lastSpace);
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
					output.add(String.format("**$%,d**!", applyBaseMultiplier(baseValue)));
					total += applyBaseMultiplier(baseValue);
					picksRemaining--;
				}
				else if (lastPick == 2) //A Star!
				{
					output.add("...");
					output.add(String.format("It's a **STAR**! You've won a **$%,d** bonus!", applyBaseMultiplier(BONUS_VALUES[stage])));
					total += applyBaseMultiplier(BONUS_VALUES[stage]);
					if (starHit == false)
					{
						starHit = true;
						if (stage < 4)
						{
							output.add("Next Stage unlocked!");
						}
						else
						{
							sendMessages(output);
							output.clear();
							Achievement.STARDUST_JACKPOT.check(getPlayer());
						}
					}
					picksRemaining--;
				}
			}
			if(alive)
			{

				if(picksRemaining == 0) //End of stage!
				{
					if(starHit == true) //If at least one star is hit then the player can go to the next stage.
					{
						starHit = false;
						if (stage >= 4)
						{
							output.add("You have achieved the **PERFECT STARDUST**! Congratulations!");
							alive = false;
						}
						else
						{
							stage ++;
							picksRemaining = 5;
							picksRemaining -= stage;
							if(stage == 1)
							{
								output.add("You've made it to the Galactic Arm!");
								output.add(String.format("You have fuel for 4 picks in this stage. "
										+ "There are now %d stars and %d black holes on the board. The star bonus is now worth $%,d!",
										starCount[1],bombCount[1],applyBaseMultiplier(BONUS_VALUES[1])));
								output.add("You can now decide to STOP, or continue by picking a space on this new board.");
							}
							else if(stage == 2)
							{
								output.add("You've made it to the Gloaming Galaxy!");
								output.add(String.format("You have fuel for 3 picks in this stage. "
										+ "There are now %d stars and %d black holes on the board. The star bonus is now worth $%,d!",
										starCount[2],bombCount[2],applyBaseMultiplier(BONUS_VALUES[2])));
								output.add("You can now decide to STOP, or continue by picking a space on this new board.");
							}
							else if(stage == 3)
							{
								output.add("You've made it to Abell "+clusterNumber+"\n!");
								output.add(String.format("You have fuel for 2 picks in this stage. "
										+ "There are now %d stars and %d black holes on the board. The star bonus is now worth $%,d!",
										starCount[3],bombCount[3],applyBaseMultiplier(BONUS_VALUES[3])));
								output.add("You can now decide to STOP, or continue by picking a space on this new board.");
							}
							else if(stage == 4)
							{
								output.add("You've made it to the Edge of the Universe!");
								output.add(String.format("There are %d stars and %d black holes on this final board. "
										+ "If you reach a star with the last of your fuel, you will win the $%,d star bonus!",
										starCount[4],bombCount[4],applyBaseMultiplier(BONUS_VALUES[4])));
								output.add("You can now decide to STOP, or continue by picking a space on this new board.");
							}
								output.add(generateBoard());
						}
					}
					else //No star means the game is over.
					{
						output.add("We're out of fuel without a star, so that's the end of the game!");
						alive = false;
					}
					
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
	
	private boolean checkValidNumber(String message)
	{
		int location = Integer.parseInt(message)-1;
		return (location >= 0 && location < numbers.get(stage).size() && !pickedSpaces[stage][location]);
	}
	
	private String generateBoard()
	{
		if (stage == 0)
		{
			StringBuilder display = new StringBuilder();
			display.append("```\n");
			if(starHit)
			{
				display.append("*  STARDUST   \n");
			}
			else
			{
				display.append("   STARDUST   \n");
			}
			for(int i=0; i<numbers.get(stage).size(); i++)
			{
				if(pickedSpaces[stage][i])
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
			display.append("Home Nebula\n");
			display.append(String.format("Bank: $%,d\n",total));
			display.append(String.format("%d fuel remaining\n",picksRemaining));
			display.append("```");
			return display.toString();
		}
		else if (stage == 1)
		{
			StringBuilder display = new StringBuilder();
			display.append("```\n");
			if(starHit)
			{
				display.append("* *STARDUST   \n");
			}
			else
			{
				display.append("*  STARDUST   \n");
			}
			for(int i=0; i<numbers.get(stage).size(); i++)
			{
				if(pickedSpaces[stage][i])
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
			display.append("Galactic Arm\n");
			display.append(String.format("Bank: $%,d\n",total));
			display.append(String.format("%d fuel remaining\n",picksRemaining));
			display.append("```");
			return display.toString();
		}
		else if (stage == 2)
		{
			StringBuilder display = new StringBuilder();
			display.append("```\n");
			if(starHit)
			{
				display.append("* *STARDUST*  \n");
			}
			else
			{
				display.append("* *STARDUST   \n");
			}
			for(int i=0; i<numbers.get(stage).size(); i++)
			{
				if(pickedSpaces[stage][i])
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
			display.append("Gloaming Galaxy\n");
			display.append(String.format("Bank: $%,d\n",total));
			display.append(String.format("%d fuel remaining\n",picksRemaining));
			display.append("```");
			return display.toString();
		}
		else if (stage == 3)
		{
			StringBuilder display = new StringBuilder();
			display.append("```\n");
			if(starHit)
			{
				display.append("* *STARDUST* *\n");
			}
			else
			{
				display.append("* *STARDUST*  \n");
			}
			for(int i=0; i<numbers.get(stage).size(); i++)
			{
				if(pickedSpaces[stage][i])
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
			display.append("Abell "+clusterNumber+"\n");
			display.append(String.format("Bank: $%,d\n",total));
			display.append(String.format("%d fuel remaining\n",picksRemaining));
			display.append("```");
			return display.toString();
		}
		else if (stage == 4)
		{
			StringBuilder display = new StringBuilder();
			display.append("```\n");
			if(starHit)
			{
				display.append("***STARDUST***\n");
			}
			else
			{
				display.append("* *STARDUST* *\n");
			}
			for(int i=0; i<numbers.get(stage).size(); i++)
			{
				if(pickedSpaces[stage][i])
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
			display.append("Edge of the Universe\n");
			display.append(String.format("Bank: $%,d\n",total));
			display.append(String.format("%d fuel remaining\n",picksRemaining));
			display.append("```");
			return display.toString();
		}
		else return "```\nBADSTAGE\n```";
	}
	
	@Override
	String getBotPick()
	{
		//Stop test goes 20% / 40% / 60% / 80%
		if(picksRemaining == (6 - stage) && (Math.random() * STAGES) < stage)
			return "STOP";
		else
		{
			//random open space in the usual way
			ArrayList<Integer> openSpaces = new ArrayList<>(BOARD_SIZE);
			for(int i=0; i<BOARD_SIZE; i++)
				if(!pickedSpaces[stage][i])
					openSpaces.add(i+1);
			return String.valueOf(openSpaces.get((int)(Math.random()*openSpaces.size())));
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
	@Override public String getEnhanceText() { return "The value of non-star spaces are doubled."; }

}
