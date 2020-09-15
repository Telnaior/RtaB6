package tel.discord.rtab.games.objs;

// Swiped and slightly modified from another project of mine I did for university.
// Order shouldn't matter *TOO* much here so long as it's also updated in Card.java. --Coug

public enum CardSuit {
    CLUBS("Clubs","C"),
    DIAMONDS("Diamonds","D"),
    HEARTS("Hearts","H"),
    SPADES("Spades","S");
    
    private final String name, symbol;

    CardSuit(String name, String symbol) {
        this.name = name;
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }
}