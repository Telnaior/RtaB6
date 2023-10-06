package tel.discord.rtab.games;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static tel.discord.rtab.RaceToABillionBot.rng;

public class Optimise extends MiniGameWrapper
{
	//Note: This is "(O)ptimise Jr.", an abbreviated version with three colours.
	//The full version of (O)ptimise uses four colours and takes twenty turns rather than nine.
	//That was considered a little too long for an RtaB minigame, so we cut it down in adaptation.
	private static final String NAME = "(O)ptimise";
	private static final String SHORT_NAME = "(O)";
	private static final boolean BONUS = false;
	int[][] ladder = {{0, 5_000, 50_000, 500_000}, {0, 10_000, 100_000, 1_000_000}, {0, 20_000, 200_000, 2_000_000}};
	int[] ladderPosition = {0, 0, 0};
	int[] picksLeft = {3, 3, 3};
	int[] goldsLeft = {5, 3, 2};
	int target = 1;
	int collapses = 0;
	List<Integer> silvers = Arrays.asList(1, 2, 3, 4);
	
	private enum RGBColour { RED, GREEN, BLUE}
	private enum RevealState { NONE, FOUR, ALL}
	private enum SphereLetter { W, X, Y, Z}
	
	@Override
	void startGame()
	{
		//Give instructions, then pass over to prepareTurn()
		LinkedList<String> output = new LinkedList<>();
		output.add("(O)ptimise features three coloured money ladders, but you will only be winning one of them.");
		output.add("There are ten tickets in the barrel - 5 red, 3 green, and 2 blue. At the end of the game, "
				+ "you will draw one ticket and win that colour's value.");
		output.add(String.format("The red ladder goes up to $%,d, the green ladder $%,d, and the blue ladder $%,d!", 
				applyBaseMultiplier(ladder[0][3]), applyBaseMultiplier(ladder[1][3]), applyBaseMultiplier(ladder[2][3])));
		output.add("You will have three chances to build up each colour to its jackpot value.");
		output.add("You begin with a *target value* of 1. There are four spheres labelled W-X-Y-Z, each containing a number from 1 to 4.");
		output.add("On each turn, you pick a colour and one of the four spheres. If the number in that sphere is larger than your target, "
				+ "that money ladder moves up a step. Otherwise, you lose the turn.");
		output.add("Then, the number you picked becomes your target value for the next turn.");
		output.add("You can't have a 4 as a target, so if you find the 4 you'll need to pick one of the other spheres to become your new target.");
		output.add("Finally, after every turn we tear up one of the ten tickets at random, giving you a better idea of what you might be winning.");
		output.add("The smaller targets give you a better chance of winning the turn, so choose carefully which targets you give to which colours.");
		output.add("The game ends once each colour has been picked three times. At that point, we draw one of the remaining tickets to win!");
		output.add(String.format("Can you (O)ptimise your way to **$%,d**? Pick your first sphere and colour when you are ready.", applyBaseMultiplier(ladder[2][3])));
		if(enhanced)
		{
			goldsLeft[0] -= 2;
			goldsLeft[2] += 2;
			output.add("ENHANCE BONUS: Two red tickets have been turned blue.");
		}
		sendSkippableMessages(output);
		prepareTurn();
	}

