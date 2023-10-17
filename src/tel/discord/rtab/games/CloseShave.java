package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import tel.discord.rtab.Achievement;
import tel.discord.rtab.RtaBMath;

public class CloseShave extends MiniGameWrapper {
	static final String NAME = "Close Shave";
	static final String SHORT_NAME = "Shave";
	static final boolean BONUS = false;
	ArrayList<Integer> money = new ArrayList<>();
		//We add a 5 and two 4s later
	ArrayList<Integer> choices = new ArrayList<>();
	int picks = 0;
	int total = 0;
	int fives = 0;
	int lastPick;
	boolean noMoreRevealing = false;
	boolean enhanceTime = false;
	
	@Override
	void startGame()
	{
		LinkedList<String> output = new LinkedList<>();
		//Initialise stuff
		total = 0;
		money.addAll(Arrays.asList(5_000, 6_500, 7_500, 7_777, 8_000, 8_500, 9_000, 9_500, 9_750, 9_999, 10_000, 15_000, 20_000));
		money.add((int) (RtaBMath.random() * 9000) + 1000);
		money.add((int) (RtaBMath.random() * 9000) + 1000);
		money.add(1000 * ((int) (RtaBMath.random() * 11) + 10));
		Collections.shuffle(money);
		//Give instructions
		output.add("In Close Shave, the object is to get as close to $50,000 without going over. You'll see sixteen spaces, each with money.");
		output.add("You'll pick a space, and we'll add its value to your bank, but we won't show you exactly what you picked. Instead, we'll show you how many digits are in that money value.");
		output.add("There are four 5-digit values: $10,000, $15,000, $20,000, and a random value from $10,000 to $20,000.");
		output.add("There are twelve 4-digit values: $5,000, $6,500, $7,500, $7,777, $8,000, $8,500, $9,000, $9,500, $9,750, $9,999, and two random 4-digit values from $1,000 to $9,999.");
		output.add("Once you stop, we'll reveal what you picked, and see what, if anything, you win.");
		if(enhanced)
			output.add("ENHANCE BONUS: After the reveal, you will be given the option to choose one more space.");
		output.add("""
				Your final total will be multiplied as follows:
				```     $0 to $29,999:   x1
				$30,000 to $39,999:   x3
				$40,000 to $44,999:  x10
				$45,000 to $47,999:  x25
				$48,000 to $50,000:  x50```""");
		if (applyBaseMultiplier(1_000_000) != 1_000_000)
		{
			output.add("At the end, we'll multiply your winnings by the base multiplier as well, which means...");
		}
		output.add("You could win up to "+String.format("$%,d!",applyBaseMultiplier(2_500_000)));
		output.add("Of course, if your bank goes over $50,000, you win nothing.");
		output.add("When you are ready, make your first pick, and when you're satisfied, say STOP to end the game.");
		sendSkippableMessages(output);
		sendMessage(generateBoard());
		getInput();
	}
	
	@Override
	void playNextTurn(String pick)
	{
		LinkedList<String> output = new LinkedList<>();
		if(pick.equalsIgnoreCase("STOP"))
		{
			if(enhanceTime)
			{
				output.add("Then that's all! Congratulations!");
				output.addAll(congratulateWinner());
			}
			else if (picks == 0)
			{
				output.add("You haven't picked any spaces yet! There's no risk yet, so go ahead and pick one!");
			}
			else
			{
				output.add("You have chosen to stop. Hopefully your bank is close to $50,000, and not over!");
				output.addAll(revealSpaces());
			}
		}
		else if(!isNumber(pick))
		{
			//.'s count as non-numbers, so don't use them
			getInput();
			return;
		}
		else if(!checkValidNumber(pick))
		{
			sendMessage("Invalid pick.");
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
			if(enhanceTime)
			{
				output.add("...");
				total += money.get(lastPick);
				output.add(String.format("**$%,d**",money.get(lastPick)) + (total>50_000 ? "." : "!"));
				if (total > 50_000)
				{
					output.add("Too bad, you went over $50,000, so you win nothing.");
					output.add(generateFinalBoard());
					total = 0;
					noMoreRevealing = true;
				}
				else
				{
					output.add(String.format("Your bank is now **$%,d**.",total));
					output.add("And that's all! Congratulations!");
					output.addAll(congratulateWinner());
				}
			}
			else
			{
				int cashLen = Integer.toString(money.get(lastPick)).length();
				output.add(String.format("It's a %d-digit amount.",(cashLen)));
				if (cashLen == 5)
				{
					fives++;
				}
				if(picks == money.size())
				{
					output.add("Picking all the spaces? That's a bold strategy Cotton, let's see if it pays off for them.");
					output.addAll(revealSpaces());
				}
				else
				{
					output.add("Pick another number to continue, or say STOP to end the game.");
					output.add(generateBoard());
				}
			}
		}
		sendMessages(output);
		if(noMoreRevealing)
			awardMoneyWon(total);
		else
			getInput();
	}
	
