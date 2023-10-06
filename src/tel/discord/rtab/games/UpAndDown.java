package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import static tel.discord.rtab.RaceToABillionBot.rng;

public class UpAndDown extends MiniGameWrapper {
	static final String NAME = "Up And Down";
	static final String SHORT_NAME = "Up";
	static final boolean BONUS = false;
	static final int BOARD_SIZE = 5;
	int[] dollarValues = new int[BOARD_SIZE];
	int[] dollarChange = new int[BOARD_SIZE];
	int[] curMulti = {-25, 170, 200, 260, 650};
	int[] multiChange = {10, 240, 250, 340, 1750};
	String[] alphabet = {"A", "B", "C", "D", "E"};
	ArrayList<Integer> shuffleResult = new ArrayList<>();

	int roundNum;
	int yourChoice;
	int total;
	boolean alive;
		
	@Override
	void startGame()
	{
		shuffleResult.addAll(Arrays.asList(0, 1, 2, 3, 4));
		for (int i=0; i<BOARD_SIZE; i++)
		{
			dollarValues[i] = 0;
			dollarChange[i] = 0;
			curMulti[i] = applyBaseMultiplier(curMulti[i]);
			//multiChange[i] = applyBaseMultiplier(multiChange[i]);
		}
		LinkedList<String> output = new LinkedList<>();
		alive = true;
		roundNum = 0;
		//This first one doesn't actually set up the values, just the initial multipliers
		updateValues();
		total = applyBaseMultiplier(10_000);
		//Display instructions
		output.add("In Up And Down, you can win **unlimited** money! But with that potential comes big risk, too.");
		output.add(String.format("You'll start with **$%,d**. We'll put five dollar amounts in envelopes, and shuffle them up.",total));
		output.add("After each pick, the lowest money amount will get lower, the highest money amount will get higher, and the others will change as well.");
		output.add("They will start by rising, but the further you get into the game, the lower they get, and soon all but the high value will become negative!");
		output.add("The game ends when you choose to stop, or when your bank becomes negative. In either case, you'll leave with your bank.");
		output.add("With that said, please pick envelope **A**, **B**, **C**, **D**, or **E** to begin, and **STOP** when you're satisfied!");
		roundNum++;
		updateValues();
		output.add(generateBoard());
		sendSkippableMessages(output);
		getInput();
	}
	
	@Override
	void playNextTurn(String pick)
	{
		LinkedList<String> output = new LinkedList<>();
		yourChoice = -1;
		switch (pick.toUpperCase()) {
			case "A", "1" -> {
				yourChoice = 0;
				output.add("Let's open envelope A!");
			}
			case "B", "2" -> {
				yourChoice = 1;
				output.add("Let's open envelope B!");
			}
			case "C", "3" -> {
				yourChoice = 2;
				output.add("Let's open envelope C!");
			}
			case "D", "4" -> {
				yourChoice = 3;
				output.add("Let's open envelope D!");
			}
			case "E", "5" -> {
				yourChoice = 4;
				output.add("Let's open envelope E!");
			}
			case "STOP" -> alive = false;
		}
		if(yourChoice != -1)
		{
			for (int j=0; j<4; j++)
			{
				if (total + dollarValues[j] < 0)
				{
					output.add("...");
					break;
				}
			}
			total = total + dollarValues[shuffleResult.get(yourChoice)];
			if (dollarValues[shuffleResult.get(yourChoice)] > 0)
			{
				output.add(String.format("**$%,d!**",dollarValues[shuffleResult.get(yourChoice)]));
			}
			else
			{
				output.add(String.format("**$%,d.**",dollarValues[shuffleResult.get(yourChoice)]));
			}
			if (total < 0)
			{
				output.add("Too bad, that's the end for you.");
				alive = false;
			}
			else
			{
				output.add("Let's change the values and see if you want to play another round!");
				roundNum++;
				updateValues();
				output.add(generateBoard());
				output.add("Will you pick envelope **A**, **B**, **C**, **D**, or **E**, or will you **STOP** with your bank?");
			}
			sendMessages(output);
		}
		if(!alive)
			awardMoneyWon(total);
		else
			getInput();
	}
	
	@Override
	void abortGame()
	{
		awardMoneyWon(total);
	}

	private void updateValues()
	{
		for (int j=0; j<5; j++)
		{
			dollarValues[j] += dollarChange[j];
			dollarChange[j] += 5 * curMulti[j];
			for (int k=0; k<j; k++)
			{
				if (dollarChange[k] > dollarChange[j])
				{
					dollarChange[j] = (int)(dollarChange[k] * .95);
				}
			}
			curMulti[j] = curMulti[j] + multiChange[j];
			multiChange[j] = multiChange[j] - (int)((4.95 - j) * (rng.nextInt(105, 215)));
		}
		if (curMulti[4] < 400)
		{
			curMulti[4] = 400;
		}
	}
	
	private String generateBoard()
	{
		Collections.shuffle(shuffleResult);
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append(" Up And Down \n\n");
		display.append("Round ").append(roundNum).append("\n");
		display.append(String.format("TOTAL: $%,d%nVALUES: ",total));
		for(int i=0; i<4; i++)
		{
			display.append(String.format("$%,d, ",dollarValues[i]));
		}
		display.append(String.format("$%,d%n",dollarValues[4]));
		display.append("\n");
		display.append("```");
		return display.toString();
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
	
	@Override
	String getBotPick()
	{
		boolean willStop = false;
		for (int j=0; j<4; j++)
		{
			if (total + dollarValues[j] < 0)
			{
				if (rng.nextDouble() < (.05 * roundNum))
				{
					willStop = true;
				}
			}
		}	
		if (rng.nextDouble(1_000_000) < total)
		{
			willStop = true;
		}
		if (roundNum < 4)
		{
			willStop = false;
		}		
		if (willStop)
		{
			return "STOP";
		}
		else
		{
			return alphabet[r.nextInt(5)];
		}
	}
}