	@Override
	void playNextTurn(String input)
	{
		if(target == 4)
		{
			repickFour(input);
			return;
		}
		String pick = input.toUpperCase();
		RGBColour chosenColour;
		SphereLetter chosenSphere;
		String[] tokens;
		//First we have to split the tokens - are there two characters without a space?
		if(pick.length() == 2)
		{
			tokens = new String[2];
			tokens[0] = pick.substring(0,1);
			tokens[1] = pick.substring(1);
		}
		else
			tokens = pick.split("\\s");
		if(tokens.length > 2)
		{
			//they're probably just saying some nonsense, return
			getInput();
			return;
		}
		if(tokens.length < 2)
		{
			sendMessage("Please choose a colour and a sphere on the same line.");
			getInput();
			return;
		}
		//If they put a sphere first, switch them
		try
		{
			SphereLetter.valueOf(tokens[0]);
			//If there's no exception, that was a valid sphere
			String temp = tokens[0];
			tokens[0] = tokens[1];
			tokens[1] = temp;
		}
		catch(IllegalArgumentException e) { /*Working as intended*/ }
		//Alright, parse what they've sent into a colour and a sphere
		switch (tokens[0]) {
			case "R", "RED" -> chosenColour = RGBColour.RED;
			case "G", "GREEN" -> chosenColour = RGBColour.GREEN;
			case "B", "BLUE" -> chosenColour = RGBColour.BLUE;
			default -> {
				sendMessage("What colour are you choosing?");
				getInput();
				return;
			}
		}
		try
		{
			chosenSphere = SphereLetter.valueOf(tokens[1]);
		}
		catch(IllegalArgumentException e)
		{
			sendMessage("What sphere are you choosing? The spheres are labelled W, X, Y, and Z.");
			getInput();
			return;
		}
		//Make sure the chosen colour still has picks left
		if(picksLeft[chosenColour.ordinal()] == 0)
		{
			sendMessage("That colour has already been chosen three times. Choose one of the other colours.");
			getInput();
			return;
		}
		//Alright, let's process their turn
		LinkedList<String> output = new LinkedList<>();
		output.add(String.format("Sphere %s selected for %s...", chosenSphere, chosenColour));
		picksLeft[chosenColour.ordinal()] --;
		int chosenNumber = silvers.get(chosenSphere.ordinal());
		output.add(String.format("It's a **%d**"+(chosenNumber>target?"!":"."),chosenNumber));
		if(chosenNumber > target)
		{
			ladderPosition[chosenColour.ordinal()] ++;
		}
		if(goldsLeft[chosenColour.ordinal()] > 0)
			output.add(String.format((chosenNumber > target ? "That moves the value of %s up to $%,d!" : "That means the value of %s stays at $%,d."), 
				chosenColour.toString().toLowerCase(), applyBaseMultiplier(ladder[chosenColour.ordinal()][ladderPosition[chosenColour.ordinal()]])));
		target = chosenNumber;
		if(isGameOver())
		{
			sendMessages(output);
			playEndGame();
			return; //We don't want to reveal the last ticket left, so go right to the final draw with the last two unknown and end the game
		}
		if(collapses != 2)
		{
			output.add("Now we throw away a ticket...");
			RGBColour goldLost = pickGoldenSphere();
			output.add(String.format("It's %s.", goldLost));
			if(goldsLeft[goldLost.ordinal()] == 0)
			{
				output.add("There are no more "+goldLost.toString().toLowerCase()+" tickets, so you cannot win that colour anymore.");
				//Double check if that collapse triggered the gameover condition
				if(isGameOver())
				{
					sendMessages(output);
					playEndGame();
					return;
				}
				//Remind them they still have its remaining picks, if there are any
				if(picksLeft[goldLost.ordinal()] > 0)
				{
					output.add("You can still use its remaining picks to try and get rid of high targets.");
				}
			}
		}
		else
			pickGoldenSphere(); //Just remove it silently when there's only one colour left, it doesn't matter
		sendMessages(output);
		if(target == 4)
		{
			sendMessage(showSilvers(RevealState.FOUR));
			sendMessage("You can't have a 4 as a target, so pick another sphere.");
			getInput();
		}
		else
		{
			sendMessage(showSilvers(RevealState.ALL));
			prepareTurn();
		}
	}
	
	void prepareTurn()
	{
		LinkedList<String> output = new LinkedList<>();
		output.add(generateBoard());
		int turnCount = 10 - picksLeft[0] - picksLeft[1] - picksLeft[2];
		output.add("It is now Turn **"+turnCount+"/9**. Your target is **"+target+"**. Pick a colour and a sphere.");
		Collections.shuffle(silvers);
		showSilvers(RevealState.NONE);
		sendMessages(output);
		getInput();
	}
	
	RGBColour pickGoldenSphere()
	{
		int totalSpheres = goldsLeft[0] + goldsLeft[1] + goldsLeft[2];
		int chosenSphere = rng.nextInt(totalSpheres);
		if(chosenSphere < goldsLeft[0])
		{
			goldsLeft[0] --;
			if(goldsLeft[0] == 0)
				collapses ++;
			return RGBColour.values()[0];
		}
		else if(chosenSphere < (goldsLeft[0] + goldsLeft[1]))
		{
			goldsLeft[1] --;
			if(goldsLeft[1] == 0)
				collapses ++;
			return RGBColour.values()[1];
		}
		else
		{
			goldsLeft[2] --;
			if(goldsLeft[2] == 0)
				collapses ++;
			return RGBColour.values()[2];
		}
	}
	
