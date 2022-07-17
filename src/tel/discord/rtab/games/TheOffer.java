package tel.discord.rtab.games;

import java.util.LinkedList;

import tel.discord.rtab.Achievement;

public class TheOffer extends MiniGameWrapper
{
	static final String NAME = "Three Offers";
	static final String SHORT_NAME = "Offers";
	static final boolean BONUS = false;
	int chanceToBomb = 0; // Expressed as percentage
	int round = 0;
	int baseTotal = 50_000; //Initial bank
	int total; // Current bank
	int[] offer = new int[3];
	int[] ticks = new int[3];
	boolean alive = true; //Player still alive?
	
	enum OfferLabel { LOW, MEDIUM, HIGH }

	/**
	 * Initialises the variables used in the minigame and prints the starting messages.
	 */
	@Override
	void startGame()
	{
		baseTotal = applyBaseMultiplier(baseTotal);
		total = baseTotal;
		LinkedList<String> output = new LinkedList<>();
		//Give instructions
		output.add("In Three Offers, you can enter a room with a live bomb.");
		output.add("In each room, there will be three offers for how long to spend with the bomb. "
				+ "You'll earn more money for taking on a greater risk... *if* you survive.");
		output.add("Each room will feature a more volatile bomb, "
				+ "and you'll lose everything if you're still standing there when it explodes."); //~Duh
		output.add(String.format("Choose your offers wisely, and good luck! You start with **$%,d**.",total));
		if(enhanced)
			output.add("ENHANCE BONUS: The Banker sees the explode chance as 5% higher than it really is.");
		sendSkippableMessages(output);
		output.clear();
		output.add(makeOffers());
		output.add("Type the offer you want to accept, or STOP to leave with your current total.");
		sendMessages(output);
		getInput();
	}

	/**
	 * Takes the next player input and uses it to play the next "turn" - up until the next input is required.
	 * @param pick  The next input sent by the player.
	 */
	@Override
	void playNextTurn(String pick)
	{
		LinkedList<String> output = new LinkedList<>();
		String choice = pick.toUpperCase();
		choice = choice.replaceAll("\\s","");
		if("STOP".startsWith(choice) || "QUIT".startsWith(choice))
		{
			output.add("You took the money!");
			awardMoneyWon(total);
			return;
		}
		int seconds = 0;
		for(OfferLabel next : OfferLabel.values())
		{
			if(next.toString().startsWith(choice))
			{
				output.add("Going "+ next +"...");
				total += offer[next.ordinal()];
				seconds = ticks[next.ordinal()];
				break;
			}
		}
		if(seconds > 0)
		{
			output.add("The Bomb goes live!");
			//Let's find out if we explode
			for(int i=0; i<seconds; i++)
			{
				if (chanceToBomb > Math.random()*100)
				{
					output.add(String.format("Tick %d... **BOOM**",i+1));
					alive = false;
					break;
				}
				else
					output.add(String.format("Tick %d... _\\*tick*_",i+1));
			}
			sendMessages(output);
			output.clear();
			//If still alive, let's run it
			if(alive)
			{
				output.add("You survived!");
				if(seconds > 12)
					Achievement.OFFER_JACKPOT.check(getCurrentPlayer());
				output.add(makeOffers());
				sendMessages(output);
			}
			else
			{
				awardMoneyWon(0);
			}
		}
		//If it's neither of those it's just some random string we can safely ignore
		getInput();
	}

	/**
	* @return Will prepare the next offers and return the display
	**/
	private String makeOffers()
	{
		round++;
		chanceToBomb += 5;
		if(enhanced) // Enhance bonus makes the banker see the chance as higher than reality
			chanceToBomb += 5;
		//Figure out how many ticks we want each offer to be
		double tickMod = (100.0-chanceToBomb)/100;
		ticks[0] = 1; //Low offer always 1 tick
		ticks[1] = (int)((Math.random()*(4*tickMod))+2); //Med offer 2-5 ticks at first, with upper bound reducing over time
		ticks[2] = (int)((Math.random()*(4*tickMod))+ticks[1]+1); // High offer 1-4 above the med offer, also reducing over time...
		if(Math.random() < (tickMod/10)) //But occasionally boost it to a super offer
			ticks[2] += (int)(5*tickMod); // Final high offer can be anything from 3-13 ticks, though the chance drops quickly
		//Now calculate the prices for each offer
		for(int i=0; i<3; i++)
		{
			offer[i] = (int)(baseTotal * Math.pow(1+(chanceToBomb/100.0), ticks[i]*2));
			offer[i] -= offer[i] % applyBaseMultiplier(1000);
		}
		if(enhanced)
			chanceToBomb -= 5;
		//Then print it all out!
		StringBuilder output = new StringBuilder();
		output.append("```\n");
		output.append(String.format("ROOM %d\n",round));
		output.append(String.format("Bomb Explode Chance: %d%% per tick\n", chanceToBomb));
		output.append(String.format("STOP: Leave with $%,d\n\n", total));
		output.append("  Three Offers  \n");
		for(OfferLabel next : OfferLabel.values())
		{
			output.append(String.format("%s: Survive %d ticks to add $%,d\n", next.toString(), ticks[next.ordinal()], offer[next.ordinal()]));
		}
		output.append("```");
		return output.toString();
	}

	/**
	 * Returns an int containing the player's winnings, pre-booster.
	 */
	private int getMoneyWon()
	{
		return alive ? applyBaseMultiplier(total) : 0;
	}
	
	@Override
	String getBotPick()
	{
		//Do a "trial run", take the highest offer it says we'll survive
		for(int i=0; i<ticks[2]; i++)
		{
			if (chanceToBomb > Math.random()*100)
			{
				if(i < ticks[0])
					return "STOP";
				else if(i < ticks[1])
					return "LOW";
				else
					return "MEDIUM";
			}
		}
		//Trial run says we'll survive everything, so take the high offer
		return "HIGH";
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

	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	@Override public String getEnhanceText() { return "The Banker will treat the explode chance as 5% higher than it really is."; }
}
