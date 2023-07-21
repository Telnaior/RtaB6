package tel.discord.rtab.games;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Stardust extends MiniGameWrapper
{
	static final String NAME = "Stardust";
	static final String SHORT_NAME = "Star";
	static final boolean BONUS = false;
	static final int[] BONUS_VALUES = new int[] {200_000, 500_000, 1_000_000, 2_500_000, 5_000_000};
	int total;
	int picksRemaining;
	int stage;
	List<Integer> numbersA = Arrays.asList(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2,2,2,2,2);
	List<Integer> numbersB = Arrays.asList(0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2,2,2,2);
	List<Integer> numbersC = Arrays.asList(0,0,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2,2,2);
	List<Integer> numbersD = Arrays.asList(0,0,0,0,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2,2);
	List<Integer> numbersE = Arrays.asList(0,0,0,0,0,0,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2);
	// 0 = Bomb, 1 = Normal Cash, 2 = Star
	boolean alive;
	boolean starHit;
	boolean[] pickedSpacesA;
	boolean[] pickedSpacesB;
	boolean[] pickedSpacesC;
	boolean[] pickedSpacesD;
	boolean[] pickedSpacesE;
	int lastSpace;
	int lastPick;
	int baseValue;
	
	/**
	 * Initializes the variables used in the minigame and prints the starting messages.
	 */
	@Override
	void startGame()
	{
		alive = true;
		total = 0;
		stage = 1;
		picksRemaining = 5;
		starHit = false;
		pickedSpacesA = new boolean[numbersA.size()];
		pickedSpacesB = new boolean[numbersB.size()];
		pickedSpacesC = new boolean[numbersC.size()];
		pickedSpacesD = new boolean[numbersD.size()];
		pickedSpacesE = new boolean[numbersE.size()];
		baseValue = 50_000;

		Collections.shuffle(numbersA);
		Collections.shuffle(numbersB);
		Collections.shuffle(numbersC);
		Collections.shuffle(numbersD);
		Collections.shuffle(numbersE);
		// Le Rules
		LinkedList<String> output = new LinkedList<>();
		output.add("In Stardust, you have a chance to win millions of dollars over the course of 5 stages!");
		output.add(String.format("There are 25 spaces in each stage. Most spaces contain $%,d, but there are also stars hidden among them.", 
				applyBaseMultiplier(baseValue)));
		output.add("In stage 1, you will have to make 5 picks and find at least one of the 5 stars on the board.");
		output.add(String.format("If you do, you win the stage's bonus value of $%,d and advance to the next stage.", applyBaseMultiplier(BONUS_VALUES[0])));
		output.add("Any cash you won, stars or otherwise, will be added to your bank. If you find multiple stars, then you win multiple bonuses!");
		output.add("After you finish all picks in a stage, if you found at least one star, you may choose to stop and collect your winnings.");
		output.add("...or continue playing in the next stage with less picks and less star spaces but bigger bonus value!");
		output.add("But later stages also have some bombs. Hit one and you lose everything.");
		output.add(String.format("Stage 2 has 4 picks, 4 star spaces, $%,d bonus and 1 bomb.", applyBaseMultiplier(BONUS_VALUES[1])));
		output.add(String.format("Stage 3 has 3 picks, 3 star spaces, $%,d bonus and 3 bombs.", applyBaseMultiplier(BONUS_VALUES[2])));
		output.add(String.format("Stage 4 has 2 picks, 2 star spaces, $%,d bonus and 5 bombs.", applyBaseMultiplier(BONUS_VALUES[3])));
		output.add(String.format("Stage 5 has 1 pick, 1 star space, $%,d bonus and 7 bombs.", applyBaseMultiplier(BONUS_VALUES[4])));
		output.add("The game ends when you finish a stage without finding a star, choose to stop, hit a bomb or complete Stage 5.");
		if(enhanced)
		{
			baseValue = 100_000;
			output.add(String.format("ENHANCE BONUS: The non-star cash values have been doubled to $%,d!",applyBaseMultiplier(baseValue)));
		}
		output.add("Good luck! Let's start with Stage 1! Choose a space.");
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
			if(picksRemaining != (6 - stage) || stage == 1) // Stopping is only available between stages.
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
			if(stage == 1)
			{
				pickedSpacesA[lastSpace] = true;
				lastPick = numbersA.get(lastSpace);
			}
			else if(stage == 2)
			{
				pickedSpacesB[lastSpace] = true;
				lastPick = numbersB.get(lastSpace);
			}
			else if(stage == 3)
			{
				pickedSpacesC[lastSpace] = true;
				lastPick = numbersC.get(lastSpace);
			}
			else if(stage == 4)
			{
				pickedSpacesD[lastSpace] = true;
				lastPick = numbersD.get(lastSpace);
			}
			else if(stage == 5)
			{
				pickedSpacesE[lastSpace] = true;
				lastPick = numbersE.get(lastSpace);
			}
			//Start printing output
			output.add(String.format("Space %d selected...",lastSpace+1));
			if(lastPick == 0) // Unlucky...
			{
				alive = false;
						total = 0;
						output.add("...");
						output.add("It's a **BOMB**.");
						output.add("Sorry, you lose.");
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
					output.add(String.format("It's a **STAR**! You've won a **$%,d** bonus!", applyBaseMultiplier(BONUS_VALUES[stage-1])));
					total += applyBaseMultiplier(BONUS_VALUES[stage-1]);
					if (starHit == false)
					{
						starHit = true;
						if (stage != 5)
						{
							output.add(String.format("Stage %d unlocked!", (stage+1)));
						}
						else
						{
							sendMessages(output);
							output.clear();
							//Achievement.PERFECT_STARDUST.check(getCurrentPlayer()); TODO add achievement
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
						if (stage == 5)
						{
							output.add("You have achieved the **PERFECT STARDUST**! Congratulations!");
							alive = false;
						}
						else
						{
							stage ++;
							picksRemaining = 6;
							picksRemaining -= stage;
							if(stage == 2)
							{
								output.add("You've made it to Stage 2!");
								output.add(String.format("You need to make 4 picks in this stage. "
										+ "There are now 4 star spaces and 1 bomb space on the board. The bonus is now worth $%,d!", applyBaseMultiplier(BONUS_VALUES[1])));
								output.add("You can now decide to STOP, or continue by picking a space on this new board.");
							}
							else if(stage == 3)
							{
								output.add("You've made it to Stage 3!");
								output.add(String.format("You need to make 3 picks in this stage. "
										+ "There are now 3 star spaces and 3 bomb spaces on the board. The bonus is now worth $%,d!", applyBaseMultiplier(BONUS_VALUES[2])));
								output.add("You can now decide to STOP, or continue by picking a space on this new board.");
							}
							else if(stage == 4)
							{
								output.add("You've made it to Stage 4!");
								output.add(String.format("You need to make 2 picks in this stage. "
										+ "There are now 2 star spaces and 5 bomb spaces on the board. The bonus is now worth $%,d!", applyBaseMultiplier(BONUS_VALUES[3])));
								output.add("You can now decide to STOP, or continue by picking a space on this new board.");
							}
							else if(stage == 5)
							{
								output.add("You've made it to the final Stage!");
								output.add(String.format("There are 7 bombs and one star space on this new board. "
										+ "If you make one final pick and hit the star, you will win the $%,d bonus!", applyBaseMultiplier(BONUS_VALUES[4])));
								output.add("You can now decide to STOP, or continue by picking a space on this new board.");
							}
								output.add(generateBoard());
						}
					}
					else //No star means the game is over.
					{
						output.add("That's the end of the game!");
						alive = false;
					}
					
				}
				else // Otherwise let 'em pick another space.
				{
					output.add(String.format("%d picks remaining.", picksRemaining));
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
		if (stage == 1)
		{
			int location = Integer.parseInt(message)-1;
			return (location >= 0 && location < numbersA.size() && !pickedSpacesA[location]);
		}
		else if (stage == 2)
		{
			int location = Integer.parseInt(message)-1;
			return (location >= 0 && location < numbersB.size() && !pickedSpacesB[location]);
		}
		else if (stage == 3)
		{
			int location = Integer.parseInt(message)-1;
			return (location >= 0 && location < numbersC.size() && !pickedSpacesC[location]);
		}
		else if (stage == 4)
		{
			int location = Integer.parseInt(message)-1;
			return (location >= 0 && location < numbersD.size() && !pickedSpacesD[location]);
		}
		else if (stage == 5)
		{
			int location = Integer.parseInt(message)-1;
			return (location >= 0 && location < numbersE.size() && !pickedSpacesE[location]);
		}
		else return false;
	}
	
	private String generateBoard()
	{
		if (stage == 1)
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
			for(int i=0; i<numbersA.size(); i++)
			{
				if(pickedSpacesA[i])
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
			display.append(String.format("Bank: $%,d\n",total));
			display.append("Stage: 1\n");
			display.append(String.format("%d picks remaining\n",picksRemaining));
			display.append("```");
			return display.toString();
		}
		else if (stage == 2)
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
			for(int i=0; i<numbersB.size(); i++)
			{
				if(pickedSpacesB[i])
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
			display.append(String.format("Bank: $%,d\n",total));
			display.append("Stage: 2\n");
			display.append(String.format("%d picks remaining\n",picksRemaining));
			display.append("```");
			return display.toString();
		}
		else if (stage == 3)
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
			for(int i=0; i<numbersC.size(); i++)
			{
				if(pickedSpacesC[i])
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
			display.append(String.format("Bank: $%,d\n",total));
			display.append("Stage: 3\n");
			display.append(String.format("%d picks remaining\n",picksRemaining));
			display.append("```");
			return display.toString();
		}
		else if (stage == 4)
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
			for(int i=0; i<numbersD.size(); i++)
			{
				if(pickedSpacesD[i])
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
			display.append(String.format("Bank: $%,d\n",total));
			display.append("Stage: 4\n");
			display.append(String.format("%d picks remaining\n",picksRemaining));
			display.append("```");
			return display.toString();
		}
		else if (stage == 5)
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
			for(int i=0; i<numbersE.size(); i++)
			{
				if(pickedSpacesE[i])
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
			display.append(String.format("Bank: $%,d\n",total));
			display.append("FINAL STAGE\n");
			display.append(String.format("%d pick remaining\n",picksRemaining));
			display.append("```");
			return display.toString();
		}
		else return "```\nBADSTAGE\n```";
	}
	
	@Override
	String getBotPick()
	{
		// TODO Auto-generated method stub
		return null;
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
