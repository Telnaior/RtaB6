package tel.discord.rtab.games.objs;

// Swiped and slightly modified from another project of mine I did for university. The order of the ranks matter here. --Coug
public enum CardRank {
    ACE("Ace","A")
    {
    	@Override
    	public int getValue(boolean acesHigh) {
    		if(acesHigh)
    			return this.ordinal() + 14;
        	return this.ordinal() + 1;
        }	
    },
    DEUCE("Deuce","2"),
    THREE("Three","3"),
    FOUR("Four","4"),
    FIVE("Five","5"),
    SIX("Six","6"),
    SEVEN("Seven","7"),
    EIGHT("Eight","8"),
    NINE("Nine","9"),
    TEN("Ten","T"),
    JACK("Jack","J"),
    QUEEN("Queen","Q"),
    KING("King","K");
    
    private final String name, symbol;

    CardRank(String name, String symbol) {
        this.name = name;
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }
    
    /**
     * @param acesHigh  true if aces should be considered the highest card (rather than the lowest)
     * @return the value of the card - aces are 14 if aces are high, or 1 if aces are low.
     */
    public int getValue(boolean acesHigh) {
    	return this.ordinal() + 1;
    }
}