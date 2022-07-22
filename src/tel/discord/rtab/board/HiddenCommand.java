package tel.discord.rtab.board;

public enum HiddenCommand implements WeightedSpace
{
	NONE(0,		"**Absolutely Nothing!**\n"
			+ "(You should never see this message)",
			"You do not currently possess any hidden command."),
	FOLD(1,		"A **FOLD**!\n"
			+ "The fold allows you to drop out of the round at any time by typing **!fold**.\n"
			+ "If you use it, you will keep your multipliers and minigames, "
			+ "so consider it a free escape from a dangerous board!",
			"You currently possess a **FOLD**.\n"
			+ "You may use it at any time by typing **!fold**."),
	BLAMMO(1,	"A **BLAMMO SUMMONER**!\n"
			+ "You may use this by typing **!blammo** at any time to give the next player a blammo!\n"
			+ "This will activate on the NEXT turn (not the current one), and will replace that player's normal turn.",
			"You currently possess a **BLAMMO SUMMONER**.\n"
			+ "You may use it at any time by typing **!blammo**."),
	DEFUSE(1,	"A **SHUFFLER**!\n"
			+ "You may use this at any time by typing **!shuffle 13**, replacing '13' with the space you wish to shuffle.\n"
			+ "This will replace the contents of the space with a newly-generated space, removing any bomb there. Use this wisely!\n",
			"You currently possess a **SHUFFLER**.\n"
			+ "You may use it at any time by typing **!shuffle** followed by the space you wish to shuffle."),
	WAGER(1,	"A **WAGERER**!\n"
			+ "The wager allows you to force all living players to add a portion of their total bank to a prize pool, "
			+ "which the winner(s) of the round will claim.\n"
			+ "The amount is equal to 1% of the average total bank in the game, "
			+ "and you can activate this at any time by typing **!wager**.",
			"You currently possess a **WAGERER**.\n"
			+ "You may use it at any time by typing **!wager**."),
	BONUS(1,	"A **BONUS BAG**!\n"
			+ "The bonus bag contains many things, "
			+ "and you can use this command to pass your turn and draw from the bag instead.\n"
			+ "To do so, type !bonus followed by either 'cash', 'boost', 'game', or 'event', depending on what you want.",
			"You currently possess a **BONUS BAG**.\n"
			+ "You may use it at any time by typing **!bonus** followed by 'cash', 'boost', 'game', or 'event'."),
	TRUESIGHT(1,"An **EYE OF TRUTH**!\n"
			+ "You may use the eye by typing **!truth 13**, replacing '13' with the space you wish to look at.\n"
			+ "This will allow you to look at the exact contents of the space, not just the category.",
			"You currently possess an **EYE OF TRUTH**.\n"
			+ "You may use it at any time by typing **!truth** followed by the space you wish to look at."),
	FAILSAFE(1,	"A **FAILSAFE**!\n"
			+ "If you are ever presented with a situation where every remaining space is a bomb, "
			+ "you can use this command by typing **!failsafe** to escape and *immediately* win the round.\n"
			+ "If there is even one safe space left on the board, however, you lose this command and pay a $1,000,000 penalty.",
			"You currently possess a **FAILSAFE**.\n"
			+ "You may use it when you believe every remaining space is a bomb by typing **!failsafe**."),
	MINESWEEP(1,"A **MINESWEEPER**!\n"
			+ "You can use this command at any time by typing **!minesweeper 13**, replacing '13' with the space you wish to sweep around.\n"
			+ "This will tell you how many unpicked bombs are in the eight spaces adjacent to the chosen space.\n"
			+ "It will not tell you anything about the space you chose directly.",
			"You currently possess a **MINESWEEPER**.\n"
			+ "You may use it at any time by typing **!minesweeper** followed by the space you wish to sweep around."),
	REPEL(0,	"**BLAMMO REPELLENT**!\n"
			+ "You may use this by typing **!repel** whenever any player is facing a blammo to automatically block it.\n"
			+ "The person affected will then need to choose a different space from the board.",
			"You currently possess **BLAMMO REPELLENT**.\n"
			+ "You may use it when a blammo is in play by typing **!repel**.");
	
	public final String pickupText;
	public final String carryoverText;
	final int weight;
	
	HiddenCommand(int weight, String pickupText, String carryoverText)
	{
		this.weight = weight;
		this.pickupText = pickupText;
		this.carryoverText = carryoverText;
	}
	@Override
	public int getWeight(int playercount)
	{
		return weight;
	}
}
