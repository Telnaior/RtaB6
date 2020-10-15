package tel.discord.rtab.games;

import java.util.LinkedList;

import tel.discord.rtab.games.objs.Card;
import tel.discord.rtab.games.objs.CardRank;
import tel.discord.rtab.games.objs.Deck;

public class MoneyCards extends MiniGameWrapper {
	static final String NAME = "Money Cards";
	static final String SHORT_NAME = "Cards";
    static final boolean BONUS = false;
	static final int BOARD_SIZE = 8;
    boolean isAlive;
	int score, startingMoney, addOn, minimumBet, betMultiple;
	byte stage, firstRowBust;
	boolean canChangeCard;
	Deck deck;
	Card layout[] = new Card[BOARD_SIZE], orig1stRowEnd;
	boolean isVisible[] = new boolean[BOARD_SIZE];

	@Override
	void startGame() {
		LinkedList<String> output = new LinkedList<>();
		// initialize game variables
		isAlive = true;
		score = startingMoney = addOn = applyBaseMultiplier(20000);
		minimumBet = betMultiple = startingMoney / 4;
		stage = 0;
		firstRowBust = -1; // magic number more than anything, but it matters that it's not from 0 to 7
		canChangeCard = true; 
		deck = new Deck();
		deck.shuffle(); // since the Deck object has its own shuffle method that can be called
		for (int i = 0; i < layout.length; i++) {
			layout[i] = deck.dealCard();
			isVisible[i] = i == 0;
		}
		orig1stRowEnd = layout[3];
		
		//Display instructions
        output.add("In Money Cards, you will be presented with a layout of eight cards "
				+ "from a standard 52-card deck and must bet on whether each card will "
				+ "be higher or lower than the previous card. Each correct prediction "
				+ "pays even money, and if the card is the same rank, your bet loses.");
		output.add("In this game, aces are always high and suits do not matter.");
		output.add(String.format("You start with a stake of $%,d, with ",startingMoney)
				+ "which you must bet on the three cards after your base card in the "
				+ "bottom row. To bet, type the amount you'd like to wager (without the "
				+ "dollar sign or commas) along with HIGHER or LOWER. If you would like "
				+ "to bet everything, you can type ALL-IN along with your call.");
		output.add("After you clear the bottom row, we will add another "
				+ String.format("$%,d to your bankroll for the next three cards.", addOn));
		output.add(String.format("The minimum bet is $%,d until you reach ",minimumBet)
				+ "the top card, the Big Bet. There, you must risk at least half of "
				+ String.format("your money. Bets must be in multiples of $%,d ",betMultiple)
				+ "except that you can always bet exactly half on the Big Bet.");
		output.add("If you run out of money on the bottom row, we'll move the last "
				+ "revealed card from there to the base card in the middle row and " 
				+ String.format("give you the $%,d add-on. If you run out of money ", addOn)
				+ "after the base card in the middle row, however, the game is over.");
		output.add(String.format("But if you play your cards right, you could win up to $%,d!",
				(int)Math.scalb(Math.scalb(startingMoney, 3) + addOn, 4)));
		output.add("You may change the first card in each row if you so wish. To do so, "
				+ "just type CHANGE.");
		output.add("Good luck! Your first card is a" + (layout[0].getRank()==CardRank.ACE
				|| layout[0].getRank()==CardRank.EIGHT ? "n" : "") + " **" + layout[0] + "**.");
		sendSkippableMessages(output);
        sendMessage(generateBoard(false));
        getInput();
	}

