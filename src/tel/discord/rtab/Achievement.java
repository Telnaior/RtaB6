package tel.discord.rtab;

public enum Achievement
{
	//Event Achievements - 12 total
	TRIPLE_MINIGAME("Triple Stakes Minigame", "Win three or more copies of a single minigame", AchievementType.EVENT, 0, false),
	MEGA_DEFUSE("The Great Escape", "Block a Mega Blammo", AchievementType.EVENT, 1, false),
	SOLO_BOARD_CLEAR("Clean Sweep", "Achieve a Solo Board Clear with fourteen or more players", AchievementType.EVENT, 2, false),
	BOOST_MAGNET("Neodymium Magnet", "Steal 400% boost with a single Boost Magnet", AchievementType.EVENT, 3, false),
	EXTRA_JOKER("Double Joker", "Hold two Jokers at the same time", AchievementType.EVENT, 4, false),
	MINIGAME_FOR_ONE("Megagame", "???", AchievementType.EVENT, 5, false),
	EXTRA_PEEKS("Knowledge is Power", "Use three peeks in one turn", AchievementType.EVENT, 6, false),
	SPLIT_COMMUNISM("Bowser to the Rescue","Undo a Split and Share with Bowser Revolution", AchievementType.EVENT, 7, false),
	STAR_MINEFIELD("Super Star", "Find a Starman that destroys more bombs than there are players in-game", AchievementType.EVENT, 8, false),
	UNBANKRUPT("Reverse Bankrupt", "Gain more than the bomb penalty from a bankrupt bomb", AchievementType.EVENT, 9, false),
	STREAK_BLAST("Streak Blast Beneficiary", "Gain 4x streak from a Streak Blast Bomb", AchievementType.EVENT, 10, false),
	LUCKY_WIN("The Last Hope", "Have a bomb on the final space of the board fail to explode", AchievementType.EVENT, 11, false),
	//Minigame Achievements - 16 total
	SUPERCASH_JACKPOT("Supercash Jackpot", "Win the Jackpot prize in Supercash", AchievementType.MINIGAME, 0, false),
	DIGITAL_JACKPOT("Digital Fortress Jackpot", "Find all 10 digits in Digital Fortress", AchievementType.MINIGAME, 1, false),
	SPECTRUM_JACKPOT("Spectrum Jackpot", "Win every pair in Spectrum", AchievementType.MINIGAME, 2, false),
	HYPERCUBE_JACKPOT("Hypercube Jackpot", "Score 500 or more points in Hypercube", AchievementType.MINIGAME, 3, false),
	ROULETTE_JACKPOT("Bomb Roulette Jackpot", "Hit all three doubles and win half the top prize in Bomb Roulette", AchievementType.MINIGAME, 4, false),
	BOOSTER_JACKPOT("Booster Smash Maxout", "Reach 999% booster with Booster Smash", AchievementType.MINIGAME, 5, false),
	BUMPER_JACKPOT("Bumper Grab World Tour", "Escape through the last exit with half the top prize in Bumper Grab", AchievementType.MINIGAME, 6, false),
	SHAVE_JACKPOT("Close Shave Jackpot", "Hit a 20x multiplier in Close Shave", AchievementType.MINIGAME, 7, false),
	DEAL_JACKPOT("DoND Dream Finish", "Have the top two boxes at the end of Deal or No Deal", AchievementType.MINIGAME, 8, false),
	DEUCES_JACKPOT("Deuces Wild Jackpot", "Win Five of a Kind or better in Deuces Wild", AchievementType.MINIGAME, 9, true),
	ZEROES_JACKPOT("Double Zero Jackpot", "Find a Double Zero with a 9 as your first digit in Double Zeroes", AchievementType.MINIGAME, 10, true),
	FTROTS_JACKPOT("For the Rest of the Season", "Reach the top of the time ladder in For the Rest of the Season", AchievementType.MINIGAME, 11, false),
	HILO_JACKPOT("Hi/Lo Roller", "Correctly predict five rolls in Hi/Lo Dice", AchievementType.MINIGAME, 12, true),
	FLOW_JACKPOT("Full to the Brim", "Find both jokers and win some of everything in Overflow", AchievementType.MINIGAME, 13, false),
	BOX_JACKPOT("Box Slammed Shut", "Win the top prize in Shut the Box", AchievementType.MINIGAME, 14, false),
	STRIKE_JACKPOT("Struck Gold", "Win the top prize with a full count in Strike it Rich", AchievementType.MINIGAME, 15, true),
	//Milestone Achievements - 8 total
	VETERAN("Veteran", "Earn $100m in ten different seasons (use !history to check)", AchievementType.MILESTONE, 0, false),
	REGULAR("Regular", "Earn $200m in five different seasons", AchievementType.MILESTONE, 1, false),
	GRINDER("Nose to the Grindstone", "Earn $500m in two different seasons", AchievementType.MILESTONE, 2, false),
	FOUR("Hot Streak", "Achieve a 4x win streak", AchievementType.MILESTONE, 3, false),
	EIGHT("On Fire", "Achieve an 8x win streak", AchievementType.MILESTONE, 4, false),
	TWELVE("Rampage", "Achieve a 12x win streak", AchievementType.MILESTONE, 5, false),
	SIXTEEN("Unstoppable", "Achieve a 16x win streak", AchievementType.MILESTONE, 6, false),
	TWENTY("Beyond", "Achieve a 20x win streak", AchievementType.MILESTONE, 7, false);
	
	String publicName;
	String unlockCondition;
	AchievementType achievementType;
	int recordLocation;
	boolean retired;
	
	Achievement(String publicName, String unlockCondition, AchievementType achievementType, int recordLocation, boolean retired)
	{
		this.publicName = publicName;
		this.unlockCondition = unlockCondition;
		this.achievementType = achievementType;
		this.recordLocation = recordLocation;
		this.retired = retired;
	}
	
	public boolean award(Player winner)
	{
		return false; //TODO
	}

	private enum AchievementType
	{
		EVENT, MINIGAME, MILESTONE;
	}
}