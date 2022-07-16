package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import tel.discord.rtab.Achievement;
import tel.discord.rtab.games.objs.Card;
import tel.discord.rtab.games.objs.CardRank;
import tel.discord.rtab.games.objs.CardSuit;
import tel.discord.rtab.games.objs.Deck;
import tel.discord.rtab.games.objs.PokerHand;

public class DeucesWild extends MiniGameWrapper
{
	static final String NAME = "Deuces Wild";
	static final String SHORT_NAME = "Deuces";
	static final boolean BONUS = false;
	static final int BOARD_SIZE = 52;
	Deck deck;
	Card[] cardsPicked = new Card[5];
	boolean[] cardsHeld = new boolean[5];
	ArrayList<Card> board = new ArrayList<Card>(BOARD_SIZE);
	int lastSpace;
	Card lastPicked;
	PokerHand hand = PokerHand.NOTHING;
	boolean[] pickedSpaces = new boolean[BOARD_SIZE];
	boolean redrawUsed;
	boolean enhancedRedraw;
	byte gameStage;

	@Override
	void startGame()
	{
		LinkedList<String> output = new LinkedList<>();
		//Initialise board
		board.clear();
		deck = new Deck();
		deck.shuffle(); // since the Deck object has its own shuffle method that can be called
		for(int i=0; i<BOARD_SIZE; i++)
				board.add(deck.dealCard());
		cardsPicked = new Card[5];
		cardsHeld = new boolean[5];
		pickedSpaces = new boolean[BOARD_SIZE];
		redrawUsed = false;
		enhancedRedraw = enhanced;
		gameStage = 0;
		//Display instructions
		output.add("In Deuces Wild, your objective is to obtain the best poker hand possible.");
		output.add("We have shuffled a standard deck of 52 playing cards, from which you will pick five cards.");
		output.add("As the name of the game suggests, deuces (twos) are wild. "
				+ "Those are always treated as the best card possible.");
		output.add("After you draw your five cards, you may redraw as many of them as you wish, but only once.");
		if(enhanced)
			output.add("ENHANCE BONUS: You will get two opportunities to redraw cards, not just one.");
		output.add(String.format("You must get at least a pair to win any money. That pays $%,d, but if it's at least a pair of jacks, " 
				+ "we'll increase it to $%,d.",getMoneyWon(PokerHand.ONE_PAIR), getMoneyWon(PokerHand.JACKS_OR_BETTER)));
		output.add(String.format("Two pairs pay $%,d, a three of a kind pays $%,d, a straight pays $%,d, ",
				getMoneyWon(PokerHand.TWO_PAIR), getMoneyWon(PokerHand.THREE_OF_A_KIND), getMoneyWon(PokerHand.STRAIGHT))
				+ String.format("a flush pays $%,d, a full house pays $%,d, a four of a kind pays $%,d, ",
				getMoneyWon(PokerHand.FLUSH), getMoneyWon(PokerHand.FULL_HOUSE), getMoneyWon(PokerHand.FOUR_OF_A_KIND))
				+ String.format("a straight flush pays $%,d, a five of a kind pays $%,d, a wild royal flush pays $%,d, ",
				getMoneyWon(PokerHand.STRAIGHT_FLUSH), getMoneyWon(PokerHand.FIVE_OF_A_KIND), getMoneyWon(PokerHand.WILD_ROYAL))
				+ String.format("and four deuces pay $%,d.", getMoneyWon(PokerHand.FOUR_DEUCES)));
		output.add(String.format("If you are lucky enough to get a natural royal flush, you will win $%,d!", getMoneyWon(PokerHand.NATURAL_ROYAL)));
		output.add("Best of luck! Pick your cards when you're ready.");
		sendSkippableMessages(output);
		sendMessage(generateBoard(false));
		getInput();
	}
	
