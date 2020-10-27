package tel.discord.rtab.games;

import java.util.LinkedList;

public class TheOffer extends MiniGameWrapper
{
	static final String NAME = "The Offer";
	static final String SHORT_NAME = "Offer";
	static final boolean BONUS = false;
	double chanceToBomb; 
	int offer;
	int seconds;
	boolean alive; //Player still alive?
	boolean accept; //Accepting the Offer
	
	/**
	 * Initialises the variables used in the minigame and prints the starting messages.
	 */
	@Override
	void startGame()
	{
		offer = 1000 * (int)(Math.random()*51+50); // First Offer starts between 50,000 and 100,000
		chanceToBomb = offer/10000;  // Start chance to Bomb 5-10% based on first offer
		seconds = 1;
		alive = true; 
		accept = false;

		LinkedList<String> output = new LinkedList<>();
		//Give instructions
		output.add("In The Offer, you can enter a room with a live bomb.");
		output.add("Every room you survive will earn you more money," +
				"but the chance of the bomb exploding will also increase significantly.");
		output.add("Every room will also increase the amount of chances to explode!");
		output.add("If the bomb explodes, you lose everything."); //~Duh
		sendSkippableMessages(output);
		sendMessage(makeOffer(offer, seconds, chanceToBomb));
		getInput();
	}

	/**
	 * Takes the next player input and uses it to play the next "turn" - up until the next input is required.
	 * @param  The next input sent by the player.
	 */
	@Override
	void playNextTurn(String pick)
	{
		LinkedList<String> output = new LinkedList<>();
		String choice = pick.toUpperCase();
		choice = choice.replaceAll("\\s","");
		if(choice.equals("REFUSE") || choice.equals("NODEAL") || choice.equals("DARE"))
		{
			output.add("The Bomb goes live!");
			output.add("...");
			//Let's find out if we explode
			for(int i=0; i<seconds; i++)
			{
				if (chanceToBomb > Math.random()*100)
				{
					output.add("**BOOM**");
					alive = false;
					break;
				}
				else
					output.add(String.format("You survived Tick %d!",i+1));
			}
			//If still alive, let's run it
			if(alive)
			{
				double increment = Math.random()*0.5;
				offer += (int)(offer * (1 + increment));
				offer -= offer%100;
				seconds++;
				chanceToBomb += 5 + (increment*10);
				output.add(makeOffer(offer, seconds, chanceToBomb));
			}
		}
		else if(choice.equals("ACCEPT") || choice.equals("DEAL") || choice.equals("TAKE"))
		{
			accept = true;
			output.add("You took the money!");
		}
		//If it's neither of those it's just some random string we can safely ignore
		sendMessages(output);
		if(isGameOver())
			awardMoneyWon(getMoneyWon());
		else
			getInput();
	}

	/**
	* @param offer The amount that gets offered to the Player
	* @param times The amount of times the Bomb will Tick
	* @param bomb The Chance of the Bomb going Boom per Tick
	* @return Will Return a nice looking output with all Infos
	**/
	private String makeOffer(int offer, int times, double bomb)
	{
		StringBuilder output = new StringBuilder();
		output.append("```\n");
		output.append("  The Offer  \n\n");
		output.append("Next Room:\n");
		output.append("Bomb: " + String.format ("%.2f%%\n", bomb));
		output.append("Ticks: " + String.format("%,d Times\n\n", times));
		output.append("Current Money: " + String.format("$%,d\n\n", applyBaseMultiplier(offer)));

		output.append(" 'Take' the Money  or  'Dare' the Bomb \n");
		output.append("```");
		return output.toString();
	}

	/**
	 * Returns true if the minigame has ended
	 */
	private boolean isGameOver()
	{
		return accept || !alive;
	}


	/**
	 * Returns an int containing the player's winnings, pre-booster.
	 */
	private int getMoneyWon()
	{
		return alive ? applyBaseMultiplier(offer) : 0;
	}
	
	@Override
	String getBotPick()
	{
		//Do a "trial run", quit if it fails
		for(int i=0; i<=seconds; i++)
		{
			if (chanceToBomb > Math.random()*100)
			{
				return "ACCEPT";
			}
		}
		//Trial run says we'll survive, so play on
		return "REFUSE";
	}
	
	@Override
	public String toString()
	{
		return NAME;
	}

	@Override
	void abortGame()
	{
		//Push your luck game = auto-stop
		awardMoneyWon(getMoneyWon());
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
