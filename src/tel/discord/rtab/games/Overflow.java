package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import tel.discord.rtab.Achievement;
import tel.discord.rtab.MoneyMultipliersToUse;

public class Overflow extends MiniGameWrapper {
	static final String NAME = "Overflow";
	static final String SHORT_NAME = "Flow";
	static final boolean BONUS = false;
	static final int BOARD_SIZE = 20;
	
	int moneyScore, streakScore, boostScore, turnsScore, chargerScore;
	int moneyPicked, streakPicked, boostPicked, turnsPicked, chargerPicked, jokersPicked;
	int currentPick, genericValue, roundNumber;
	int annuityAmount;
	ArrayList<Integer> board = new ArrayList<>(BOARD_SIZE);
	boolean[] pickedSpaces = new boolean[BOARD_SIZE];
	boolean lostTheGame, canQuit, needsDoubling;
	
	@Override
	void startGame()
	{
		LinkedList<String> output = new LinkedList<>();
		annuityAmount = applyBaseMultiplier(50_000);
		board.clear();
		board.addAll(Arrays.asList(11, 12, 13, 14, 15, 21, 22, 23, 31, 32, 33, 41, 42, 43, 51, 52, 53, 77, 77, 0));
			//11-15 is money (35k/50k/75k/100k/125k)
			//21-23 is streak (0.1x/0.2x/0.3x)
			//31-33 is boost (20/25/30%)
			//41-43 is turns of 10k annuity (3/4/5)
			//51-53 is boost charge (2/3/5% per turn)
			//77 is a joker and 0 is the overflow
		if(enhanced)
			board.set(0, 77);
		Collections.shuffle(board);
		pickedSpaces = new boolean[BOARD_SIZE];
		//Prep other variables
		lostTheGame = false;
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
		jokersPicked = 0;
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
		if(enhanced)
			output.add("ENHANCE BONUS: The lowest money value has been replaced with a third joker.");
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
				endGame();
				return;
			}
			else
			{
				output.add("There's no risk because you haven't picked a block yet!");
			}
		}
		else if (needsDoubling && (pick.equalsIgnoreCase("CASH") || pick.equalsIgnoreCase("MONEY") || pick.equalsIgnoreCase("BUCKS")))
		{
			sendMessage(doubleMoney());
			needsDoubling = false;
		}
		else if (needsDoubling && pick.equalsIgnoreCase("STREAK"))
		{
			sendMessage(doubleStreak());
			needsDoubling = false;
		}
		else if (needsDoubling && (pick.equalsIgnoreCase("BOOST") || pick.equalsIgnoreCase("PERCENT")))
		{
			sendMessage(doubleBoost());
			needsDoubling = false;
		}
		else if (needsDoubling && (pick.equalsIgnoreCase("ANNUITY") || pick.equalsIgnoreCase("TURNS")))
		{
			sendMessage(doubleAnnuity());
			needsDoubling = false;
		}
		else if (needsDoubling && (pick.equalsIgnoreCase("CHARGE") || pick.equalsIgnoreCase("CHARGER")))
		{
			sendMessage(doubleCharger());
			needsDoubling = false;
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
					board.get(currentPick) == 0 || board.get(currentPick) == 77)
			{
				output.add("...");
			}
			if (board.get(currentPick) == 0)
			{
				output.add("It's the **Overflow**.");
				output.add("Too bad, you don't win anything.");
				lostTheGame = true;
			}
			else if (board.get(currentPick) <= 19)
			{
				if (moneyPicked == 2)
				{
					output.add("It's your third **Money block**... and that means an Overflow.");
					output.add("Too bad, you don't win anything.");
					lostTheGame = true;
				}
				else if (moneyPicked == 1)
				{
					output.add("It's your second **Money block**!");
					output.add(doubleMoney());
				}
				else
				{
					output.add("It's your first **Money block**!");
					genericValue = giveMoney(board.get(currentPick));
					moneyScore += genericValue;
					output.add(String.format("This one is worth **$%,d**!", genericValue));
					if (moneyScore != 0)
					{
						output.add(String.format("This will bring your banked money up to **$%,d**!",moneyScore));
					}
				}
				moneyPicked++;
			}
			else if (board.get(currentPick) <= 29)
			{
				if (streakPicked == 2)
				{
					output.add("It's your third **Streak block**... and that means an Overflow.");
					output.add("Too bad, you don't win anything.");
					lostTheGame = true;
				}
				else if (streakPicked == 1)
				{
					output.add("It's your second **Streak block**!");
					output.add(doubleStreak());
				}
				else
				{
					output.add("It's your first **Streak block**!");
					genericValue = giveStreak(board.get(currentPick));
					output.add(String.format("This one is worth **+%1$d.%2$dx**!",genericValue / 10, genericValue % 10));
					streakScore = genericValue;
				}
				streakPicked++;
			}
			else if (board.get(currentPick) <= 39)
			{
				if (boostPicked == 2)
				{
					output.add("It's your third **Boost block**... and that means an Overflow.");
					output.add("Too bad, you don't win anything.");
					lostTheGame = true;
				}
				else if (boostPicked == 1)
				{
					output.add("It's your second **Boost block**!");
					output.add(doubleBoost());
				}
				else
				{
					output.add("It's your first **Boost block**!");
					genericValue = giveBoost(board.get(currentPick));
					output.add(String.format("This one is worth **+%1$d%%**!",genericValue));
					boostScore = genericValue;
				}
				boostPicked++;
			}
			else if (board.get(currentPick) <= 49)
			{
				if (turnsPicked == 2)
				{
					output.add("It's your third **Annuity block**... and that means an Overflow.");
					output.add("Too bad, you don't win anything.");
					lostTheGame = true;
				}
				else if (turnsPicked == 1)
				{
					output.add("It's your second **Annuity block**!");
					output.add(doubleAnnuity());
				}
				else
				{
					output.add("It's your first **Annuity block**!");
					genericValue = giveAnnuity(board.get(currentPick));
					output.add(String.format("This one is worth **%d** turns of $%,d annuity!",genericValue,annuityAmount));
					turnsScore = genericValue;
				}
				turnsPicked++;
			}
			else if (board.get(currentPick) <= 59)
			{
				if (chargerPicked == 2)
				{
					output.add("It's your third **Charger block**... and that means an Overflow.");
					output.add("Too bad, you don't win anything.");
					lostTheGame = true;
				}
				else if (chargerPicked == 1)
				{
					output.add("It's your second **Charger block**!");
					output.add(doubleCharger());
				}
				else
				{
					output.add("It's your first **Charger block**!");
					genericValue = giveCharge(board.get(currentPick));
					output.add(String.format("This one is worth **+%1$d%%** per turn!",genericValue));
					chargerScore = genericValue;
				}
				chargerPicked++;
			}
			else //joker
			{
				output.add("It's a **Joker**!");
				jokersPicked ++;
				output.add("This means we will double one of your banks, with no added Overflow risk!");
				if (moneyScore == 0 && streakScore == 0 && boostScore == 0 && turnsScore == 0 && chargerScore == 0)
				{
					moneyScore = applyBaseMultiplier(500_000);
					output.add("But since this is your first turn, "
							+ String.format("have $%,d instead!",moneyScore));
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
						funString += String.format("\nBOOST (Currently **%1$d%%**)",boostScore);
					}
					if (turnsScore != 0)
					{
						funString += String.format("\nANNUITY (Currently **%d** turns of $%,d annuity)",turnsScore,annuityAmount);
					}
					if (chargerScore != 0)
					{
						funString += String.format("\nCHARGER (Currently +**%1$d%%** per turn)",chargerScore);
					}
					output.add(funString);
				}
			}
		}
		if(!lostTheGame && !needsDoubling)
		{
			output.add("Pick another space to play on, or type STOP to leave with what you have.");
			output.add(generateBoard());
		}
		sendMessages(output);
		if(lostTheGame)
		{
			sendMessage(generateBoard());
			awardMoneyWon(0);
		}
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
		display.append("   OVERFLOW\n");
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
			display.append(i%5 == 4 ? "\n" : " ");
		}
		display.append("\n");
		StringBuilder funString = new StringBuilder();
		if (moneyScore != 0)
		{
			funString.append(String.format("\nMONEY (Currently $%,d)", moneyScore));
			funString.append("*".repeat(Math.max(0, moneyPicked)));
		}
		if (streakScore != 0)
		{
			funString.append(String.format("\nSTREAK (Currently +%1$d.%2$dx)", streakScore / 10, streakScore % 10));
			funString.append("*".repeat(Math.max(0, streakPicked)));
		}
		if (boostScore != 0)
		{
			funString.append(String.format("\nBOOST (Currently %1$d%%)", boostScore));
			funString.append("*".repeat(Math.max(0, boostPicked)));
		}
		if (turnsScore != 0)
		{
			funString.append(String.format("\nANNUITY (Currently %d turns of $%,d annuity)", turnsScore, annuityAmount));
			funString.append("*".repeat(Math.max(0, turnsPicked)));
		}
		if (chargerScore != 0)
		{
			funString.append(String.format("\nCHARGER (Currently +%1$d%% per turn)", chargerScore));
			funString.append("*".repeat(Math.max(0, chargerPicked)));
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
			else if (streakScore > 0)
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
		int moneyAmount = switch (boardCode) {
			case 11 -> 100_000;
			case 12 -> 200_000;
			case 13 -> 350_000;
			case 14 -> 500_000;
			case 15, default -> 750_000;
		};
		return applyBaseMultiplier(moneyAmount);
	}
	
	int giveStreak(int boardCode)
	{
		int streakAmount = switch (boardCode) {
			case 21 -> 1;
			case 22 -> 2;
			case 23, default -> 3;
		};
		return streakAmount * gameMultiplier;
	}
	
	int giveBoost(int boardCode)
	{
		int boostAmount = switch (boardCode) {
			case 31 -> 20;
			case 32 -> 25;
			case 33, default -> 30;
		};
		return boostAmount * gameMultiplier;
	}
	
	//Game multiplier is already factored into the amount itself here, so the turns aren't multiplied
	int giveAnnuity(int boardCode)
	{
		return switch (boardCode) {
			case 41 -> 3;
			case 42 -> 4;
			case 43, default -> 5;
		};
	}
	
	int giveCharge(int boardCode)
	{
		int chargeAmount = switch (boardCode) {
			case 51 -> 2;
			case 52 -> 3;
			case 53, default -> 5;
		};
		return chargeAmount * gameMultiplier;
	}
	
	private String doubleMoney()
	{
		moneyScore *= 2;
		return "We'll double your money bank from " + String.format("$%,d",moneyScore / 2) 
			+ " to " + String.format("**$%,d**!", moneyScore);
	}
	
	private String doubleStreak()
	{
		streakScore *= 2;
		return String.format("We'll double your streak bank from +%1$d.%2$dx to +%3$d.%4$dx!",
				streakScore / 20, (streakScore/2) % 10, streakScore / 10, streakScore % 10);
	}
	
	private String doubleBoost()
	{
		boostScore *= 2;
		return String.format("We'll double your boost bank from %1$d%% to **%2$d%%**!",
				boostScore / 2, boostScore);
	}
	
	private String doubleAnnuity()
	{
		turnsScore *= 2;
		return String.format("We'll double your annuity bank from %1$d turns of $%3$,d per turn to **%2$d** turns!",
				turnsScore / 2, turnsScore, annuityAmount);
	}
	
	private String doubleCharger()
	{
		chargerScore *= 2;
		return String.format("We'll double your Boost Charger bank from %1$d%% per turn to **%2$d%%**!",
				chargerScore / 2, chargerScore);
	}
	
	private void endGame()
	{
		LinkedList<String> output = new LinkedList<>();
		StringBuilder resultString = new StringBuilder();
		StringBuilder extraResult = null;
		int achievementWon = (jokersPicked == (enhanced ? 3 : 2) ? 1 : 0);
		if (getCurrentPlayer().isBot)
		{
			resultString.append(getCurrentPlayer().getName()).append(" won ");
			//* gameMultiplier
		}
		else
		{
			resultString.append("Game Over. You won ");
		}
		if (moneyScore != 0)
		{
			resultString.append(String.format("**$%,d** in cash, ",moneyScore));
			extraResult = getCurrentPlayer().addMoney(moneyScore, MoneyMultipliersToUse.BOOSTER_OR_BONUS);
			achievementWon ++;
		}
		if (turnsScore != 0)
		{
			resultString.append(String.format("**%d** turns of $%,d per turn annuity",turnsScore,annuityAmount));
			int finalAnnuityAmount = getCurrentPlayer().addAnnuity(annuityAmount,turnsScore);
			if(finalAnnuityAmount != annuityAmount)
				resultString.append(String.format(" (which gets boosted to **$%,d**)",finalAnnuityAmount));
			resultString.append(", ");
			achievementWon ++;
		}
		if (streakScore != 0)
		{
			resultString.append(String.format("**+%1$d.%2$dx** Streak bonus, ",streakScore / 10, streakScore % 10));
			getCurrentPlayer().addWinstreak(streakScore);
			achievementWon ++;
		}
		if (boostScore != 0)
		{
			resultString.append(String.format("**+%d%%** in boost, ",boostScore));
			getCurrentPlayer().addBooster(boostScore);
			achievementWon ++;
		}
		if (chargerScore != 0)
		{
			resultString.append(String.format("and **+%d%%** in boost per turn until you bomb, ",chargerScore));
			getCurrentPlayer().boostCharge = getCurrentPlayer().boostCharge + chargerScore;
			achievementWon ++;
		}
		resultString.append("from ");
		if(gameMultiplier > 1)
			resultString.append(String.format("%d copies of ",gameMultiplier));
		resultString.append(getName()).append(".");
		output.add(resultString.toString());
		if(extraResult != null)
			output.add(extraResult.toString());
		sendMessage(generateBoard());
		sendMessages = true;
		sendMessages(output);
		if(achievementWon == 6)
			Achievement.FLOW_JACKPOT.check(getCurrentPlayer());
		gameOver();
	}

	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	@Override public String getEnhanceText() { return "The lowest money amount will be replaced with a third joker."; }
}
