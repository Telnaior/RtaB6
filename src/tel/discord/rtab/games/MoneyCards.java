package tel.discord.rtab.games;

import java.util.Arrays;
import java.util.LinkedList;

import tel.discord.rtab.games.objs.Card;
import tel.discord.rtab.games.objs.CardRank;
import tel.discord.rtab.games.objs.CardSuit;
import tel.discord.rtab.games.objs.Deck;

public class MoneyCards extends MiniGameWrapper {
	static final String NAME = "Money Cards";
	static final String SHORT_NAME = "Cards";
    static final boolean BONUS = false;
	static final int BOARD_SIZE = 8;
    boolean isAlive;
	int score, startingMoney, addOn, minimumBet, betMultiple;
	byte stage, firstRowBust, acesLeft, deucesLeft;
	boolean canChangeCard;
	Deck deck;
	Card layout[] = new Card[BOARD_SIZE], orig1stRowEnd;
	boolean isVisible[] = new boolean[BOARD_SIZE];

	@Override
	void startGame() {
		LinkedList<String> output = new LinkedList<>();
		// initialize game variables
		isAlive = true;
		minimumBet = betMultiple = applyBaseMultiplier(5000);
		score = startingMoney = addOn = minimumBet*4;
		stage = 0;
		firstRowBust = -1; // magic number more than anything, but it matters that it's not from 0 to 7
		acesLeft = deucesLeft = (byte) CardSuit.values().length; // for foolproofing in corner cases
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
				+ "pays even money, and if the card is the same rank, your bet pushes.");
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
        if (layout[0].getRank() == CardRank.ACE)
        	acesLeft--;
        else if  (layout[0].getRank() == CardRank.DEUCE)
        	deucesLeft--;
        getInput();
	}

	@Override
	public void playNextTurn(String pick) {
		LinkedList<String> output = new LinkedList<>();
		pick = pick.toUpperCase();
		String[] tokens = pick.split("\\s");

		// Handle the "all" and "all-in" aliases
		String[] allInAliases = {"ALL", "ALL IN", "ALL-IN", "ALLIN"};
		String[] higherAliases = {"HIGHER", "HIGH", "H"};
		String[] lowerAliases = {"LOWER", "LOW", "L"};
		
		//Change is handled before everything else
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
				boolean goodChange = Math.abs(newRank.getValue(true) - 8) > Math.abs(oldRank.getValue(true) - 8);
				
				output.add("Alright then. The " + oldRank.getName() + " now becomes...");
				output.add("...a" + (newRank==CardRank.ACE
						|| newRank==CardRank.EIGHT ? "n" : "")
						+ " **" + newCard.toString() + "**" + (goodChange ? "!" : "."));
				output.add(generateBoard(false));
			}
			else
			{
				output.add("You can't change your card right now.");
			}
		}
		// Bot snark time :P
		else if (Arrays.asList(higherAliases).contains(pick) || Arrays.asList(lowerAliases).contains(pick))
		{
			output.add("You must wager something.");
		}
		else if (isNumber(pick))
		{
			output.add(String.format("Wagering $%,d on what?", Integer.parseInt(pick)));
		}
		else if (Arrays.asList(allInAliases).contains(pick))
		{
			output.add("Going all in on what?");
		}
		else if (tokens.length != 2) {
			getInput();
		}
		else if (Arrays.asList(higherAliases).contains(tokens[0])
				|| Arrays.asList(lowerAliases).contains(tokens[0]))
		{
			String temp = tokens[1];
			tokens[1] = tokens[0];
			tokens[0] = temp;
		}

		if (Arrays.asList(allInAliases).contains(tokens[0])) {
			tokens[0] = score;
		}
		if (tokens[0].charAt(tokens[0].length() - 1) == 'K')
		{
			playNextTurn(tokens[0].substring(0, tokens[0].length() - 1) + "000 " + tokens[1]);
			return;
		}
		if (!isNumber(tokens[0])
				|| !(Arrays.asList(higherAliases).contains(tokens[1])
				|| Arrays.asList(lowerAliases).contains(tokens[1])))
		{
			getInput();
		}
		else
		{			
			int bet = Integer.parseInt(tokens[0]);
			boolean betOnHigher = Arrays.asList(higherAliases).contains(tokens[1]);
			
			// Check if the bet is legal first
			if (bet > score) {
				output.add("You don't have that much money.");
				if(getCurrentPlayer().isBot)
				{
					sendMessage("AI broke");
					abortGame();
					return;
				}
			}
			else if (bet < minimumBet) {
				output.add(String.format("You must bet at least $%,d.", minimumBet));
				if(getCurrentPlayer().isBot)
				{
					sendMessage("AI broke");
					abortGame();
					return;
				}
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
				if(getCurrentPlayer().isBot)
				{
					sendMessage("AI broke");
					abortGame();
					return;
				}
			}
			
			// Foolproofing so player is not certain to lose
			// TODO: See if this code can be simplified a bit
			else if (layout[stage].getRank()==CardRank.ACE && betOnHigher) {
				output.add("There are no cards in the deck higher than an Ace.");
			}
			else if (layout[stage].getRank()==CardRank.KING && betOnHigher && acesLeft == 0) {
				output.add("There are no more cards in the deck higher than a King.");
			}
			else if (layout[stage].getRank()==CardRank.DEUCE && !betOnHigher) {
				output.add("There are no cards in the deck lower than a Deuce.");
			}
			else if (layout[stage].getRank()==CardRank.THREE && !betOnHigher && deucesLeft == 0) {
				output.add("There are no more cards in the deck lower than a Three.");
			}
			
			else {
				CardRank firstRank = layout[stage].getRank(), secondRank;
				boolean isCorrect;
				
				if ((firstRank.getValue(true) > 8 && betOnHigher)
						|| (firstRank.getValue(true) < 8 && !betOnHigher))
					output.add("Going against the odds? OK, good luck :four_leaf_clover:");
				
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
				else if (firstRank.getValue(true) != secondRank.getValue(true))
					score -= bet;

				if (secondRank == CardRank.ACE)
					acesLeft--;
				else if (secondRank == CardRank.DEUCE)
					deucesLeft--;
					
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
}
