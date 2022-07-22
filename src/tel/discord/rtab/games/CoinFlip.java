package tel.discord.rtab.games;

import java.util.LinkedList;

public class CoinFlip extends MiniGameWrapper
{
	static final String NAME = "CoinFlip";
	static final String SHORT_NAME = "Flip";
	static final boolean BONUS = false; 
	static final int[] PAYTABLE = {10_000,25_000,50_000,100_000,250_000,500_000,1_000_000,2_500_000};
	static final int MAX_STAGE = PAYTABLE.length-1;
	int stage;
	int coins;
	boolean alive; //Player still alive?
	boolean accept; //Accepting the Offer
	/**
	 * Initialises the variables used in the minigame and prints the starting messages.
	 */
	@Override
	void startGame()
	{
		stage = 0; // We always start on Stage 0
		coins = 10;
		
		alive = true; 
		accept = false;

		LinkedList<String> output = new LinkedList<>();
		//Give instructions
		output.add("Welcome to CoinFlip!");
		output.add("Here there are "+MAX_STAGE+" stages to clear, "
				+ "and up to "+String.format("**$%,d**", payTable(MAX_STAGE))+" to be won!");
		output.add("You start with ten coins, and at each stage you choose Heads or Tails.");
		output.add("As long as even one coin shows your choice, you clear the stage.");
		output.add("However, any coins that land on the wrong side are removed from your collection.");
		output.add("You can stop at any time, but if you ever run out of coins you will lose 90% of your bank."); //~NOT DUH?!?!
		if(enhanced)
			output.add("ENHANCE BONUS: If you do run out of coins, your bailout is muliplied by how many you had when you lost.");
		output.add(ShowPaytable(stage));
		sendSkippableMessages(output);
		sendMessage(makeOverview(coins, stage));
		getInput();
	}

	/**
	 * Takes the next player input and uses it to play the next "turn" - up until the next input is required.
	 * @param pick The next input sent by the player.
	 */
	@Override
	void playNextTurn(String pick)
	{
		LinkedList<String> output = new LinkedList<>();

		boolean heads = false; //Default variable
		boolean tails = false; //Default variable

		String choice = pick.toUpperCase();
		choice = choice.replaceAll("\\s","");
        switch (choice) {
            case "HEADS":
            case "H":
            	heads = true;
            	break;
            case "TAILS":
            case "T":
            	tails = true;
            	break;
            case "ACCEPT":
            case "DEAL":
            case "TAKE":
            case "STOP":
            case "S":
                accept = true;
                output.add("You took the money!");
                int tailCoins = 0;
                int headCoins = 0;
                for (int i = 0; i < coins; i++)
                    if (Math.random() < 0.5)
                        tailCoins++;
                    else
                        headCoins++;
                output.add(String.format("Your next flip would have been %d HEADS and %d TAILS.", headCoins, tailCoins));
                break;
            case "!PAYTABLE":
                output.add(ShowPaytable(stage));
                output.add(makeOverview(coins, stage));
                break;
        }
		//If it's none of those it's just some random string we can safely ignore
		
		if(heads || tails)
		{	
			int newCoins = 0;
			stage++;
			for(int i=0; i < coins; i++)
			{
				if (Math.random() < 0.5)
				{
					if (tails) newCoins++;
				}
				else
				{
					if (heads) newCoins++;
				}
			}
			output.add(String.format("Flipping %d coin"+(coins!=1?"s":"")+"...", coins));
			if (heads)
				output.add(String.format("You got %d HEADS"+(newCoins==0?".":(coins/newCoins>=2?".":"!")), newCoins));
			else if (tails)
				output.add(String.format("You got %d TAILS"+(newCoins==0?".":(coins/newCoins>=2?".":"!")), newCoins));
			if(newCoins == 0)
			{
				alive = false;
			}
			else
			{
				coins = newCoins;
				output.add(String.format("You cleared Stage %d and won $%,d! \n", stage, payTable(stage)));
				if (stage >= MAX_STAGE) accept = true;
				else output.add(makeOverview(coins, stage));
			}
		}
		sendMessages(output);
		if(isGameOver())
			awardMoneyWon(getMoneyWon());
		else
			getInput();
		}

	/**
	* @param  stage Shows the selected Stage bold.
	* @return Will Return a nice looking Paytable with all Infos
	**/
	private String ShowPaytable(int stage)
	{
		StringBuilder output = new StringBuilder();
		output.append("```\n");
		output.append("     Win Stages    \n\n");
		for(int i=0; i<=MAX_STAGE; i++)
			output.append(String.format("Stage %1$d: $%2$,9d\n", i, payTable(i)));
		output.append("```");
		return output.toString();
	}

	/**
	* @param coins The amount of coins left
	* @param stage The current stage
	* @return Will Return a nice looking output with all Infos
	**/
	private String makeOverview(int coins, int stage)
	{
		StringBuilder output = new StringBuilder();
		output.append("```\n");
		output.append("  CoinFlip  \n\n");
		output.append("Current Coins: ").append(String.format("%d \n", coins));
		output.append("Current Stage: ").append(String.format("%d - ", stage)).append(String.format("$%,d\n", payTable(stage)));
		output.append("   Next Stage: ").append(String.format("%d - ", stage + 1)).append(String.format("$%,d\n", payTable(stage + 1)));
		output.append("Current Bailout:   ").append(String.format("$%,d\n\n", payTable(stage + 1) * (enhanced ? coins : 1) / 10));
		output.append("'Heads' or 'Tails'   (or 'Stop')? \n");
		output.append("```");
		return output.toString();
	}

	private int payTable(int stage)
	{
		//If it's a stage on the paytable, return that
		if(stage >= 0 && stage <= MAX_STAGE)
			return(applyBaseMultiplier(PAYTABLE[stage]));
		else
			return 0;
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
	 * If game isn't over yet, should return lowest possible win (usually 0) because player timed out for inactivity.
	 */
	private int getMoneyWon()
	{
		return (alive) ? payTable(stage) : payTable(stage) * (enhanced?coins:1) / 10;
	}
	
	@Override
	String getBotPick()
	{
		//Do a "trial run" and quit if it fails
		if (Math.random()*Math.pow(2,coins) > 1)
		{
			// Decide heads or tails randomly
			if (0.5 < Math.random()){
					return "TAILS";
				}
				else{
					return "HEADS";
				}
		}
		return "STOP";
	}

	@Override
	void abortGame()
	{
		//Auto-stop
		accept = true;
		awardMoneyWon(getMoneyWon());
	}

	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	@Override public String getEnhanceText() { return "If you lose, your consolation prize is multiplied by how many coins you had."; }
}