package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import tel.discord.rtab.MoneyMultipliersToUse;

public class Overflow extends MiniGameWrapper {
	static final String NAME = "Overflow";
	static final String SHORT_NAME = "Flow";
	static final boolean BONUS = false;
	static final int BOARD_SIZE = 20;
	
	int moneyScore, streakScore, boostScore, turnsScore, chargerScore;
	int moneyPicked, streakPicked, boostPicked, turnsPicked, chargerPicked;
	int currentPick, genericValue, roundNumber;
	int annuityAmount;
	ArrayList<Integer> board = new ArrayList<Integer>(BOARD_SIZE);
	boolean[] pickedSpaces = new boolean[BOARD_SIZE];
	boolean weAreDone, canQuit, needsDoubling;
	
	@Override
	void startGame()
	{
		LinkedList<String> output = new LinkedList<>();
		annuityAmount = applyBaseMultiplier(10_000);
		board.clear();
		board.addAll(11, 12, 13, 14, 15, 21, 22, 23, 31, 32, 33, 41, 42, 43, 51, 52, 53, 77, 77, 0);
			//11-15 is money (35k/50k/75k/100k/125k)
			//21-23 is streak (0.1x/0.2x/0.3x)
			//31-33 is boost (20/25/30%)
			//41-43 is turns of 10k annuity (3/4/5)
			//51-53 is boost charge (2/3/5% per turn)
			//77 is a joker and is the overflow
		Collections.shuffle(board);
		pickedSpaces = new boolean[BOARD_SIZE];
		//Prep other variables
		weAreDone = false;
		canQuit = false;
		needsDoubling = false;
		moneyScore = 0;
		streakScore = 0;
		boostScore = 0;
		turnsScore = 0;
		chargerScore = 0;
		
		moneyPicked = 0;
		streakPicked = 0;
		boostPicked = 0;
		turnsPicked = 0;
		chargerPicked = 0;
		currentPick = -1;
		genericValue = -1;
		roundNumber = 0;
		//Display instructions
		output.add("In Overflow, you are playing for an array of prizes!");
		output.add("You could win money, boost, streak bonus, "
				+ String.format("or even Boost Charger percent and turns of $%,d annuity!",annuityAmount));
		output.add("One at a time, you'll pick a block. The first time you pick a prize block, you'll get its value in your bank.");
		output.add("The second time you pick a block of a prize type, you'll double your banked amount for that prize!");
		output.add("Watch out, though! If you pick the third block of that prize type, you Overflow, and you win nothing.");
		output.add("You'll also lose if you pick the Overflow block, so avoid that.");
		output.add("Finally, there are two Joker blocks. Pick one of those, and you can pick a prize to double without adding any risk of Overflow!");
		output.add("Now, let's begin. What block number will you start with?");
		sendSkippableMessages(output);
		sendMessage(generateBoard());
		getInput();
	}
	