	void repickFour(String input)
	{
		if(input.length() > 1)
		{
			//If it's more than one character we don't care
			getInput();
			return;
		}
		SphereLetter chosenSphere;
		try
		{
			chosenSphere = SphereLetter.valueOf(input.toUpperCase());
		}
		catch(IllegalArgumentException e)
		{
			sendMessage("What sphere are you choosing? The spheres are labelled W, X, Y, and Z.");
			getInput();
			return;
		}
		int chosenNumber = silvers.get(chosenSphere.ordinal());
		if(chosenNumber == 4)
		{
			sendMessage("You already opened that sphere; it had the 4. Pick a different sphere.");
			sendMessage(showSilvers(RevealState.FOUR));
			getInput();
			return;
		}
		sendMessage(String.format("Sphere %s has a %d.", chosenSphere, chosenNumber));
		target = chosenNumber;
		sendMessage(showSilvers(RevealState.ALL));
		prepareTurn();
	}
	
	boolean isGameOver()
	{
		//If any colour still has picks left and tickets left
		for(int i=0; i<3; i++)
		{
			if(picksLeft[i] > 0 && goldsLeft[i] > 0)
				return false;
		}
		return true;
	}
	
	void playEndGame()
	{
		int chosenColour = -1;
		if(collapses == 2)
		{
			if(goldsLeft[0] > 0)
				chosenColour = 0;
			else if(goldsLeft[1] > 0)
				chosenColour = 1;
			else
				chosenColour = 2;
			sendMessage("That's as far as you can go, you've won the **"+RGBColour.values()[chosenColour]+"** bank!");
		}
		else
		{
			LinkedList<String> output = new LinkedList<>();
			output.add("That's the end of the game, let's draw your winning ticket...");
			output.add("...");
			RGBColour winner = pickGoldenSphere();
			output.add("**"+winner+"**!");
			chosenColour = winner.ordinal();
			sendMessages(output);
		}
		awardMoneyWon(applyBaseMultiplier(ladder[chosenColour][ladderPosition[chosenColour]]));
	}
	
	String generateBoard()
	{
		StringBuilder output = new StringBuilder();
		output.append("```\n");
		output.append("              (O)PTIMISE               \n");
		output.append("              Target = ").append(target).append("               \n\n");
		output.append("     RED         GREEN        BLUE     \n");
		for(int i=0; i<3; i++)
		{
			output.append("  ");
			output.append(picksLeft[i]);
			output.append("/3 Picks  ");
		}
		output.append("\n");
		for(int position = 3; position >= 0; position--)
		{
			for(int i=0; i<3; i++)
			{
				if(goldsLeft[i] == 0)
					output.append("             ");
				else
				{
					boolean isHere = ladderPosition[i] == position;
					output.append(isHere ? ">" : " ");
					if(ladderPosition[i] + picksLeft[i] >= position && ladderPosition[i] <= position)
						output.append(String.format("$%,9d", applyBaseMultiplier(ladder[i][position])));
					else
						output.append("          ");
					output.append(isHere ? "< " : "  ");
				}
			}
			output.append("\n");
		}
		for(int i=0; i<3; i++)
		{
			output.append("  ");
			output.append(goldsLeft[i]);
			output.append(" Ticket").append(goldsLeft[i] != 1 ? "s" : " ");
			output.append("  ");
		}
		output.append("\n```");
		return output.toString();
	}
	
	String showSilvers(RevealState reveal)
	{
		StringBuilder output = new StringBuilder();
		output.append("```\n");
		switch (reveal) {
			case NONE -> output.append("(W) (X) (Y) (Z) ");
			case FOUR -> {
				for (int i = 0; i < silvers.size(); i++) {
					if (silvers.get(i) == 4)
						output.append("(4) ");
					else
						output.append("(").append(SphereLetter.values()[i]).append(") ");
				}
			}
			case ALL -> {
				output.append(" W   X   Y   Z \n");
				for (int next : silvers)
					output.append("(").append(next).append(") ");
			}
		}
		output.append("\n```");
		return output.toString();
	}