	@Override
	void playNextTurn(String pick)
	{
		LinkedList<String> output = new LinkedList<>();
		
		if (gameStage == 5)
		{
			if (pick.equalsIgnoreCase("STOP")) {
				if (hand == PokerHand.NOTHING) {
					redrawUsed = false;
					output.add("Stopping would prevent you from winning anything. Hold at least one card, then type 'DEAL'.");
				} else {
					for (int i = 0; i < cardsHeld.length; i++)
						cardsHeld[i] = true;
					redrawUsed = true;
					output.add(generateBoard(true));
				}
			}
			else if (pick.equalsIgnoreCase("DEAL")) {
				if(enhancedRedraw)
					enhancedRedraw = false;
				else
					redrawUsed = true;
				gameStage = 0;

				String cardsHeldAsString = "Cards held: ";
				String cardsRedrawingAsString = "Cards being redrawn: ";

				for (int i = 0; i < cardsHeld.length; i++) {
					if (cardsHeld[i])
						cardsHeldAsString += cardsPicked[i].toStringShort() + " ";
					else cardsRedrawingAsString += cardsPicked[i].toStringShort() + " ";
				}
				
				if (cardsHeldAsString.equals("Cards held: ")) { // i.e. we're redrawing everything
						output.add("Redrawing all five cards.");
				}
				else if (cardsRedrawingAsString.equals("Cards being redrawn: ")) { // i.e. there aren't any
					gameStage = 5;
					if (hand == PokerHand.NOTHING) {
						output.add("Holding all five cards would prevent you from winning anything. Release at least one card first.");
						redrawUsed = false;
					}	
					else {
						output.add("All five cards held; ending game.");
						output.add(generateBoard(true));
					}
					endTurn(output);
					return;
				}
				else {
					output.add(cardsHeldAsString);
					output.add(cardsRedrawingAsString);
				}
				
				// Find out what stage we should be on now
                for (boolean b : cardsHeld) {
                    if (!b)
                        break;
                    else gameStage++;
                }
				output.add(generateBoard(false));
				output.add("Select your cards for the redraw when you are ready.");
			}

			// The next two if-else blocks could *probably* be merged together since they do the same thing with two
			// exceptions, but I'm lazy :P --Coug
			else if (pick.toUpperCase().startsWith("HOLD ")) {
				String[] tokens = pick.split("\\s");
				
				// If there are any non-numeric tokens after "HOLD", assume it's just the player talking
				for (int i = 1; i < tokens.length; i++)
				{
					if (!isNumber(tokens[i]))
					{
						endTurn(output);
						return;
					}
				}
				
				// Make sure the player's choices correspond to actual cards
				try {
					// The manual deep copy is intentional for safety. If we go into the catch block, we lose this array.
					boolean[] testCardsHeld = deepCopy(cardsHeld);

					for (int i = 1; i < tokens.length; i++)
					{
						testCardsHeld[Integer.parseInt(tokens[i])-1] = true;
					}
					cardsHeld = testCardsHeld;
					output.add(generateHand());
					output.add("You may 'HOLD' other cards, 'RELEASE' cards you no longer wish to hold, or type 'DEAL' to start the redraw.");
				}
				catch (IndexOutOfBoundsException e)
				{
					output.add("Invalid card(s).");
					endTurn(output);
					return;
				}
			}
			else if (pick.toUpperCase().startsWith("RELEASE ")) {
				String[] tokens = pick.split("\\s");
				
				// If there are any non-numeric tokens after "RELEASE", assume it's just the player talking
				for (int i = 1; i < tokens.length; i++)
				{
					if (!isNumber(tokens[i]))
					{
						endTurn(output);
						return;
					}
				}
				
				// Make sure the player's choices correspond to actual cards
				try {
					// The manual deep copy is intentional for safety. If we go into the catch block, we lose this array.
					boolean[] testCardsHeld = deepCopy(cardsHeld);

					for (int i = 1; i < tokens.length; i++)
					{
						testCardsHeld[Integer.parseInt(tokens[i])-1] = false;
					}
					cardsHeld = testCardsHeld;
					output.add(generateHand());
					output.add("You may 'HOLD' other cards, 'RELEASE' cards you no longer wish to hold, or type 'DEAL' to start the redraw.");
				}
				catch (IndexOutOfBoundsException e)
				{
					output.add("Invalid card(s).");
					endTurn(output);
					return;
				}
			}
		}
		else //This code only triggers if we still need to draw cards, of course
		{
			String[] tokens = pick.split("\\s");
            for (String token : tokens) {
                if (!isNumber(token)) {
                    endTurn(output);
                    return;
                }
                if (!checkValidNumber(token)) {
                    output.add("Invalid pick.");
                    endTurn(output);
                    return;
                }
            }
			for(String nextPick : tokens)
			{
				//Stop picking cards if we've already got five
				if(gameStage == 5)
					break;
				lastSpace = Integer.parseInt(nextPick)-1;
				if(pickedSpaces[lastSpace]) {
					output.add("You cannot draw the same card more than once.");
					break;
				}
				else pickedSpaces[lastSpace] = true;
				lastPicked = board.get(lastSpace);
				cardsPicked[gameStage] = lastPicked;
				//Autohold deuces, or any card once we've already redrawn
				if(redrawUsed || lastPicked.getRank() == CardRank.DEUCE)
					cardsHeld[gameStage] = true;
				do {
					gameStage++;
				} while (gameStage < 5 && cardsHeld[gameStage]);
				if (gameStage == 5)
					hand = evaluateHand(cardsPicked);
				output.add(String.format("Space %d selected...",lastSpace+1));
				output.add("**" + lastPicked.toString() + "**");
			}
			output.add(generateBoard(gameStage == 5 && (redrawUsed || hand == PokerHand.NATURAL_ROYAL)));
			if (gameStage == 5 && hand != PokerHand.NATURAL_ROYAL && !redrawUsed) {
				sendMessages(output);
				output.clear();
				
				LinkedList<String> skippableOutput = new LinkedList<>();
				String firstMessage = "You may now hold any or all of your five cards by typing HOLD followed by the numeric positions "
						+ "of each card.";
				
				int numDeuces = 0;
                for (Card card : cardsPicked) {
                    if (card.getRank() == CardRank.DEUCE)
                        numDeuces++;
                }
				if (numDeuces > 0) {
					firstMessage += " Your deuce";
					
					if (numDeuces > 1)
						firstMessage += "s have";
					else firstMessage += " has";

					firstMessage += " been automatically held for you.";
				}

				skippableOutput.add(firstMessage);
				skippableOutput.add("For example, type 'HOLD 1' to hold the " + cardsPicked[0] + ".");
				skippableOutput.add("If you change your mind or make a mistake, type RELEASE followed by the position number of the card " +
						"you would rather redraw, e.g. 'RELEASE 2' to remove any hold on the " + cardsPicked[1] + ".");
				skippableOutput.add("You may also hold or release more than one card at a time; for example, you may type 'HOLD 3 4 5' to " +
						"hold the " + cardsPicked[2] + ", the " + cardsPicked[3]  + ", and the " + cardsPicked[4] + ".");
				skippableOutput.add("The cards you do not hold will all be redrawn in the hopes of a better hand.");
				if(hand != PokerHand.NOTHING)
					skippableOutput.add(String.format("If you like your hand, you may also type 'STOP' to end the game and claim your "+
							"prize of $%,d.", getMoneyWon(hand)));
				skippableOutput.add("When you are ready, type 'DEAL' to redraw the unheld cards.");
				sendSkippableMessages(skippableOutput);
			}
		}
		endTurn(output);
	}
	
