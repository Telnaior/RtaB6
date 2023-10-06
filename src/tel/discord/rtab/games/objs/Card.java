package tel.discord.rtab.games.objs;

// Swiped and slightly modified from another project of mine I did for university. --Coug

/**
 * @param rank the card's rank
 * @param suit the card's suit
 */
public record Card(CardRank rank, CardSuit suit) implements Comparable<Card> {
    /**
     * Checks whether this card outranks the card in the argument. This method,
     * unlike {@code compareTo(Card o, boolean acesHigh)}, ignores suit, eliminating some of the
     * overhead from the {@code compareTo(Card o, boolean acesHigh)} method.
     *
     * @param other    the card to be compared to
     * @param acesHigh true if aces should be considered the highest card (rather than the lowest)
     * @return true if the object card is higher in rank than the argument card,
     * false otherwise
     */
    public boolean outranks(Card other, boolean acesHigh) {
        if (other == null)
            throw new NullPointerException();

        return this.rank.getValue(acesHigh) > other.rank().getValue(acesHigh);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        if (getClass() != obj.getClass())
            return false;

        final Card other = (Card) obj;

        return this.rank == other.rank() && this.suit == other.suit();
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
     * @param other    the card to be compared to
     * @param acesHigh true if aces should be considered the highest card (rather than the lowest)
     * @return a positive number if this card is higher than the card in the
     * argument, a negative number if this card is lower than the card in the
     * argument, and 0 if the two cards are identical
     */
    public int compareTo(Card other, boolean acesHigh) {
        if (other == null)
            throw new NullPointerException();

        return (this.suit.ordinal() * CardRank.values().length +
                this.rank.getValue(acesHigh)) - (other.suit().ordinal() *
                CardRank.values().length + other.rank().getValue(acesHigh));
    }

    @Override
    public String toString() {
        return this.rank.getName() + " of " + this.suit.getName();
    }

    public String toStringShort() {
        return this.rank.getSymbol() + this.suit.getSymbol();
    }
}