	@Override
	public void playNextTurn(String pick) {
		LinkedList<String> output = new LinkedList<>();
		
		// Handle the "all" and "all-in" aliases
		String[] aliases = {"ALL", "ALL IN", "ALL-IN", "ALLIN"};
		for (int i = 0; i < aliases.length; i++)
		{
			if (pick.equalsIgnoreCase(aliases[i] + " HIGHER") || pick.equalsIgnoreCase("HIGHER " + aliases[i]))
			{
				playNextTurn(score + " HIGHER");
				return;
			}
			else if (pick.equalsIgnoreCase(aliases[i] + " LOWER") || pick.equalsIgnoreCase("LOWER " + aliases[i]))
			{
				playNextTurn(score + " LOWER");
				return;
			}
		}
		
		if (pick.equalsIgnoreCase("CHANGE"))
		{
			if (canChangeCard)
			{
				canChangeCard = false;
				Card oldCard = layout[stage];
				CardRank oldRank = oldCard.getRank();
				changeCard();
				Card newCard = layout[stage];
				CardRank newRank = newCard.getRank();
				
				output.add("Alright then. The " + oldRank.getName() + " now becomes...");
				output.add("...a" + (newRank==CardRank.ACE
						|| newRank==CardRank.EIGHT ? "n" : "")
						+ " **" + newCard.toString() + "**.");
				output.add(generateBoard(false));
			}
			else
			{
				output.add("You can't change your card right now.");
			}
		}
		
		// Bot snark time :P
		else if (pick.equalsIgnoreCase("HIGHER") || pick.equalsIgnoreCase("LOWER"))
		{
			output.add("You must wager something.");
		}
		else if (isNumber(pick))
		{
			output.add(String.format("Wagering $%,d on what?", Integer.parseInt(pick)));
		}
		else if (pick.equalsIgnoreCase("ALL IN") || pick.equalsIgnoreCase("ALL-IN"))
		{
			output.add("Going all in on what?");
		}
		
		else
		{
			String[] tokens = pick.split("\\s");
			
			// Check to make sure it's a string we can deal with
			if (tokens.length == 2 && ((tokens[0].equalsIgnoreCase("HIGHER")
					|| tokens[0].equalsIgnoreCase("LOWER"))) && isNumber(tokens[1]))
			{
				String temp = tokens[1];
				tokens[1] = tokens[0];
				tokens[0] = temp;
			}
			
			if (tokens.length != 2 || !isNumber(tokens[0])
					|| !(tokens[1].equalsIgnoreCase("HIGHER")
					|| tokens[1].equalsIgnoreCase("LOWER")))
			{
				getInput();
				return;
			}
			
			int bet = Integer.parseInt(tokens[0]);
			boolean betOnHigher = tokens[1].equalsIgnoreCase("HIGHER");
			
			// Check if the bet is legal first
			if (bet > score) {
				output.add("You don't have that much money.");
			}
			else if (bet < minimumBet) {
				output.add(String.format("You must bet at least $%,d.", minimumBet));
			}
			else if (bet != minimumBet && bet % betMultiple != 0) {
				String message = String.format("You must bet in multiples of $%,d", betMultiple);
				/* address the special case of the minimum bet during the Big Bet
				 * not being a multiple of the original minimum bet */
				if (minimumBet % betMultiple != 0)
					message += String.format(" unless you want to make the $%,d"
							+ " minimum wager for the Big Bet", minimumBet);
				message += ".";
				output.add(message);
			}
			
			// Foolproofing so player is not certain to lose
			else if (layout[stage].getRank()==CardRank.ACE && betOnHigher) {
				output.add("There are no cards in the deck higher than an Ace.");
			}
			else if (layout[stage].getRank()==CardRank.DEUCE && !betOnHigher) {
				output.add("There are no cards in the deck lower than a Deuce.");
			}
			
			else {
				CardRank firstRank = layout[stage].getRank(), secondRank;
				boolean isCorrect;
				
				String message = String.format("Wagering $%,d that the next card is ", bet);
				
				if (betOnHigher)
					message += "higher";
				else message += "lower";
				
				message += " than a" + (firstRank==CardRank.ACE
						|| firstRank==CardRank.EIGHT ? "n" : "")
						+ " " + firstRank.getName() + "...";
				output.add(message);
				
				// Flip the card
				isVisible[stage+1] = true;
				secondRank = layout[stage+1].getRank();
				isCorrect = (firstRank.getValue(true) < secondRank.getValue(true) && betOnHigher)
						|| (firstRank.getValue(true) > secondRank.getValue(true) && !betOnHigher);
				
				output.add("...and it is a" + (secondRank==CardRank.ACE
						|| secondRank==CardRank.EIGHT ? "n" : "") + " **" + layout[stage+1].toString()
						+ "**" + (isCorrect ? "!" : "."));
				
				if (isCorrect)
					score += bet;
				else score -= bet;

				stage++;
				if (stage == layout.length-1)
					isAlive = false;
				
				if (score == 0) {
					if (stage > 3) {
						output.add("Sorry, but you have busted.");
						isAlive = false;
					} else {
						output.add("You've run out of money, but that's OK this once.");
						if (stage < 3) {
							firstRowBust = stage;
							layout[3] = layout[stage];
							isVisible[3] = true;
							stage = 3;
						}
					}
				}
				
				if (stage == 3)
					score += addOn;
				
				output.add(generateBoard(!isAlive));

				if (isAlive) {
					if (stage % 3 == 0) {
						message = "We have now moved your card up to the next row ";
						if (stage == 3) {
							message += String.format("and give you another $%,d.", addOn);
						} else { // meaning we're at the Big Bet
							minimumBet = score / 2;
							message += "for the Big Bet. You must wager at least " +
									String.format("$%,d on this last card.", minimumBet);
						}
						message += " You may CHANGE your card if you wish.";
						output.add(message);
						canChangeCard = true;
					} else {
						canChangeCard = false;
					}
				}
			}
		}
		sendMessages(output);
		if(!isAlive)
			awardMoneyWon(score);
		else
			getInput();
	}
	