	private void endTurn(LinkedList<String> output)
	{
		sendMessages(output);
		if(isGameOver())
			awardMoneyWon(getMoneyWon(hand));
		else
			getInput();
	}
	
	private boolean checkValidNumber(String message)
	{
		int location = Integer.parseInt(message)-1;
		return (location >= 0 && location < BOARD_SIZE);
	}
	
	private String generateBoard(boolean reveal)
	{
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append("      DEUCES WILD\n   ");
		for(int i=0; i<BOARD_SIZE; i++)
		{
			if(pickedSpaces[i])
			{
				display.append("  ");
			}
			else if (reveal) {
				display.append(board.get(i).toStringShort());				
			} else {
				display.append(String.format("%02d",(i+1)));
			}
			if (i == 45)
				display.append("\n   ");
			else if(i == 5 || (i > 5 && i%8 == 5))
				display.append("\n");
			else
				display.append(" ");
		}
		display.append("\n\n" + "Current hand: "); // The concatenation here is more for human legibility than anything
		for (int i = 0; i < cardsPicked.length; i++)
		{
			if (cardsPicked[i] == null)
				break;
			if (gameStage <= i && !cardsHeld[i])
				display.append("   ");
			else display.append(cardsPicked[i].toStringShort() + " ");
		}
		if (gameStage == 5) {
			display.append("(" + hand.toString() + ")");
			if (!redrawUsed && hand != PokerHand.NATURAL_ROYAL)
			{
				display.append("\n              ");
				for (int i = 0; i < cardsPicked.length; i++)
				{
					display.append(i+1);
					display.append(cardsHeld[i] ? "*" : " ");
					display.append(" ");
				}
			}
		}
		display.append("\n```");
		return display.toString();
	}

	private String generateHand() {
		StringBuilder display = new StringBuilder();

		display.append("```\n" + "Current hand: ");
        for (Card card : cardsPicked) {
            display.append(card.toStringShort() + " ");
        }
		display.append("\n" + "              ");
		for (int i = 0; i < cardsPicked.length; i++)
		{
			display.append(i+1);

			if (cardsHeld[i])
				display.append ("*");
			else display.append(" ");

			display.append(" ");
		}
		display.append("```");
		return display.toString();
	}

