package tel.discord.rtab.games.objs;

import java.util.Collections;
import java.util.LinkedList;

public class Deck {
	LinkedList<Card> deck;

	public Deck() {
		this(1);
	}

	public Deck(int numDecks) {
		deck = new LinkedList<>();
		addDecks(numDecks);
	}

	public void addDecks(int numDecks) {
		CardRank[] ranks = CardRank.values();
		CardSuit[] suits = CardSuit.values();
		
		for (int i = 0; i < numDecks; i++) {
			for (int j = 0; j < ranks.length * suits.length; j++) {
				deck.add(new Card(ranks[j/suits.length],suits[j%suits.length]));
			}
		}
	}

	public void clear() {
		deck.clear();
	}

	public void shuffle() {
		Collections.shuffle(deck);
	}

	public Card dealCard() {
		return deck.pop();
	}
}