	LinkedList<String> revealSpaces()
	{
		LinkedList<String> output = new LinkedList<>();
		for (int i=1; i<=picks; i++)
		{
			output.add("Pick number " + i + ", space #" + (choices.get(i-1)+1) + ", was " 
					+ Integer.toString(money.get(choices.get(i-1))).length() + " digits long, and it was...");
			if (total > 30_000)
			{
				output.add("...");
			}
			total += money.get(choices.get(i-1));
			output.add(String.format("**$%,d**",money.get(choices.get(i-1))) + (total>50_000 ? "." : "!"));
			if (total > 50_000)
			{
				output.add("Too bad, you went over $50,000, so you win nothing.");
				output.add(generateFinalBoard());
				total = 0;
				noMoreRevealing = true;
				break;
			}
			else
			{
				output.add(String.format("Your bank is now **$%,d**.",total));
				if (i == picks && enhanced)
				{
					enhanceTime = true;
					output.add("Would you like to pick one more space? Pick again if so, or type STOP to end the game.");
					output.add(generateBoard());
				}
				else if (i == picks && !enhanced)
				{
					output.add("And that's all! Congratulations!");
					output.addAll(congratulateWinner());
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
		return output;
	}
	
	LinkedList<String> congratulateWinner()
	{
		LinkedList<String> output = new LinkedList<>();
		output.add(generateFinalBoard());
		if (total < 30_000)
		{
			output.add(String.format("You'll keep your bank of **$%,d**.",total));
		}
		else if (total >= 30_000 && total <= 39_999)
		{
			total *= 3;
			output.add(String.format("We'll multiply your bank by 3; it becomes **$%,d**.",total));
		}
		else if (total >= 40_000 && total <= 44_999)
		{
			total *= 10;
			output.add(String.format("We'll multiply your bank by 10; it becomes **$%,d**!",total));
		}
		else if (total >= 45_000 && total <= 47_999)
		{
			total *= 25;
			output.add(String.format("We'll multiply your bank by 25; it becomes **$%,d**!",total));
		}
		else if (total >= 48_000 && total <= 50_000)
		{
			total *= 50;
			output.add(String.format("We'll multiply your bank by 50! That means it becomes **$%,d**!",total));
			Achievement.SHAVE_JACKPOT.check(getPlayer());
		}
		if (applyBaseMultiplier(1_000_000) != 1_000_000)
		{
			total = applyBaseMultiplier(total);
			output.add(String.format("Finally, we'll apply the base multiplier, which means your final total is **$%,d**!",total));
		}	
		noMoreRevealing = true;
		return output;
	}
	
	@Override
	void abortGame()
	{
		if(picks == 0)
			awardMoneyWon(0);
		else
			playNextTurn("STOP");
	}
	
	boolean checkValidNumber(String message)
	{
		int location = Integer.parseInt(message)-1;
		return location >= 0 && location < 16 && !choices.contains(location);
	}

	String generateFinalBoard()
	{
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append("CLOSE SHAVE\n");			
		for(int i=0; i<16; i++)
		{
			display.append(String.format("%02d: ",(i+1)));
			display.append(String.format("$%,6d" ,money.get(i)));
				display.append(i%2==1 ? "\n" : "  ");
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
			if (!choices.contains(i))
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
	
	@Override
	String getBotPick()
	{
		if(enhanceTime)
		{
			//special handling for final pick of an enhanced game - take a trial run and stop if we'd bust
			ArrayList<Integer> openSpaces = new ArrayList<>(16);
			for(int i=0; i<16; i++)
				if(!choices.contains(i))
					openSpaces.add(i+1);
			if(money.get((int)(RtaBMath.random()*openSpaces.size())) + total > 50_000)
				return "STOP";
			else
				return String.valueOf(openSpaces.get((int)(RtaBMath.random()*openSpaces.size())));
		}
		else if ((picks + fives == 5 && (RtaBMath.random() < .5 || (enhanced && RtaBMath.random() < .5)))
				|| (picks + fives == 6 && (RtaBMath.random() < .9 || enhanced))
				|| (picks + fives >= 7))
		{
			return "STOP";
		}
		else
		{
			ArrayList<Integer> openSpaces = new ArrayList<>(16);
			for(int i=0; i<16; i++)
				if(!choices.contains(i))
					openSpaces.add(i+1);
			return String.valueOf(openSpaces.get((int)(RtaBMath.random()*openSpaces.size())));
		}
	}

	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	@Override public String getEnhanceText() { return "After the final reveal, you will be given the option to choose one more space."; }
}