	private boolean isGameOver()
	{
		if (gameStage == 5) {
			if (hand == PokerHand.NATURAL_ROYAL)
				return true;
			else return redrawUsed;
		}
		
		return false;
	}

	public int getMoneyWon(PokerHand pokerHand) {
		switch(pokerHand) {
			case NOTHING: return applyBaseMultiplier(0);
			case ONE_PAIR: return applyBaseMultiplier(25_000);
			case JACKS_OR_BETTER: return applyBaseMultiplier(50_000);
			case TWO_PAIR: return applyBaseMultiplier(75_000);
			case THREE_OF_A_KIND: return applyBaseMultiplier(100_000);
			case STRAIGHT: return applyBaseMultiplier(200_000);
			case FLUSH: return applyBaseMultiplier(300_000);
			case FULL_HOUSE: return applyBaseMultiplier(400_000);
			case FOUR_OF_A_KIND: return applyBaseMultiplier(500_000);
			case STRAIGHT_FLUSH: return applyBaseMultiplier(750_000);
			case FIVE_OF_A_KIND: return applyBaseMultiplier(1_000_000);
			case WILD_ROYAL: return applyBaseMultiplier(2_000_000);
			case FOUR_DEUCES: return applyBaseMultiplier(5_000_000);
			case NATURAL_ROYAL: return applyBaseMultiplier(10_000_000);
			default: throw new IllegalArgumentException(); // since the above is supposed to already handle everything
		}		
	}

	// This is probably not the most efficient way to write the hand evaluator--some things are checked more than once. 
	private PokerHand evaluateHand(Card[] cards) {
		if (cards.length != 5)
			throw new IllegalArgumentException("The hand evaluator function needs 5 cards to work; it was passed " + cards.length + ".");

		byte[] rankCount = new byte[CardRank.values().length];
		byte[] suitCount = new byte[CardSuit.values().length];

        for (Card card : cards) {
            rankCount[card.getRank().ordinal()]++;
            if (card.getRank() != CardRank.DEUCE)      // for the purposes of this evaluator, deuces have no suit; that's the only
                suitCount[card.getSuit().ordinal()]++; // way I can think of to get it to work right when checking for a flush
        }

		// If we have four deuces, that precludes a natural royal flush and outpays a wild royal flush; so it's less work to check for that first
		if (rankCount[CardRank.DEUCE.ordinal()] == 4)
		{
			Achievement.DEUCES_JACKPOT.check(getCurrentPlayer());
			return PokerHand.FOUR_DEUCES;
		}

		CardRank highCardOfStraight = findStraightHighCard(rankCount); // If this is null, we do not have a straight
		boolean isFlush = isFlush(suitCount);

		// Put off the five of a kind check until these are all done--if we had one, we would have paid higher for four deuces already
		if (highCardOfStraight != null && isFlush)
		{
			if (highCardOfStraight == CardRank.ACE)
			{
				Achievement.DEUCES_JACKPOT.check(getCurrentPlayer());
				if (rankCount[CardRank.DEUCE.ordinal()] == 0)
					return PokerHand.NATURAL_ROYAL;
				else return PokerHand.WILD_ROYAL;
			}
			else return PokerHand.STRAIGHT_FLUSH;
		}

		byte modeOfRanks = modeOfRanks(rankCount); // That is, how many are there of the most common rank?
		
		switch (modeOfRanks) {
			case 5: 
				Achievement.DEUCES_JACKPOT.check(getCurrentPlayer());
				return PokerHand.FIVE_OF_A_KIND;
			case 4: return PokerHand.FOUR_OF_A_KIND;
			case 3: if (hasExtraPair(rankCount)) return PokerHand.FULL_HOUSE; // we need to check for a straight or flush
			default: break;                                                   // before we pay for a three of a kind
		}
		
		if (isFlush) return PokerHand.FLUSH;
		if (highCardOfStraight != null) return PokerHand.STRAIGHT;
		
		switch (modeOfRanks) {
			case 3: return PokerHand.THREE_OF_A_KIND;
			case 2: 
				if (hasExtraPair(rankCount)) return PokerHand.TWO_PAIR;
				else if (hasMultipleAcesOrFaces(rankCount)) return PokerHand.JACKS_OR_BETTER;
				else return PokerHand.ONE_PAIR;
			default: return PokerHand.NOTHING;
		}
	}

