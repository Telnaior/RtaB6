package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class TriplePlay extends MiniGameWrapper
{
	static final String NAME = "Triple Play";
	static final String SHORT_NAME = "Triple";
	static final boolean BONUS = false;
	static final int AI_STOPPING_POINT = 650_000;
	List<Integer> money = Arrays.asList(1_000, 2_000, 3_000, 5_000, 7_000, 10_000, 20_000, 30_000, 50_000, 70_000,
			100_000, 150_000, 200_000, 250_000, 350_000, 500_000, 700_000, 1_000_000, 1_500_000, 2_500_000);
	boolean alive;
	boolean[] pickedSpaces;
	int lastPick;
	int lastSpace;
	int total;
	int target;
	int picksLeft;
	
	@Override
	void startGame()
	{
        money.replaceAll(this::applyBaseMultiplier);
		alive = true;
		pickedSpaces = new boolean[money.size()];
		total = 0;
		target = 0;
		lastPick = 0;
		picksLeft = 3;
		Collections.shuffle(money);
		giveInstructions();
		sendMessage(generateBoard());
		getInput();
	}
	
	private void giveInstructions()
	{
		LinkedList<String> output = new LinkedList<>();
		output.add("In Triple Play, you pick three spaces and add their values together.");
		output.add("Then, you can either leave with that total or throw it away to pick three more spaces.");
		output.add("If you play on, your previous total becomes a target that you must beat, or you will leave with nothing.");
		output.add("The biggest possible win is "+String.format("$%,d!",applyBaseMultiplier(5_000_000)));
		if(enhanced)
			output.add("ENHANCE BONUS: In the second round, you will get to pick a fourth space.");
		output.add("Best of luck! Pick your first space when you're ready.");
		sendSkippableMessages(output);
	}
	
	@Override
	void playNextTurn(String pick)
	{
		LinkedList<String> output = new LinkedList<>();
		if(pick.equalsIgnoreCase("STOP") && picksLeft == 0)
		{
			total = target;
			alive = false;
		}
		else if(!isNumber(pick))
		{
			//Definitely don't say anything for random strings
		}
		else if(!checkValidNumber(pick))
		{
			output.add("Invalid pick.");
		}
		else
		{
			//If we're starting our second set, refresh the counter
			if(picksLeft == 0)
			{
				picksLeft = enhanced ? 4 : 3;
			}
			lastSpace = Integer.parseInt(pick)-1;
			pickedSpaces[lastSpace] = true;
			total += money.get(lastSpace);
			lastPick = money.get(lastSpace);
			picksLeft --;
			//Start printing output
			output.add(String.format("Space %d selected...",lastSpace+1));
			if(picksLeft == 0)
				output.add("...");
			output.add(String.format("$%,d!",lastPick));
			if(picksLeft > 0)
			{
				output.add(String.format("You have %d pick" + (picksLeft == 1 ? "" : "s") + " left.", picksLeft));
				output.add(generateBoard());
			}
			else
			{
				if(target == 0)
				{
					target = total;
					total = 0;
					output.add(String.format("You can now choose to leave with your total of $%,d, "
							+ "or pick "+(enhanced?"four":"three")+" more spaces and try to get a higher total.",target));
					output.add("Type STOP to quit, or pick your first space if you are playing on.");
					output.add(generateBoard());
				}
				else if(total > target)
				{
					output.add("Congratulations, you beat your target!");
					alive = false;
				}
				else
				{
					output.add("Sorry, you fell short of the target.");
					total = 0;
					alive = false;
				}
			}
		}
		sendMessages(output);
		if(!alive)
		{
			sendMessage(generateRevealTable());
			awardMoneyWon(total);
		}
		else
			getInput();
	}
	
	boolean isNumber(String message)
	{
		try
		{
			Integer.parseInt(message);
			return true;
		}
		catch(NumberFormatException e1)
		{
			return false;
		}
	}
	
	boolean checkValidNumber(String message)
	{
		int location = Integer.parseInt(message)-1;
		return (location >= 0 && location < money.size() && !pickedSpaces[location]);
	}
	
	String generateBoard()
	{
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append(" TRIPLE  PLAY \n");
		for(int i=0; i<money.size(); i++)
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
		//Next display our total and last space picked
		if(picksLeft == 0)
		{
			display.append(String.format(" Total: $%,d\n",target));
		}
		else
		{
			display.append(String.format(" Total: $%,d\n",total));
			if(target > 0)
				display.append(String.format("Target: $%,d\n",target));
		}
		display.append("```");
		return display.toString();
	}
	
	String generateRevealTable()
	{
		StringBuilder display = new StringBuilder();
		display.append("```\n");			
		for(int i=0; i<20; i++)
		{
			display.append(String.format("%02d: ",(i+1)));
			display.append(String.format("$%,9d" ,money.get(i)));
				display.append(i%2==1 ? "\n" : "  ");
		}
		display.append("\n\n");
		display.append("\n```");
		return display.toString();
	}
	
	@Override
	public String getBotPick()
	{
		//We only need to check if we'll stop if 
		if(picksLeft == 0)
		{
			//Arbitrary stopping point lol
			boolean willStop = target > applyBaseMultiplier(enhanced ? AI_STOPPING_POINT*3/2 : AI_STOPPING_POINT);
			if(willStop)
				return "STOP";
		}
		//If we aren't going to stop, let's just pick our next space
		ArrayList<Integer> openSpaces = new ArrayList<>(money.size());
		for(int i=0; i<money.size(); i++)
			if(!pickedSpaces[i])
				openSpaces.add(i+1);
		return String.valueOf(openSpaces.get((int)(Math.random()*openSpaces.size())));
	}

	@Override
	void abortGame()
	{
		//Stop if we aren't behind our target, otherwise assume they lose
		if(total > target)
			awardMoneyWon(total);
		else
			awardMoneyWon(0);
	}

	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	@Override public String getEnhanceText() { return "In the second round, you can pick a fourth space."; }
}
