package tel.discord.rtab.games;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class CloseShave extends MiniGameWrapper {
	static final String NAME = "Close Shave";
	static final String SHORT_NAME = "Shave";
	static final boolean BONUS = false;
	List<Integer> money = Arrays.asList(5_000, 6_500, 7_500, 7_777, 8_000, 8_500, 9_000, 9_500, 9_750, 9_999, 10_000, 15_000, 20_000);
		//We add a 5 and two 4s later
	List<Integer> choices = Arrays.asList();
	int picks = 0;
	int total = 0;
	int fives = 0;
	int lastPick;
	boolean noMoreRevealing = false;
	
	@Override
	void startGame()
	{
		LinkedList<String> output = new LinkedList<>();
		//Initialise stuff
		total = 0;
		equation = "";
		money.add((int) ((Math.random() * 9000)) + 1000);
		money.add((int) ((Math.random() * 9000)) + 1000);
		money.add(1000 * ((int) (Math.random() * 11) + 10));
		Collections.shuffle(money);
		//Give instructions
		output.add("In Close Shave, the object is to get as close to $50,000 without going over. You'll see sixteen spaces, each with money.");
		output.add("You'll pick a space, and we'll add its value to your bank, but we won't show you exactly what you picked. Instead, we'll show you how many digits are in that money value.");
		output.add("There are four 5-digit values: $10,000, $15,000, $20,000, and a random value from $10,000 to $20,000.");
		output.add("There are twelve 4-digit values: $5,000, $6,500, $7,500, $7,777, $8,000, $8,500, $9,000, $9,500, $9,750, $9,999, and two random 4-digit values from $1,000 to $9,999.");
		output.add("Once you stop, we'll reveal what you picked, and see what, if anything, you win.");
		output.add("The pre-base multiplier payout window is as follows:\n```$0 to $29,999: x1\n$30,000 to $39,999: x2\n$40,000 to $44,999: x3\n$45,000 to $47,999: x5\n$48,000 to $50,000: x20```");
		//output.add("If you stopped under $30,000, you win your bank, and that's it. If you stopped from $30,000 to $39,999, your bank doubles.");
		//output.add("If you stopped from $40,000 to $44,999, your bank is multiplied by 3. If you stopped from $45,000 to $47,999, your bank is multiplied by 5!");
		//output.add("And if you stopped from $48,000 to $50,000, your bank is multiplied by 20!");
		if (applyBaseMultiplier(1_000_000) != 1_000_000)
		{
			output.add("At the end, we'll multiply your winnings by the base multiplier as well, which means...");
		}
		output.add("You could win up to "+String.format("$%,d!",applyBaseMultiplier(1_000_000)));
		output.add("Of course, if your bank goes over $50,000, you win nothing.");
		output.add("When you are ready, make your first pick, and when you're satisfied, say STOP to end the game.");
		sendSkippableMessages(output);
		sendMessage(generateBoard());
		getInput();
	}
	
	@Override
	void playNextTurn(String pick) {
		LinkedList<String> output = new LinkedList<>();
		if(pick == "STOP")
		{
			if (picks == 0)
			{
				output.add("You haven't picked any spaces yet! There's no risk yet, so go ahead and pick one!");
			}
			else
			{
				output.add("You have chosen to stop. Hopefully your bank is close to $50,000, and not over!");
				for (int i=1; i <= picks; i++)
				{
					output.add("Pick number " + i + ", space number " + choices.get(i) + ", was " + ((String) (money.get(i - 1))).length + " digits long, and it was...");
					if (total > 30_000)
					{
						output.add("...");
					}
					total = total + money.get(i - 1);
					if (total > 50_000)
					{
						output.add(String.format("**$%,d**.",money.get(i - 1)));
						i = picks;
						output.add("Too bad, you went over $50,000, so you win nothing.");
						generateFinalBoard();
						total = 0;
						noMoreRevealing = true;
					}
					else
					{
						output.add(String.format("**$%,d**!",money.get(i - 1)));
						output.add(String.format("Your bank is now **$%,d**.",total));
						if (i == picks)
						{
							output.add("And that's all! Congratulations...");
							generateFinalBoard();
							noMoreRevealing = true;
						}
						else if (picks - i == 1)
						{
							output.add("One more pick, let's see what it is.");
						}
						else
						{
							output.add("There are " + (picks - i) + " picks left to reveal. Next one...");
						}
						
					}
					
				}
			}
		}
		else if(!isNumber(pick))
		{
			//.'s count as non-numbers, so don't use them
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
			lastPick = Integer.parseInt(pick) - 1;
			picks++;
			choices.add(lastPick);
			//Print stuff
			output.add(String.format("Space %d selected...",(lastPick+1)));
			var cashLen = ((String) (money.get(lastPick))).length;
			output.add(String.format("It's a %d-digit amount.",(cashLen)));
			if (cashLen == 5)
			{
				fives++;
			}
			output.add(generateBoard());
			output.add("Pick another number to continue, or say STOP to end the game.");
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
		return !(location < 0 || location >= 16);
	}

	String generateFinalBoard()
	{
		//return "Doesn't do anything yet, but the game should be over now, so nyah";		
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append("CLOSE SHAVE \n");			
		for(int i=0; i<16; i++)
		{
			display.append(String.format("%02d: ",(i+1)));
			display.append(String.format("$%,d" ,money.get(i)));
			if (i % 2 == 1)
			{
				display.append("\n");
			}
		}
		display.append("\n\n");
		display.append("\n```");
		return display.toString();
	}
	
	String generateBoard()
	{
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append("CLOSE SHAVE \n");			
		for(int i=0; i<16; i++)
		{
			if (choices.contains(i))
			{
				display.append(String.format("%02d ",(i+1)));
			}
			else
			{
				display.append("   ");
			}
			if (i % 4 == 3)
			{
				display.append("\n");
			}
		}
		display.append("\n\n");
		display.append("\n```");
		return display.toString();
	}
	
	boolean isGameOver()
	{
		return noMoreRevealing;
	}

	int getMoneyWon()
	{
		if(isGameOver())
			return total;
		else return 0_000_000_000_000;
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
		int pick = (int) (Math.random() * 16);
		if (Math.random() < .3 || fives == 2)
		{
			return "STOP";
		}
		else
		{
			return String.valueOf(pick+1);
		}
	}
}
