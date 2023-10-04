package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import tel.discord.rtab.Achievement;

public class FTROTS extends MiniGameWrapper
{
	static final String NAME = "For the Rest of the Season";
	static final String SHORT_NAME = "FtRotS";
	static final boolean BONUS = false;
	static final int[] TIME_LADDER = {0, 5, 10, 20, 30, 50, 75, 100, 150, 200, 250, 300, 400, 500};
	List<Integer> money = new ArrayList<>();
	List<Integer> multis = Arrays.asList(1,1,1,1,1,1,2,2,2,2,3,3,3,4,4,5,7,10);
	List<Light> lights = Arrays.asList(Light.WHITE, Light.WHITE, Light.WHITE, Light.WHITE, Light.WHITE, Light.WHITE,
									Light.WHITE, Light.WHITE, Light.WHITE, Light.WHITE, Light.GREEN, Light.GREEN, 
									Light.GREEN, Light.RED, Light.RED, Light.RED, Light.RED, Light.RED);
	boolean[] pickedSpaces = new boolean[18];
	int stage = 0;
	int lastPick;
	int total = 0;
	int whiteLightsLeft = 13;
	int redLightsLeft = 5;
	int greenLightsLeft = 3;
	int maxWhiteLights;
	int timeLadderPosition = 0;
	int checkpointPosition = 0;
	boolean canStop = false;
	
	enum Light { WHITE, RED, GREEN }
	
	@Override
	void startGame()
	{
		LinkedList<String> output = new LinkedList<>();
		//Generate money values: $500-$749, $750-$999, etc, up to $4750-$4999
		int minCash = 0, maxCash = 0;
		for(int i=0; i<18; i++)
		{
			int moneyToAdd = applyBaseMultiplier((int)(Math.random()*250) + 250*(i+2));
			money.add(moneyToAdd);
			switch(i)
			{
			case 0 -> minCash = moneyToAdd;
			case 17 -> maxCash = moneyToAdd;
			}
		}
		//Shuffle everythihg
		Collections.shuffle(money);
		Collections.shuffle(multis);
		Collections.shuffle(lights);
		//Give instructions
		output.add("For the Rest of the Season is a two-part game where your prize is an annuity.");
		output.add("In part one you will build up a small sum of money, then in part two you decide how many turns the annuity will last.");
		output.add(String.format("Our eighteen-space board is currently filled with cash ranging from $%,d to $%,d.",minCash,maxCash));
		output.add("To start building up your bank, please select two cash spaces from the board.");
		sendSkippableMessages(output);
		sendMessage(generateBoard(false));
		getInput();
	}
	
