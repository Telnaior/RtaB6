package tel.discord.rtab.games;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class MathTime extends MiniGameWrapper {
	static final String NAME = "Math Time";
	static final String SHORT_NAME = "Math";
	static final boolean BONUS = false;
	List<Integer> money = Arrays.asList(0,10_000,25_000,50_000,75_000,100_000,150_000);
	List<String> ops1 = Arrays.asList("+","+","+","+","+","-","-");
	List<String> ops2 = Arrays.asList("x","x","x","x","/","/","/");
	List<Integer> multis = Arrays.asList(1,2,3,4,5,7,10);
	int stage = 0;
	String result2, result4;
	int lastPick;
	int total = 0;
	String equation = "";
	
	@Override
	void startGame()
	{
		LinkedList<String> output = new LinkedList<>();
		//Initialise stuff
		total = 0;
		equation = "";
		for(int i=0; i<money.size(); i++)
			money.set(i, applyBaseMultiplier(money.get(i)));
		Collections.shuffle(money);
		Collections.shuffle(ops1);
		Collections.shuffle(ops2);
		Collections.shuffle(multis);
		//Give instructions
		output.add("In Math Time, you will pick five spaces that will, together, form an equation.");
		output.add("If you pick well, you could win up to "+String.format("$%,d!",applyBaseMultiplier(3_000_000)));
		output.add("But if things go poorly you could *lose* money in this minigame, so be careful.");
		output.add("When you are ready, make your first pick from the money stage.");
		sendSkippableMessages(output);
		stage = 1;
		sendMessage(generateBoard());
		getInput();
	}
	
	@Override
	void playNextTurn(String pick) {
		LinkedList<String> output = new LinkedList<>();
		if(!isNumber(pick))
		{
			//Ignore non-number picks entirely
			getInput();
			return;
		}
		if(!checkValidNumber(pick))
		{
			output.add("Invalid pick.");
			getInput();
			return;
		}
		else
		{
			lastPick = Integer.parseInt(pick)-1;
			//Print stuff
			output.add(String.format("Space %d selected...",(lastPick+1)));
			if(stage == 5)
				output.add("...");
			switch(stage)
			{
			case 1:
			case 3:
				if(stage == 3 && result2 == "-")
					total -= money.get(lastPick);
				else
					total += money.get(lastPick);
				String result = String.format("$%,d",money.get(lastPick));
				output.add(result + "!");
				stage++;
				if(stage == 2)
					equation += "( ";
				equation += result;
				if(stage == 4)
					equation += " )";
				if(stage > 3 && total == 0)
					output.add("That equals... nothing. Sorry.");
				else
					output.add("Next, pick an operation...");
				break;
			case 2:
				result2 = ops1.get(lastPick);
				output.add("**"+result2+"**");
				output.add("Next, pick more cash...");
				equation += (" "+result2+" ");
				//Reshuffle the money so stage 3 isn't the same as stage 1
				Collections.shuffle(money);
				stage++;
				break;
			case 4:
				result4 = ops2.get(lastPick);
				output.add("**"+result4+"**");
				output.add("Finally, pick a multiplier...");
				equation += (" "+result4+" ");
				stage++;
				break;
			case 5:
				if(result4 == "/")
					total /= multis.get(lastPick);
				else
					total *= multis.get(lastPick);
				String result5 = String.format("%d",multis.get(lastPick));
				output.add(result5+"!");
				equation += result5 + " = ";
				equation += String.format("$%,d",total);
				stage++;
				break;
			}
			output.add(generateBoard());
			sendMessages(output);
			if(isGameOver())
				awardMoneyWon(getMoneyWon());
			else
				getInput();
		}
	}
	
	@Override
	void abortGame()
	{
		awardMoneyWon(getMoneyWon());
	}
	
	boolean checkValidNumber(String message)
	{
		int location = Integer.parseInt(message)-1;
		return !(location < 0 || location >= 7);
	}

	String generateBoard()
	{
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		if(stage <= 5)
		{
			display.append("    MATH    TIME    \n");
			for(int i=0; i<7; i++)
			{
				display.append(String.format("%02d",(i+1)));
				display.append(" ");	
			}
			display.append("\n\n");
		}
		display.append(equation);
		display.append("\n```");
		return display.toString();
	}
	
	boolean isGameOver()
	{
		return (stage >= 6 || (stage > 3 && total == 0));
	}

	int getMoneyWon()
	{
		if(isGameOver())
			return total;
		else return -1000000;
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
		int pick = (int) (Math.random() * 7);
		return String.valueOf(pick+1);
	}
}
