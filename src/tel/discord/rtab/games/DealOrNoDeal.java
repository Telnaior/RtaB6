	package tel.discord.rtab.games;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import tel.discord.rtab.Achievement;

public class DealOrNoDeal extends MiniGameWrapper
{
	static final String NAME = "Deal or No Deal";
	static final String SHORT_NAME = "Deal";
	static final boolean BONUS = false;
	List<Integer> VALUE_LIST = Arrays.asList(1,10,50,100,250,500,750,1_000,2_500,5_000,7_500, //Blues
			10_000,30_000,50_000,100_000,200_000,350_000,500_000,750_000,1_000_000,2_000_000,5_000_000); //Reds
	List<Integer> VALUE_LIST_ENHANCED = Arrays.asList(1,10,50,100,250,500,750,1_000,2_500,5_000,7_500, //Blues
			10_000,30_000,50_000,100_000,200_000,500_000,750_000,1_000_000,2_000_000,3_500_000,5_000_000); //Reds
	LinkedList<Integer> values = new LinkedList<>();
	int offer;
	int prizeWon;
	int casesLeft;
	int moneyLength;
	boolean accept; //Accepting the Offer

	@Override
	void startGame()
	{
		casesLeft = VALUE_LIST.size();
		offer = 0;
		accept = false;
		//Enhanced players get the buffed board
		if(enhanced)
			VALUE_LIST = VALUE_LIST_ENHANCED;
		//Multiply each value, EXCEPT the $1, by the base multiplier
		for(int i = 1; i < VALUE_LIST.size(); i++)
		{
			VALUE_LIST.set(i, applyBaseMultiplier(VALUE_LIST.get(i)));
		}
		//Get the length of the biggest cash value for board display purposes
		moneyLength = String.format("%,d", VALUE_LIST.get(VALUE_LIST.size()-1)).length();
		//Load up the boxes and shuffle them
		values.clear();
		values.addAll(VALUE_LIST);
		Collections.shuffle(values);
		//Give instructions
		LinkedList<String> output = new LinkedList<>();
		output.add("In Deal or No Deal, there are 22 boxes, "
				+ String.format("each holding an amount of money from $1 to $%,d.",applyBaseMultiplier(5_000_000)));
		output.add("One of these boxes is 'yours', and if you refuse all the offers you win the contents of that box.");
		output.add("We open the other boxes one by one to find out which values *aren't* in your own box.");
		output.add("The first offer comes after five boxes are opened, after which offers are received every three boxes.");
		output.add("If you take an offer at any time, you win that amount instead of the contents of the final box.");
		if(enhanced)
			output.add(String.format("ENHANCE BONUS: The $%,d box has been replaced with $%,d.",
					applyBaseMultiplier(350_000),applyBaseMultiplier(3_500_000)));
		output.add("Best of luck, let's start the game...");
		sendSkippableMessages(output);
		output.clear();
		output.add(generateBoard());
		output.add("Opening five boxes...");
		for(int i=0; i<5; i++)
			output.add(openBox());
		output.add("...");
		output.add(generateOffer());
		output.add("Deal or No Deal?");
		output.add(generateBoard());
		sendMessages(output);
		getInput();
	}

	private String openBox()
	{
		casesLeft --;
		return String.format("$%,d!",values.pollFirst());
	}

	@Override
	void playNextTurn(String pick)
	{
		LinkedList<String> output = new LinkedList<>();
		String choice = pick.toUpperCase();
		choice = choice.replaceAll("\\s","");
		if(choice.equals("REFUSE") || choice.equals("NODEAL") || choice.equals("ND"))
		{
			output.add("NO DEAL!");
			if(casesLeft == 2)
			{
				output.add("Your box contains...");
				prizeWon = values.pollLast();
				output.add(String.format("$%,d!",prizeWon));
				accept = true;
			}
			else
			{
				output.add("Opening three boxes...");
				for(int i=0; i<3; i++)
					output.add(openBox());
				output.add("...");
				output.add(generateOffer());
				output.add("Deal or No Deal?");
				output.add(generateBoard());
			}
			sendMessages(output);
		}
		else if(choice.equals("ACCEPT") || choice.equals("DEAL") || choice.equals("D"))
		{
			accept = true;
			prizeWon = offer;
			output.add("It's a DONE DEAL!");
			output.add("Now for the proveout... (you can !skip this)");
			sendMessages(output);
			sendSkippableMessages(runProveout());
		}
		if(accept)
			awardMoneyWon(prizeWon);
		else
			getInput();
	}