	@Override
	String getBotPick()
	{
		RGBColour chosenColour;
		SphereLetter chosenSphere;
		if(target == 4)
		{
			//Pick a sphere at random for the repick, and if we land on the 4 then bump it to one of the others
			int ordinal = rng.nextInt(4);
			if(silvers.get(ordinal) == 4)
			{
				ordinal += rng.nextInt(1, 4);
				ordinal %= 4;
			}
			chosenSphere = SphereLetter.values()[ordinal];
			return chosenSphere.toString();
		}
		//Pick a sphere at random first
		chosenSphere = SphereLetter.values()[r.nextInt(4)];
		//Prioritise based on which sphere has the highest top value * its remaining tickets
		int[] expectedValues = new int[3];
		int options = 0;
		for(int i=0; i<3; i++)
		{
			if(picksLeft[i] == 0)
				expectedValues[i] = -1;
			else
			{
				expectedValues[i] = ladder[i][ladderPosition[i] + picksLeft[i]] * goldsLeft[i];
				options ++;
			}
		}
		//Array representing what to pick for each target
		RGBColour[] picks = new RGBColour[3];
		//Start by looking at red and green - if they're both there, assign them to 1 or 3 depending on the higher EV
		if(picksLeft[0] > 0 && picksLeft[1] > 0)
		{
			if(expectedValues[1] > expectedValues[0])
			{
				picks[0] = RGBColour.GREEN;
				picks[2] = RGBColour.RED;
			}
			else
			{
				picks[0] = RGBColour.RED;
				picks[2] = RGBColour.GREEN;
			}
			//and then to 2 based on which colour has more picks left
			picks[1] = picksLeft[1] > picksLeft[0] ? RGBColour.GREEN : RGBColour.RED;
		}
		//If we have red but not green, assign red to everything
		else if(picksLeft[0] > 0)
		{
			picks[0] = RGBColour.RED;
			picks[1] = RGBColour.RED;
			picks[2] = RGBColour.RED;
		}
		//If we have green but not red, assign green to everything
		else if(picksLeft[1] > 0)
		{
			picks[0] = RGBColour.GREEN;
			picks[1] = RGBColour.GREEN;
			picks[2] = RGBColour.GREEN;
		}
		//Then, if we have blue, figure out what to do with it based on how many total colours we have
		if(picksLeft[2] > 0)
		{
			switch (options) {
				//If no other colours have picks, assign blue to all targets
				case 1 -> {
					picks[0] = RGBColour.BLUE;
					picks[1] = RGBColour.BLUE;
					picks[2] = RGBColour.BLUE;
				}
				case 2 -> {
					int otherColour = picks[0].ordinal();
					//If two colours have picks, assign blue to 1 or 3 depending on the higher EV
					if (expectedValues[2] > expectedValues[otherColour])
						picks[0] = RGBColour.BLUE;
					else
						picks[2] = RGBColour.BLUE;
					//Then assign 2 to whichever colour has more picks left
					if (picksLeft[2] > picksLeft[otherColour])
						picks[1] = RGBColour.BLUE;
				}
				case 3 -> {
					//If everything has picks, figure out our position based on EV comparison
					if (expectedValues[2] > expectedValues[picks[0].ordinal()]) {
						picks[1] = picks[0];
						picks[0] = RGBColour.BLUE;
					} else if (expectedValues[2] < expectedValues[picks[2].ordinal()]) {
						picks[1] = picks[2];
						picks[2] = RGBColour.BLUE;
					} else
						picks[1] = RGBColour.BLUE;
				}
			}
		}
		//Then choose the colour based on our analysis and the target
		chosenColour = picks[target-1];
		return String.format("%s %s", chosenColour.toString(), chosenSphere.toString());
	}

	@Override
	void abortGame()
	{
		//Draw a ticket and give it to them immediately
		RGBColour prizeWon = pickGoldenSphere();
		awardMoneyWon(applyBaseMultiplier(ladder[prizeWon.ordinal()][ladderPosition[prizeWon.ordinal()]]));
	}

	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	@Override public String getEnhanceText() { return "Two red tickets will be turned blue."; }
}
