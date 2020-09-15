package tel.discord.rtab.games.objs;

import java.util.Objects;

// Swiped and slightly modified from another project of mine I did for university. --Coug

public class Card implements Comparable<Card> {
    private final CardRank rank;
    private final CardSuit suit;
    
    /**
     * Sole constructor.
     * 
     * @param rank  the card's rank
     * @param suit  the card's suit
     */
    public Card(CardRank rank, CardSuit suit) {
        this.rank = rank;
        this.suit = suit;
    }
    
    public CardRank getRank() {
        return rank;
    }
    
    public CardSuit getSuit() {
        return suit;
    }

    /**
     * Checks whether this card outranks the card in the argument. This method,
     * unlike {@code compareTo(Card o, boolean acesHigh)}, ignores suit, eliminating some of the
     * overhead from the {@code compareTo(Card o, boolean acesHigh)} method.
     * 
     * @param other  the card to be compared to
     * @param acesHigh  true if aces should be considered the highest card (rather than the lowest)
     * @return true if the object card is higher in rank than the argument card,
     * false otherwise
     */
    public boolean outranks(Card other, boolean acesHigh) {
        if (other == null)
            throw new NullPointerException();
        
        return this.rank.getValue(acesHigh) > other.getRank().getValue(acesHigh);
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.rank);
        hash = 59 * hash + Objects.hashCode(this.suit);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        
        if (getClass() != obj.getClass())
            return false;
        
        final Card other = (Card) obj;
        
        return this.rank == other.getRank() && this.suit == other.getSuit();
    }
    
    @Override
    public int compareTo(Card other) {
        return compareTo(other, false);
    }

    /**
     * This function is intended for sorting purposes. For simply determining if
     * one card is higher in rank than another, {@code outranks(Card other, boolean acesHigh)}
     * should be called instead as this method also takes suits into account.
     * <p>
     * For the purposes of this method, if two cards are of different suits,
     * then the card of the suit listed second in {@code CardSuit} is the
     * higher card. If two cards have the same suit, then this card is
     * higher than the card in the argument if {@code outranks(Card other, boolean acesHigh)}
     * would return true.
     * 
     * @param other  the card to be compared to
     * @param acesHigh  true if aces should be considered the highest card (rather than the lowest)
     * @return a positive number if this card is higher than the card in the
     * argument, a negative number if this card is lower than the card in the
     * argument, and 0 if the two cards are identical
     */
    public int compareTo(Card other, boolean acesHigh) {
        if (other == null)
            throw new NullPointerException();
        
        return (this.suit.ordinal() * CardRank.values().length +
                this.rank.getValue(acesHigh)) - (other.getSuit().ordinal() *
                CardRank.values().length + other.getRank().getValue(acesHigh));
    }
    
    @Override
    public String toString() {
        return this.rank.getName() + " of " + this.suit.getName();
    }
    
    public String toStringShort() {
        return this.rank.getSymbol() + this.suit.getSymbol();
    }
}