	@Override
	void playNextTurn(String pick)
	{
		LinkedList<String> output = new LinkedList<>();
		if(canStop && pick.equalsIgnoreCase("STOP"))
		{
			output.add("Congratulations, you took the money!");
			output.add(generateBoard(true));
			stage++;
		}
		else if(!isNumber(pick))
		{
			//Ignore non-number picks entirely
		}
		else if(!checkValidNumber(pick))
		{
			output.add("Invalid pick.");
		}
		else
		{
			lastPick = Integer.parseInt(pick)-1;
			pickedSpaces[lastPick] = true;
			reduceLightCount();
			//Print stuff
			output.add(String.format("Space %d selected...",(lastPick+1)));
			switch (stage) {
				case 0 -> {
					total += money.get(lastPick);
					output.add(String.format("**$%,d**!", money.get(lastPick)));
					output.add("Pick another cash space now, and we'll add that to what you just found.");
					stage++;
				}
				case 1 -> {
					total += money.get(lastPick);
					output.add(String.format("**$%,d**, for a total of $%,d!", money.get(lastPick), total));
					output.add("The next space you pick will contain a multiplier ranging from 1 to 10.");
					output.add("There's nothing to lose here, but if you hit a big number we could be on for a huge win.");
					stage++;
				}
				case 2 -> {
					output.add("...");
					total *= multis.get(lastPick);
					output.add("It's a **" + multis.get(lastPick) + "**" + (multis.get(lastPick) > 1 ? "!" : "."));
					output.add(multis.get(lastPick) > 1 ? String.format("This brings your total up to $%,d!", total)
							: String.format("This leaves your total unchanged at $%,d.", total));
					maxWhiteLights = whiteLightsLeft;
					output.add("We now take that total and move on to part two.");
					output.add("This is your time ladder, showing what you are playing for today:");
					output.add(generateTimeLadder());
					sendMessages(output);
					LinkedList<String> instructions = new LinkedList<>();
					instructions.add(String.format("On the left, you see the number of times your $%,d will be awarded - ", total)
							+ "From five times, to ten, to twenty, all the way up to **five hundred** times.");
					instructions.add("Here's how you do it. The remaining 15 spaces on the board now hide "
							+ whiteLightsLeft + " white lights and " + redLightsLeft + " red lights.");
					instructions.add("Every time you find a white light, you move up one rung on your time ladder.");
					if(enhanced && greenLightsLeft > 0)
						instructions.add("ENHANCE BONUS: "+greenLightsLeft+" white lights have turned green. "
								+ "Find them all to earn a guaranteed safe floor at your current position!");
					instructions.add("However, every time you find a red light, you move down one rung.");
					instructions.add("...and if you find *all* of the red lights, you will leave with nothing.");
					instructions.add("You can stop at any time, *unless*  the last light you found was red.");
					instructions.add("Good luck, and choose your first light when you are ready.");
					sendSkippableMessages(instructions);
					output.clear();
					//We send the result, then we send the instructions, then we send the board so we need a workaround
					//Otherwise messages get sent in the wrong order, or things that shouldn't be skipped become skippable
					stage++;
				}
				case 3 -> {
					if (redLightsLeft == 1 || timeLadderPosition >= 5)
						output.add("...");
					//Green lights only exist if they have the enhancement, so quietly hide them if we're unenhanced
					if(lights.get(lastPick) == Light.GREEN && !enhanced)
						lights.set(lastPick,Light.WHITE);
					switch(lights.get(lastPick))
					{
						case GREEN ->
						{

							timeLadderPosition++;
							canStop = true;
							output.add("It's a **GREEN** light!");
							if (whiteLightsLeft == 0)
							{
								stage++;
								output.add("Congratulations, that's as far as you can go in this game!");
							}
							else
							{
								int currentTime = getTimeValue(timeLadderPosition);
								output.add(String.format("This brings you up to %d space" + (currentTime != 1 ? "s" : "") +
										", for a total of $%,d!", currentTime, total * currentTime));
								if(greenLightsLeft == 0)
								{
									checkpointPosition = timeLadderPosition;
									output.add("And that's earned you a **CHECKPOINT**, you cannot fall below this value!");
									canStop = false;
								}
								else
									output.add("Find "+greenLightsLeft+" more to earn a checkpoint, or you can stop here.");
								output.add(generateTimeLadder());
							}
						}
						case WHITE ->
						{
							timeLadderPosition++;
							canStop = true;
							output.add("It's a **WHITE** light!");
							if (whiteLightsLeft == 0)
							{
								stage++;
								output.add("Congratulations, that's as far as you can go in this game!");
							}
							else
							{
								int currentTime = getTimeValue(timeLadderPosition);
								output.add(String.format("This brings you up to %d space" + (currentTime != 1 ? "s" : "") +
										", for a total of $%,d!", currentTime, total * currentTime));
								output.add(generateTimeLadder());
								output.add("You can stop here, or play on to find another light.");
							}
						}
						case RED ->
						{
							canStop = false;
							output.add("It's a **RED** light.");
							if (redLightsLeft == 0)
							{
								timeLadderPosition = checkpointPosition;
								stage++;
								output.add("Unfortunately, as that is the last red light, it's game over for you.");
							} 
							else
							{
								if (timeLadderPosition != 0)
								{
									if(timeLadderPosition > checkpointPosition)
										timeLadderPosition--;
									output.add("That pushes you down one rung on your time ladder, and you MUST pick again.");
								}
								else
									output.add("You don't have anything to lose yet, so pick again.");
								output.add(generateTimeLadder());
							}
						}
					}
				}
			}
			output.add(generateBoard(isGameOver()));
		}
		sendMessages(output);
		if(isGameOver())
			endGame();
		else
			getInput();
	}
	
	private boolean isGameOver()
	{
		return stage > 3;
	}
	
	void reduceLightCount()
	{
		switch(lights.get(lastPick))
		{
		case GREEN:
			greenLightsLeft --;
			//Fall through
		case WHITE:
			whiteLightsLeft --;
			break;
		case RED:
			redLightsLeft --;
			break;
		}
	}
	
	boolean checkValidNumber(String message)
	{
		int location = Integer.parseInt(message)-1;
		return (location >= 0 && location < 18 && !pickedSpaces[location]);
	}

	String generateBoard(boolean reveal)
	{
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append(" FOR THE REST OF \n   THE  SEASON   \n");
		for(int i=0; i<18; i++)
		{
			if(pickedSpaces[i])
				display.append("  ");
			else
			{
				if(reveal)
				{
					display.append(switch(lights.get(i))
					{
					case WHITE -> "Wh";
					case GREEN -> enhanced ? "Gr" : "Wh";
					case RED -> "Rd";
					});
				}
				else
					display.append(String.format("%02d",(i+1)));
			}
			if(i%6 != 5)
				display.append(" ");
			else
				display.append("\n");
		}
		display.append("```");
		return display.toString();
	}
	