	@Override
	public String getBotPick()
	{
		switch (layout[stage].getRank())
		{
			case DEUCE:
				return score + " HIGHER";
			case THREE:
			case FOUR:
			case FIVE:
				return ((score+minimumBet)/2/betMultiple*betMultiple) + " HIGHER";
			case SIX:
			case SEVEN:
				if (canChangeCard) return "CHANGE";
				else return minimumBet + " HIGHER";
			case EIGHT:
				if (canChangeCard) return "CHANGE";
				else return minimumBet + (Math.random() < 0.5 ? " HIGHER" : " LOWER");
			case NINE:
			case TEN:
				if (canChangeCard) return "CHANGE";
				else return minimumBet + " LOWER";
			case JACK:
			case QUEEN:
			case KING:
				return ((score+minimumBet)/2/betMultiple*betMultiple) + " LOWER";
			case ACE:
				return score + " LOWER";
			default:
				throw new IllegalArgumentException("Uh-oh--something's wrong with"
					+ "the bot pick for Money Cards! Tell StrangerCoug.");
		}
	}
	
	@Override
	void abortGame() {
		// Placeholder for testing; Atia didn't agree for the method to be this harsh for this game.
		awardMoneyWon(0);
	}

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

	String generateBoard(boolean fullReveal) {
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append("MONEY CARDS\n");
		display.append("$" + String.format("%,10d", score) + "\n\n");
		display.append(printBoardRow(6, 7, fullReveal));
		display.append(printBoardRow(3, 6, fullReveal));
		display.append(printBoardRow(0, 3, fullReveal));
		display.append("```");
		return display.toString();
	}
	
	private String printBoardRow(int start, int end, boolean fullReveal) {
		StringBuilder display = new StringBuilder();
		
		/* TODO: I doubt this is the most efficient way to do this. If there is a way
		 * to clean this up and still do the same thing, do so.
		 */
		for (int i = start; i <= end; i++) {
			if (i == end && (firstRowBust >= start && firstRowBust < end)) {
				if (fullReveal)
					display.append(orig1stRowEnd.toStringShort());
				else display.append("??");
			} else if (i == firstRowBust || (i == start && stage < start) ||
					(i == end && i != layout.length-1 && stage >= end)) {
				display.append("  ");
			} else if (fullReveal || isVisible[i]) {
				display.append(layout[i].toStringShort());
			} else {
				display.append("??");
			}
			display.append(" ");
		}
		display.append("\n");		
		return display.toString();
	}
	
	boolean isNumber(String message)
	{
		try
		{
			//If this doesn't throw an exception we're good
			Integer.parseInt(message);
			return true;
		}
		catch(NumberFormatException e1)
		{
			return false;
		}
	}
	
	private void changeCard() {
		layout[stage] = deck.dealCard();
	}
	
	@Override
	public String toString()
	{
		return NAME;
	}
}