	private String generateOffer()
	{
		SecureRandom r = new SecureRandom();
		//Generate "fair deal" and average
		int fairDeal = 0;
		int average = 0;
		for(int i : values)
		{
			fairDeal += Math.sqrt(i);
			average += i;
		}
		fairDeal /= casesLeft;
		average /= casesLeft;
		fairDeal = (int)Math.pow(fairDeal,2);
		//Check for dream finish achievement
		if(casesLeft == 2 && average >= applyBaseMultiplier((VALUE_LIST.get(20)+VALUE_LIST.get(21))/2) && !accept)
			Achievement.DEAL_JACKPOT.check(getPlayer());
		//Use the fair deal as the base of the offer, then add a portion of the average to it depending on round
		offer = fairDeal + ((average-fairDeal) * (20-casesLeft) / 40);
		//Add random factor: 0.90-1.10
		int multiplier = r.nextInt(21) + 90;
		offer *= multiplier;
		offer /= 100;
		//Round it off
		if(offer > 250000)
			offer -= (offer%10000);
		else if(offer > 25000)
			offer -= (offer%1000);
		else if(offer > 2500)
			offer -= (offer%100);
		else if(offer > 250)
			offer -= (offer%10);
		//And format the result they want to see
		return String.format("BANK OFFER: $%,d",offer);
	}

	private String generateBoard() {
		StringBuilder output = new StringBuilder();
		output.append("```\n");
		//Header
		output.append("    DEAL OR NO DEAL    \n");
		if(offer > 0)
			output.append(String.format("   OFFER: $%,"+moneyLength+"d   \n",offer));
		output.append("\n");
		//Main board
		int nextValue = 0;
		for(int i=0; i<VALUE_LIST.size(); i++)
		{
			if(values.contains(VALUE_LIST.get(nextValue)))
			{
				output.append(String.format("$%,"+moneyLength+"d",VALUE_LIST.get(nextValue)));
			}
			else
			{
				output.append(" ".repeat(Math.max(0, (moneyLength + 1))));
			}
			//Order is 0, 11, 1, 12, ..., 9, 20, 10, 21
			nextValue += VALUE_LIST.size()/2;
			if(nextValue >= VALUE_LIST.size())
				nextValue -= VALUE_LIST.size() - 1;
			//Space appropriately
			output.append(i%2==0 ? "   " : "\n");
		}
		output.append("```");
		return output.toString();
	}
	
	private LinkedList<String> runProveout()
	{
		LinkedList<String> output = new LinkedList<>();
		while(casesLeft > 2)
		{
			StringBuilder boxesOpened = new StringBuilder();
			for(int i=0; i<3; i++)
				boxesOpened.append(openBox()).append(" ");
			output.add(boxesOpened.toString());
			generateOffer();
			output.add(generateBoard());
		}
		output.add("Your box contained...");
		output.add(String.format("$%,d!",values.pollLast()));
		return output;
	}

	@Override
	String getBotPick() {
		SecureRandom r = new SecureRandom();
		//Chance to deal is based on how deep into the game we are and how big the offer is
		double casesSquare = Math.pow(22-casesLeft,2); //Ranges from 25 on first offer to 400 on last offer
		double offerMagnitude = Math.log10(offer) / Math.log10(applyBaseMultiplier(2_500_000)); //Ranges from 0-1
		double dealChance = casesSquare * offerMagnitude / 500;
		return (r.nextDouble() < dealChance) ? "DEAL" : "NO DEAL";
	}

	@Override
	void abortGame()
	{
		//Take the deal
		awardMoneyWon(offer);
	}

	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	@Override public String getEnhanceText() { return "The box containing 7% of the jackpot will have its value multiplied by 10 (to 70%)."; }
}
