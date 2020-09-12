package tel.discord.rtab.games;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import net.dv8tion.jda.api.entities.ChannelType;

public class TestGame extends MiniGameWrapper
{
	@Override
	void startGame()
	{
		//Send instructions
		LinkedList<String> output = getInstructions();
		sendSkippableMessages(output);
		//Then display the board and get input
		sendMessage("Awesome! Now you know how to play, here's the board:");
		sendMessage(generateBoard());
		getInput();
	}

	@Override
	void playNextTurn(String input)
	{
		if(!isNumber(input))
		{
			//Imagine a world where you say something for random strings
			//This is not that world
			getInput();
			return;
		}
		if(!isValidNumber(input))
		{
			sendMessage("That number is not on the board.");
			getInput();
			return;
		}
		//Yay they did it!
		LinkedList<String> output = new LinkedList<String>();
		output.add("Space 1 selected...");
		output.add(String.format("Congratulations, it's **$%,d**!",applyBaseMultiplier(100_000)));
		sendMessages(output);
		awardMoneyWon(applyBaseMultiplier(100_000));
	}
	
	boolean isValidNumber(String input)
	{
		//First make sure it's a number
		if(!isNumber(input))
			return false;
		//Okay let's get it
		int number = Integer.parseInt(input);
		//The number is valid if it is 1.
		return number == 1;
	}
	
	String generateBoard()
	{
		//Totes complex board generation
		return "```\nTEST\nGAME\n 01 \n```";
	}

	@Override
	String getBotPick()
	{
		//The AI just picks the space and wins
		return "1";
	}

	@Override
	void abortGame()
	{
		sendMessage("Seriously? You timed out? Well, I guess you get nothing.");
	}
	
	//Getters
	@Override
	public String getName()
	{
		return "Test Game";
	}
	@Override
	public String getShortName()
	{
		return "test";
	}
	@Override
	public boolean isBonus()
	{
		return false;
	}
	
	//Putting this at the bottom so it doesn't make it impossible to scroll through
	LinkedList<String> getInstructions()
	{
		LinkedList<String> output = new LinkedList<String>();
		output.add("Test Game is very easy.");
		output.add("Just pick a space on the board, and you win what's behind it!");
		if(channel.getType() != ChannelType.PRIVATE)
		{
			output.add("Oh, but a bunch of garbage text got mixed in with these instructions.");
			output.add("You should probably type !skip to skip them, or you could be here for a while.");
			//Read the rest of the instructions out of a file
			try
			{
				List<String> list = Files.readAllLines(Paths.get("TestGameInstructions.txt"));
				for(String next : list)
					output.add(next);
			}
			catch (IOException e)
			{
				output.add("Oops, we lost the rest of the instructions.");
			}
			//PLEASE tell me we never reach this.
			output.add("...");
			output.add("Why are you still reading this?");
			output.add("Fine, so you didn't skip. Let's just move on.");
		}
		return output;
	}
}