	int getTimeValue(int position)
	{
		if(position == maxWhiteLights)
			return TIME_LADDER[13]; //maximum
		else
			return TIME_LADDER[Math.max(position, checkpointPosition)]; //If we have a checkpoint, everything below it counts as it
	}
	
	String generateTimeLadder()
	{
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append("     YOUR TIME LADDER     \n");
		display.append(String.format("    %2d WHITE    %2d RED    %n",whiteLightsLeft,redLightsLeft));
		display.append("==========================\n");
		int maxRungReachable = timeLadderPosition + whiteLightsLeft;
		int minRungReachable = Math.max(timeLadderPosition - redLightsLeft, checkpointPosition);
		int longestMoneyLength = 2;
		for(int i=maxRungReachable; i>=minRungReachable; i--)
		{
			int currentTime = getTimeValue(i);
			if(i==minRungReachable)
				currentTime = getTimeValue(checkpointPosition); //Force the bottom position to show a full crash
			if(timeLadderPosition == i)
				display.append("> ");
			else
				display.append("  ");
			if(currentTime > 0)
			{
				display.append(String.format("%3d SPACE", currentTime));
				if(currentTime != 1)
					display.append("S");
				else
					display.append(" ");
				//Aligning the cash totals to the right is stupidly complex lmao
				String timeTotal = String.format("$%,"+(longestMoneyLength-1)+"d", total*currentTime);
				if(longestMoneyLength == 2)
					longestMoneyLength = timeTotal.length();
				display.append(" ".repeat(Math.max(0, 25 - longestMoneyLength - 13)));
				display.append(timeTotal);
			}
			else
			{
				display.append("  NOTHING");
				for(int j=9; j<21; j++)
				{
					if(j == 22-longestMoneyLength)
						display.append("$");
					else
						display.append(" ");
				}
				display.append("0");
			}
			if(timeLadderPosition == i)
				display.append(" <");
			else
				display.append("  ");
			display.append("\n");
		}
		display.append("```");
		return display.toString();
	}
	
	@Override
	String getBotPick()
	{
		//Take a trial run, and stop if we'd hit all the remaining reds
		if(canStop)
		{
			int trialRedsLeft = redLightsLeft;
			while(Math.random()*(whiteLightsLeft+trialRedsLeft) > whiteLightsLeft)
			{
				trialRedsLeft --;
				if(trialRedsLeft == 0)
					return "STOP";
			}
		}
		ArrayList<Integer> openSpaces = new ArrayList<>(18);
		for(int i=0; i<18; i++)
			if(!pickedSpaces[i])
				openSpaces.add(i+1);
		return String.valueOf(openSpaces.get((int)(Math.random()*openSpaces.size())));
	}

	@Override
	void abortGame()
	{
		//Stop if they can, otherwise nothing sorry
		if(canStop)
			endGame();
		else
			awardMoneyWon(0);
	}
	
	private void endGame()
	{
		sendMessages = true;
		if(timeLadderPosition == 0)
		{
			//ha ha you lose now you got a big bruise
			StringBuilder resultString = new StringBuilder();
			if(getPlayer().isBot)
				resultString.append(getPlayer().getName()).append(" won ");
			else
				resultString.append("Game Over. You won ");
			resultString.append("**$0** from ");
			if(gameMultiplier > 1)
				resultString.append(String.format("%d copies of ",gameMultiplier));
			resultString.append(getName()).append(".");
			sendMessage(resultString.toString());
		}
		else
		{
			LinkedList<String> output = new LinkedList<>();
			//Add their annuity prize and grab relevant values
			int timePeriod = getTimeValue(timeLadderPosition);
			if(timeLadderPosition == TIME_LADDER.length-1)
				Achievement.FTROTS_JACKPOT.check(getPlayer());
			int boostedAmount = getPlayer().addAnnuity(total, timePeriod);
			//And then tell them what they've won
			StringBuilder resultString = new StringBuilder();
			if(getPlayer().isBot)
				resultString.append(getPlayer().getName()).append(" won ");
			else
				resultString.append("Game Over. You won ");
			resultString.append(String.format("**$%,d** from ",total));
			if(gameMultiplier > 1)
				resultString.append(String.format("%d copies of ",gameMultiplier));
			resultString.append(getName()).append("...");
			output.add(resultString.toString());
			if(boostedAmount != total)
				output.add(String.format("which gets boosted to **$%,d**...",boostedAmount));
			if(timePeriod == 1)
				output.add("to be awarded on the next space selection.");
			else
				output.add("for the next **"+timePeriod+" spaces**!");
			sendMessages(output);
			checkLuckyCharm(getPlayer(), total * timePeriod);
		}
		gameOver();
	}

	//Getters
	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	@Override public String getEnhanceText() { return "Three white lights will turn green; finding them all sets your current score as a guaranteed safe level."; }
}