	private CardRank findStraightHighCard(byte[] rankCount) {
		// If we have any paired cards other than deuces, it can't be a straight, so check for that first
		for (int i = 0; i < rankCount.length; i++) {
			if (i == CardRank.DEUCE.ordinal())
				continue;
			if (rankCount[i] > 1)
				return null;
		}

		for (int i = 0; i < rankCount.length - 4; i++) {
			if (rankCount[i] + rankCount[i+1] + rankCount[i+2] + rankCount[i+3] + rankCount[i+4] == 5)
				return CardRank.values()[i+4];
			if (i > CardRank.DEUCE.ordinal() && rankCount[i] + rankCount[i+1] + rankCount[i+2] + 
					rankCount[i+3] + rankCount[i+4] + rankCount[CardRank.DEUCE.ordinal()] == 5)
				return CardRank.values()[i+4];
		}

		// The above scan doesn't catch an ace-high straight; so that is our final check:
		if (rankCount[CardRank.ACE.ordinal()] + rankCount[CardRank.DEUCE.ordinal()] + rankCount[CardRank.TEN.ordinal()]
				+ rankCount[CardRank.JACK.ordinal()] + rankCount[CardRank.QUEEN.ordinal()] + rankCount[CardRank.KING.ordinal()] == 5)
			return CardRank.ACE;
		
		return null;
	}

	private boolean isFlush(byte[] suitCount) {
		boolean suitFound = suitCount[0] != 0;

		for (int i = 1; i < suitCount.length; i++) {
			if (suitCount[i] != 0) {
				if (suitFound) return false;
				else suitFound = true;
			}
		}

		return true;
	}

	private byte modeOfRanks(byte[] rankCount) {
		byte deuces = rankCount[CardRank.DEUCE.ordinal()];
		byte max = (byte)(rankCount[CardRank.ACE.ordinal()] + deuces);

		for (int i = CardRank.THREE.ordinal(); i < rankCount.length; i++) {
			byte sum = (byte)(rankCount[i] + deuces);
			if (sum > max)
				max = sum;
		}

		return max;	
	}

	private boolean hasExtraPair(byte[] rankCount) {
		/* 
 		 * This works, but isn't entirely clear why:
 		 * There can only be a maximum of one deuce in a full house
 		 * And if there is one, it's pair + pair + deuce
 		 * Otherwise the result would be four or five of a kind and we wouldn't get to this point anyway
  		 * In any case, in a full house sorting the array comes out as either (0,2,3) or (1,2,2)
		 * And the second-to-last value would always be 2.
		 * Further, a two-pair will always be a natural hand; otherwise it'd be at least a three of a kind.
 		 */
 		byte[] sortedRankCount = deepCopy(rankCount);
 		Arrays.sort(sortedRankCount);
 		return sortedRankCount[rankCount.length - 2] == 2;
	}

	private boolean hasMultipleAcesOrFaces (byte[] rankCount) {
		for (int i = CardRank.JACK.ordinal(); i < CardRank.values().length; i++)
			if (rankCount[i] + rankCount[CardRank.DEUCE.ordinal()] > 1)
				return true;
		return rankCount[CardRank.ACE.ordinal()] + rankCount[CardRank.DEUCE.ordinal()] > 1;
	}

	private boolean[] deepCopy (boolean[] arr) { // Here for DRY purposes
		boolean[] copiedArr = new boolean[arr.length];
		for (int i = 0; i < arr.length; i++)
			copiedArr[i] = arr[i];
		return copiedArr;
	}
	private byte[] deepCopy (byte[] arr) { //overloading because generic arrays are a no-go :(
		byte[] copiedArr = new byte[arr.length];
		for (int i = 0; i < arr.length; i++)
			copiedArr[i] = arr[i];
		return copiedArr;
	}

	@Override
	String getBotPick() {
		if(gameStage == 5) {
			// If the bot has at least a straight, stop there
			if(hand.compareTo(PokerHand.STRAIGHT) >= 0)
				return "STOP";
			//Else, bot will redraw automatically in order to get as many deuces as possible (which hold automatically)
			else return "DEAL";
		}
		ArrayList<Integer> openSpaces = new ArrayList<>(BOARD_SIZE);
		for(int i=0; i<BOARD_SIZE; i++)
			if(!pickedSpaces[i])
				openSpaces.add(i+1);
		return String.valueOf(openSpaces.get((int)(Math.random()*openSpaces.size())));
	}

	@Override
	void abortGame()
	{
		//If we're sitting on a hand right now we give them that, otherwise we can't
		if(gameStage == 5)
			awardMoneyWon(getMoneyWon(hand));
		else
			awardMoneyWon(0);
	}

	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	@Override public String getEnhanceText() { return "You will be able to redraw cards twice."; }
}