	@Override
	public void playNextTurn(String pick)
	{
		LinkedList<String> output = new LinkedList<>();
		if(pick.equalsIgnoreCase("STOP"))
		{
			//Check if they can quit or not
			if(canQuit)
			{
				output.add("Very well, enjoy your loot!");
				StringBuilder resultString = new StringBuilder();
				StringBuilder extraResult = null;
				if (getCurrentPlayer().isBot)
				{
					resultString.append(getCurrentPlayer().name + " won ");
					//* gameMultiplier
				}
				else
				{
					resultString.append("Game Over. You won ");
				}
				if (moneyScore != 0)
				{
					int moneyWon = applyBaseMultiplier(moneyScore * gameMultiplier);
					resultString.append(String.format("**$%,d%** in cash, ",moneyWon));
					extraResult = getCurrentPlayer().addMoney(moneyWon, MoneyMultipliersToUse.BOOSTER_OR_BONUS);
				}
				if (streakScore != 0)
				{
					int streakWon = streakScore * gameMultiplier;
					resultString.append(String.format("**+%1$d.%2$dx** Streak bonus, ",streakWon / 10, streakWon % 10));
					getCurrentPlayer().winstreak = getCurrentPlayer().winstreak + streakWon;
				}
				if (boostScore != 0)
				{
					int boostWon = boostScore * gameMultiplier;
					resultString.append(String.format("**+%d%%** in boost... ",boostWon));
					getCurrentPlayer().addBooster(boostWon);
				}
				if (turnsScore != 0)
				{
					int turnsWon = turnsScore * gameMultiplier;
					resultString.append(String.format("**%d%** turns of $%,d per turn annuity... ",turnsWon,annuityAmount));
					getCurrentPlayer().addAnnuity(annuityAmount,turnsWon);
				}
				if (chargerScore != 0)
				{
					int chargerWon = chargerScore * gameMultiplier;
					resultString.append(String.format("and **+%d%%** in boost per turn until you bomb... ",chargerWon));
					getCurrentPlayer().boostCharge = getCurrentPlayer().boostCharge + chargerWon;
				}		
				if(gameMultiplier > 1)
					resultString.append(String.format("from %d copies of ",gameMultiplier));
				resultString.append(getName() + ".");
				sendMessage(resultString.toString());
				if(extraResult != null)
					sendMessage(extraResult.toString());
				weAreDone = true;
				output.add(generateBoard());
			}
			else
			{
				output.add("There's no risk because you haven't picked a block yet!");
			}
		}
		else if (needsDoubling && (pick.equalsIgnoreCase("CASH") || pick.equalsIgnoreCase("MONEY") || pick.equalsIgnoreCase("BUCKS")))
		{
			doubleMoney();
			needsDoubling = false;
			sendMessage(generateBoard());
		}
		else if (needsDoubling && pick.equalsIgnoreCase("STREAK"))
		{
			doubleStreak();
			needsDoubling = false;
			sendMessage(generateBoard());
		}
		else if (needsDoubling && (pick.equalsIgnoreCase("BOOST") || pick.equalsIgnoreCase("PERCENT")))
		{
			doubleBoost();
			needsDoubling = false;
			sendMessage(generateBoard());
		}
		else if (needsDoubling && (pick.equalsIgnoreCase("ANNUITY") || pick.equalsIgnoreCase("TURNS")))
		{
			doubleAnnuity();
			needsDoubling = false;
			sendMessage(generateBoard());
		}
		else if (needsDoubling && (pick.equalsIgnoreCase("CHARGE") || pick.equalsIgnoreCase("CHARGER")))
		{
			doubleCharger();
			needsDoubling = false;
			sendMessage(generateBoard());
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
		else if(!needsDoubling)
		{
			currentPick = Integer.parseInt(pick)-1;
			roundNumber++;
			canQuit = true;
			pickedSpaces[currentPick] = true;
			output.add(String.format("Space %d selected...",currentPick+1));
			if (moneyPicked == 2 || streakPicked == 2 || boostPicked == 2 || turnsPicked == 2 || chargerPicked == 2 ||
					board.get(currentPick) == 0 || board.get(currentPick) == 77 || Math.random() < .33)
			{
				output.add("...");
			}
			if (board.get(currentPick) == 0)
			{
				output.add("It's the **Overflow**.");
				output.add("Too bad, you don't win anything.");
				weAreDone = true;
				output.add(generateBoard());
			}
			else if (board.get(currentPick) <= 19)
			{
				if (moneyPicked == 2)
				{
					output.add("It's a **Money block**.");
					output.add("Too bad, you don't win anything.");
					weAreDone = true;
				}
				else if (moneyPicked == 1)
				{
					output.add("It's a **Money block**!");
					doubleMoney();
				}
				else
				{
					output.add("It's a **Money block**!");
					genericValue = giveMoney(board.get(currentPick));
					output.add(String.format("This one is worth **$%,d**!", genericValue));
					if (moneyScore != 0)
					{
						moneyScore = moneyScore + genericValue;
						String.format("This will bring your banked money up to **$%,d**!",moneyScore);
					}
				}
				moneyPicked++;
				output.add(generateBoard());
			}
			else if (board.get(currentPick) <= 29)
			{
				if (streakPicked == 2)
				{
					output.add("It's a **Streak block**.");
					output.add("Too bad, you don't win anything.");
					weAreDone = true;
				}
				else if (streakPicked == 1)
				{
					output.add("It's a **Streak block**!");
					doubleStreak();
				}
				else
				{
					output.add("It's a **Streak block**!");
					genericValue = giveStreak(board.get(currentPick));
					output.add(String.format("This one is worth **+%1$d.%2$dx**!",genericValue / 10, genericValue % 10));
					streakScore = genericValue;
				}
				streakPicked++;
				output.add(generateBoard());
			}
			else if (board.get(currentPick) <= 39)
			{
				if (boostPicked == 2)
				{
					output.add("It's a **Boost block**.");
					output.add("Too bad, you don't win anything.");
					weAreDone = true;
				}
				else if (boostPicked == 1)
				{
					output.add("It's a **Boost block**!");
					doubleBoost();
				}
				else
				{
					output.add("It's a **Boost block**!");
					genericValue = giveBoost(board.get(currentPick));
					output.add(String.format("This one is worth **+%1$d%**!",genericValue));
					boostScore = genericValue;
				}
				boostPicked++;
				output.add(generateBoard());
			}
			else if (board.get(currentPick) <= 49)
			{
				if (turnsPicked == 2)
				{
					output.add("It's an **Annuity block**.");
					output.add("Too bad, you don't win anything.");
					weAreDone = true;
				}
				else if (turnsPicked == 1)
				{
					output.add("It's an **Annuity block**!");
					doubleAnnuity();
				}
				else
				{
					output.add("It's an **Annuity block**!");
					genericValue = giveAnnuity(board.get(currentPick));
					output.add(String.format("This one is worth **%1$d** turns of $%,d annuity!",genericValue,annuityAmount));
					turnsScore = genericValue;
				}
				turnsPicked++;
				output.add(generateBoard());
			}
			else if (board.get(currentPick) <= 59)
			{
				if (chargerPicked == 2)
				{
					output.add("It's a **Charger block**.");
					output.add("Too bad, you don't win anything.");
					weAreDone = true;
				}
				else if (chargerPicked == 1)
				{
					output.add("It's a **Charger block**!");
				}
				else
				{
					output.add("It's a **Charger block**!");
					genericValue = giveCharge(board.get(currentPick));
					output.add(String.format("This one is worth **+%1$d%** per turn!",genericValue));
					chargerScore = genericValue;
				}
				chargerPicked++;
				output.add(generateBoard());
			}
			else //joker
			{
				output.add("It's a **Joker**!");
				output.add("This means we will double one of your banks, with no added Overflow risk!");
				if (moneyScore == 0 && streakScore == 0 && boostScore == 0 && turnsScore == 0 && chargerScore == 0)
				{
					output.add("But since this is your first turn, have $100,000 instead!");
					moneyScore = 100_000;
				}
				else if (moneyScore == 0 && streakScore == 0 && boostScore == 0 && turnsScore == 0)
				{
					output.add(doubleCharger());					
				}
				else if (moneyScore == 0 && streakScore == 0 && boostScore == 0 && chargerScore == 0)
				{
					output.add(doubleAnnuity());
				}
				else if (moneyScore == 0 && streakScore == 0 && turnsScore == 0 && chargerScore == 0)
				{
					output.add(doubleBoost());
				}
				else if (moneyScore == 0 && boostScore == 0 && turnsScore == 0 && chargerScore == 0)
				{
					output.add(doubleStreak());
				}
				else if (streakScore == 0 && boostScore == 0 && turnsScore == 0 && chargerScore == 0)
				{
					output.add(doubleMoney());
				}				
				else
				{
					needsDoubling = true;
					String funString = "Which of these would you like to double?";
					if (moneyScore != 0)
					{
						funString += String.format("\nMONEY (Currently **$%,d**)", moneyScore);
					}
					if (streakScore != 0)
					{
						funString += String.format("\nSTREAK (Currently **+%1$d.%2$dx**)",streakScore / 10, streakScore % 10);
					}
					if (boostScore != 0)
					{
						funString += String.format("\nBOOST (Currently **%1$d%**)",boostScore);
					}
					if (turnsScore != 0)
					{
						funString += String.format("\nANNUITY (Currently **%1$d** turns of $%,d annuity)",turnsScore,annuityAmount);
					}
					if (chargerScore != 0)
					{
						funString += String.format("\nCHARGER (Currently +**%1$d%** per turn)",chargerScore);
					}
					output.add(funString);
				}
			}
		}
		sendMessages(output);		
		if(weAreDone)
			gameOver();
		else
			getInput();
	}
		
	boolean checkValidNumber(String message)
	{
		int location = Integer.parseInt(message)-1;
		return (location >= 0 && location < BOARD_SIZE && !pickedSpaces[location]);
	}

	String generateBoard()
	{
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append("  OVERFLOW\n");
		for(int i=0; i<BOARD_SIZE; i++)
		{
			if(pickedSpaces[i])
			{
				display.append("  ");
			}
			else
			{
				display.append(String.format("%02d",(i+1)));
			}
			if(i%4 == 3)
				display.append("\n");
			else
				display.append(" ");
		}
		display.append("\n");
		String funString = "";
		if (moneyScore != 0)
		{
			funString += String.format("\nMONEY (Currently $%,d)", moneyScore);
			for(int i=0; i<moneyPicked; i++)
			{
				funString += "*";
			}
		}
		if (streakScore != 0)
		{
			funString += String.format("\nSTREAK (Currently +%1$d.%2$dx)",streakScore / 10, streakScore % 10);
			for(int i=0; i<streakPicked; i++)
			{
				funString += "*";
			}
		}
		if (boostScore != 0)
		{
			funString += String.format("\nBOOST (Currently %1$d%)",boostScore);
			for(int i=0; i<boostPicked; i++)
			{
				funString += "*";
			}
		}
		if (turnsScore != 0)
		{
			funString += String.format("\nANNUITY (Currently %1$d turns of $%,d annuity)",turnsScore,annuityAmount);
			for(int i=0; i<turnsPicked; i++)
			{
				funString += "*";
			}
		}
		if (chargerScore != 0)
		{
			funString += String.format("\nCHARGER (Currently +%1$d% per turn)",chargerScore);
			for(int i=0; i<chargerPicked; i++)
			{
				funString += "*";
			}
		}
		display.append(funString);		
		display.append("```");
		return display.toString();
	}
	
	
	@Override
	public String getBotPick()
	{
		if (needsDoubling)
		{
			if (moneyScore > 0)
			{
				return "MONEY";
			}
			else if (boostScore > 0)
			{
				return "BOOST";
			}
			else if (turnsScore > 0)
			{
				return "ANNUITY";
			}
			else if (turnsScore > 0)
			{
				return "STREAK";
			}
			else
			{
				return "CHARGER";
			}
		}
		else if ((moneyPicked == 2 || streakPicked == 2 || boostPicked == 2 || turnsPicked == 2 || chargerPicked == 2) && Math.random() < .9)
		{
			return "STOP";
		}
		else
		{
			ArrayList<Integer> openSpaces = new ArrayList<>(BOARD_SIZE);
			for (int i=0; i<BOARD_SIZE; i++)
			{
				if (!pickedSpaces[i])
				{
					openSpaces.add(i+1);
				}
			}
			return String.valueOf(openSpaces.get((int)(Math.random()*openSpaces.size())));
		}
	}

	@Override
	void abortGame()
	{
		//If they timed out they get NOTHING
		awardMoneyWon(0);
	}
	
	int giveMoney(int boardCode)
	{
		switch (boardCode)
		{
			case 11:
				return 35_000;
			case 12:
				return 50_000;
			case 13:
				return 75_000;
			case 14:
				return 100_000;
			case 15:
			default:
				return 125_000;
			
		}
	}
	
	int giveStreak(int boardCode)
	{
		switch (boardCode)
		{
			case 21:
				return 1;
			case 22:
				return 2;
			case 23:
			default:
				return 3;
			
		}
	}
	
	int giveBoost(int boardCode)
	{
		switch (boardCode)
		{
			case 31:
				return 20;
			case 32:
				return 25;
			case 33:
			default:
				return 30;
		}
	}
	
	int giveAnnuity(int boardCode)
	{
		switch (boardCode)
		{
			case 41:
				return 3;
			case 42:
				return 4;
			case 43:
			default:
				return 5;
		}
	}
	
	int giveCharge(int boardCode)
	{
		switch (boardCode)
		{
			case 51:
				return 2;
			case 52:
				return 3;
			case 53:
			default:
				return 5;
		}
	}
	
	private String doubleMoney()
	{
		moneyScore = moneyScore + moneyScore;
		return "We'll double your money bank from " + String.format("$%,d",moneyScore) 
			+ " to " + String.format("**$%,d**!",moneyScore + moneyScore);
	}
	
	private String doubleStreak()
	{
		streakScore = streakScore + streakScore;
		return String.format("We'll double your streak bank from +%1$d.%2$dx to +%3$d.%4$dx!",
				streakScore / 10, streakScore % 10, (streakScore + streakScore) / 10, (streakScore + streakScore) % 10);
	}
	
	private String doubleBoost()
	{
		boostScore = boostScore + boostScore;
		return String.format("We'll double your boost bank from %1$d% to **%2$d%**!",
				boostScore, boostScore + boostScore);
	}
	
	private String doubleAnnuity()
	{
		turnsScore = turnsScore + turnsScore;
		return String.format("We'll double your annuity bank from %1$d turns of $%3$,d per turn to **%2$d** turns!",
				turnsScore, turnsScore + turnsScore, annuityAmount);
	}
	
	private String doubleCharger()
	{
		chargerScore = chargerScore + chargerScore;
		return String.format("We'll double your Boost Charger bank from %1$d% per turn to **%2$d%**!",
				chargerScore, chargerScore + chargerScore);
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
