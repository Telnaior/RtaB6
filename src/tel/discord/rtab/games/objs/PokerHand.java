package tel.discord.rtab.games.objs;

public enum PokerHand {
    NOTHING("Nothing"), // couldn't think of any other good name for a losing hand
    ONE_PAIR("One Pair"),
    JACKS_OR_BETTER("Jacks or Better"),
    TWO_PAIR("Two Pair"),
    THREE_OF_A_KIND("Three of a Kind"),
    STRAIGHT("Straight"), // A-2-3-4-5 and 10-J-Q-K-A both count, even though aces are normally high
    FLUSH("Flush"),
    FULL_HOUSE("Full House"),
    FOUR_OF_A_KIND("Four of a Kind"),
    STRAIGHT_FLUSH("Straight Flush"), // A-2-3-4-5 counts, but not 10-J-Q-K-A -- that's one of the royal hands
    FIVE_OF_A_KIND("Five of a Kind"), // Only possible with deuces
    WILD_ROYAL("Wild Royal Flush"),
    FOUR_DEUCES("Four Deuces"),
    NATURAL_ROYAL("Natural Royal Flush");
    
	private final String name;
	
	PokerHand (String name){
		this.name = name;
	}
	
    public String toString() {
    	return name;
    }
}