package tel.discord.rtab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import tel.discord.rtab.commands.channel.BooleanSetting;

public enum Achievement
{
	/*
	 * IMPORTANT NOTE
	 * These things are stored as integers. 32-bit integers. That means you can have a maximum of 32 achievements per category.
	 * If we end up with more than that, it's time to either break into subcategories or get out the longs. 
	 */

	//Milestone Achievements - 8 total
	VETERAN("Veteran", "Earn $100m in ten different seasons (use !history to check)", AchievementType.MILESTONE, 0, false),
	REGULAR("Regular", "Earn $200m in five different seasons", AchievementType.MILESTONE, 1, false),
	GRINDER("Nose to the Grindstone", "Earn $500m in two different seasons", AchievementType.MILESTONE, 2, false),
	FOUR("Hot Streak", "Achieve a 4x win streak", AchievementType.MILESTONE, 3, false),
	EIGHT("On Fire", "Achieve an 8x win streak", AchievementType.MILESTONE, 4, false),
	TWELVE("Rampage", "Achieve a 12x win streak", AchievementType.MILESTONE, 5, false),
	SIXTEEN("Unstoppable", "Achieve a 16x win streak", AchievementType.MILESTONE, 6, false),
	TWENTY("Beyond", "Achieve a 20x win streak", AchievementType.MILESTONE, 7, false),
	SPRINT("Sprint Champion", "Earn $1,000,000,000 in a Sprint Series", AchievementType.MILESTONE, 8, false),
	TOURNAMENT("Minigame Tournament Champion", "Achieve Rank S in the final round of a Minigame Tournament", AchievementType.MILESTONE, 9, false),
	//Event Achievements - 15 total
	TRIPLE_MINIGAME("Triple Stakes Minigame", "Win three or more copies of a single minigame", AchievementType.EVENT, 0, false),
	MEGA_DEFUSE("The Great Escape", "Block a Mega Blammo", AchievementType.EVENT, 1, false),
	SUMMON_ESCAPE("Summoned for Nothing", "Win a round with a pending blammo", AchievementType.EVENT, 13, false),
	SOLO_BOARD_CLEAR("Clean Sweep", "Achieve a Solo Board Clear with fourteen or more players", AchievementType.EVENT, 2, false),
	BOOST_MAGNET("Neodymium Magnet", "Steal 400% boost with a single Boost Magnet", AchievementType.EVENT, 3, false),
	EXTRA_JOKER("Double Joker", "Hold two Jokers at the same time", AchievementType.EVENT, 4, false),
	BIG_JACKPOT("Monster Jackpot", "Win a Jackpot you found in the first turn of a game", AchievementType.EVENT, 5, false),
	EXTRA_PEEKS("Knowledge is Power", "Use three peeks in one game", AchievementType.EVENT, 6, false),
	SPLIT_COMMUNISM("Bowser to the Rescue","Undo a Split and Share with Bowser Revolution", AchievementType.EVENT, 7, false),
	STAR_MINEFIELD("Super Star", "Find a Starman that destroys more bombs than there are players in-game", AchievementType.EVENT, 8, false),
	UNBANKRUPT("Reverse Bankrupt", "Gain more than the bomb penalty from a Bankrupt Bomb", AchievementType.EVENT, 9, false),
	STREAK_BLAST("Streak Blast Beneficiary", "Gain 4x streak from a Streak Blast Bomb", AchievementType.EVENT, 10, false),
	LUCKY_WIN("The Last Hope", "Have a bomb on the final space of the board fail to explode by chance", AchievementType.EVENT, 11, false),
	BAGCEPTION("Bag of Holding", "Win a bonus bag from a bonus bag", AchievementType.EVENT, 12, false),
	BONUS_FOLD("Lock it In", "Fold with a bonus game in your minigame queue", AchievementType.EVENT, 14, false),
	//Minigame Achievements - 23 total
	SUPERCASH_JACKPOT("Supercash Jackpot", "Win the Jackpot prize in Supercash", AchievementType.MINIGAME, 0, true),
	GLOBETROTTER_JACKPOT("Around the World", "Win at least $10 million in Globetrotter", AchievementType.MINIGAME, 22, false),
	DIGITAL_JACKPOT("Digital Fortress Jackpot", "Find all 10 digits in Digital Fortress", AchievementType.MINIGAME, 1, false),
	SPECTRUM_JACKPOT("Spectrum Jackpot", "Win every pair in Spectrum", AchievementType.MINIGAME, 2, false),
	HYPERCUBE_JACKPOT("Hypercube Jackpot", "Score 500 or more points in Hypercube", AchievementType.MINIGAME, 3, false),
	ROULETTE_JACKPOT("Wheel of Fortune", "Hit all three doubles and win half the top prize in Bomb Roulette", AchievementType.MINIGAME, 4, false),
	BOOSTER_JACKPOT("Booster Smash Maxout", "Reach 999% booster with Booster Smash", AchievementType.MINIGAME, 5, true),
	BUMPER_JACKPOT("Bumper Grab World Tour", "Escape through the last exit with half the top prize in Bumper Grab", AchievementType.MINIGAME, 6, false),
	SHAVE_JACKPOT("A Perfect Shave", "Earn the maximum multiplier in Close Shave", AchievementType.MINIGAME, 7, false),
	DEAL_JACKPOT("The Dream Finish", "Have the top two boxes at the end of Deal or No Deal", AchievementType.MINIGAME, 8, false),
	DEUCES_JACKPOT("Heart of the Deuces", "Win Five of a Kind or better in Deuces Wild", AchievementType.MINIGAME, 9, true),
	ZEROES_JACKPOT("It's Over Nine Million!", "Find a Double Zero with the Joker and a 9 as your first digit", AchievementType.MINIGAME, 10, true),
	FTROTS_JACKPOT("For the Rest of the Season", "Reach the top of the time ladder in For the Rest of the Season", AchievementType.MINIGAME, 11, false),
	DANGER_RECORD("Daredevil", "Hold the Danger Dice Season Record at the end of the season", AchievementType.MINIGAME, 12, false),
	FLOW_JACKPOT("Full to the Brim", "Find every joker and win some of everything in Overflow", AchievementType.MINIGAME, 13, false),
	BOX_JACKPOT("Box Slammed Shut", "Win the top prize in Shut the Box", AchievementType.MINIGAME, 14, false),
	STRIKE_JACKPOT("Struck Gold", "Win the top prize with a full count in Strike it Rich", AchievementType.MINIGAME, 15, true),
	CYS_JACKPOT("Called Your Shot", "Find and win the gold ball in Call Your Shot", AchievementType.MINIGAME, 16, true),
	OFFER_JACKPOT("The Triskaidekaphobic Offer", "Accept and survive a 13-tick offer in Three Offers", AchievementType.MINIGAME, 17, true),
	ZILCH_JACKPOT("Dice Five Million", "Win the top prize in Zilch", AchievementType.MINIGAME, 18, false),
	PUNCH_JACKPOT("Punched It All", "Find the top space on the last punch in Punch-A-Bunch", AchievementType.MINIGAME, 19, true),
	COLOUR_JACKPOT("On the Money", "Withdraw the full amount from two banks in Colour of Money", AchievementType.MINIGAME, 20, false),
	STARDUST_JACKPOT("The Restaurant at the End of the Universe", "Find a star in Zone 5 of Stardust", AchievementType.MINIGAME, 21, false),
	//Minigame Lucky Charms - 16 total
	ROULETTE_CHARM("Lucky Roulette Wheel", "Randomly awarded while playing Bomb Roulette", AchievementType.CHARM, 0, true),
	BUMPER_CHARM("Lucky Pinball Bumper", "Randomly awarded while playing Bumper Grab", AchievementType.CHARM, 1, true),
	SHAVE_CHARM("Lucky Razor", "Randomly awarded while playing Close Shave", AchievementType.CHARM, 2, true),
	COLOUR_CHARM("Lucky Colour Wheel", "Randomly awarded while playing Colour of Money", AchievementType.CHARM, 3, true),
	DEAL_CHARM("Lucky Box Seal", "Randomly awarded while playing Deal or No Deal", AchievementType.CHARM, 4, true),
	FTROTS_CHARM("Lucky Lightstick", "Randomly awarded while playing For the Rest of the Season", AchievementType.CHARM, 5, true),
	MATH_CHARM("Lucky Abacus", "Randomly awarded while playing Math Time", AchievementType.CHARM, 6, true),
	CARDS_CHARM("Lucky Ace of Spades", "Randomly awarded while playing Money Cards", AchievementType.CHARM, 7, true),
	FLOW_CHARM("Lucky Overflow Valve", "Randomly awarded while playing Overflow", AchievementType.CHARM, 8, true),
	SAFE_CHARM("Lucky Stethoscope", "Randomly awarded while playing Safe Cracker", AchievementType.CHARM, 9, true),
	BOX_CHARM("Lucky Number Box", "Randomly awarded while playing Shut the Box", AchievementType.CHARM, 10, true),
	SPLIT_CHARM("Lucky Plan B", "Randomly awarded while playing Split Winnings", AchievementType.CHARM, 11, true),
	STAR_CHARM("Lucky Shooting Star", "Randomly awarded while playing Stardust", AchievementType.CHARM, 12, true),
	TICTAC_CHARM("Lucky X and O", "Randomly awarded while playing Tic Tac Bomb", AchievementType.CHARM, 13, true),
	TRIPLE_CHARM("Lucky Baseball", "Randomly awarded while playing Triple Play", AchievementType.CHARM, 14, true),
	ZILCH_CHARM("Lucky Set of Dice", "Randomly awarded while playing Zilch", AchievementType.CHARM, 15, true),
	DANGER_CHARM("Lucky Limitless Ladder", "Randomly awarded while playing Danger Dice", AchievementType.CHARM, 16, true),
	DASH_CHARM("Lucky Laces", "Randomly awarded while playing 50-Yard Dash", AchievementType.CHARM, 17, true),
	SMASH_CHARM("Lucky Boost Token", "Randomly awarded while playing Booster Smash", AchievementType.CHARM, 18, true);

	public enum AchievementType
	{
		EVENT(2),
		MINIGAME(3),
		MILESTONE(4),
		CHARM(5);
		
		public final int recordLocation;
		AchievementType(int recordLocation)
		{
			this.recordLocation = recordLocation;
		}
	}
	
	public final String publicName;
	public final String unlockCondition;
	public final AchievementType achievementType;
	public final int bitLocation;
	public final boolean retired;
	
	Achievement(String publicName, String unlockCondition, AchievementType achievementType, int recordLocation, boolean retired)
	{
		this.publicName = publicName;
		this.unlockCondition = unlockCondition;
		this.achievementType = achievementType;
		this.bitLocation = recordLocation;
		this.retired = retired;
	}
	
	public boolean check(Player winner)
	{
		//&& will make sure we only try to award if we're in a game channel (as opposed to a minigame test)
		return winner.game != null && award(winner.uID, winner.getName(), winner.game.channel);
	}
	
	public boolean award(String playerID, String name, TextChannel channel)
	{
		try
		{
			//Start by grabbing the channel setting from the guild file to make sure we're eligible for levels here
			String channelID = channel.getId();
			List<String> list = Files.readAllLines(Paths.get("guilds","guild"+channel.getGuild().getId()+".csv"));
			for(String channelString : list)
			{
				String[] record = channelString.split("#");
				if(record[0].equals(channelID))
				{
					if(BooleanSetting.parseSetting(record[12],false))
					{
						//That's a yes, do the math to award them!
						String[] playerRecord = getAchievementList(playerID, channel.getGuild().getId());
						playerRecord[1] = name; //Update their name in the record file
						//Get the right achievement type
						int achievementFlags = Integer.parseInt(playerRecord[achievementType.recordLocation]);
						boolean achievementOwned = (achievementFlags >>> bitLocation) % 2 == 1;
						if(achievementOwned)
							return false;
						//They don't have the achievement, flip the relevant bit and save it
						achievementFlags += (1 << bitLocation);
						playerRecord[achievementType.recordLocation] = String.valueOf(achievementFlags);
						saveAchievementList(playerRecord, channel.getGuild().getId());
						//Update their player level
						PlayerLevel playerLevelData = new PlayerLevel(channel.getGuild().getId(), playerID, name);
						playerLevelData.addAchievementLevel();
						playerLevelData.saveLevel();
						channel.sendMessage(String.format("**%s** earned a new achievement: **%s**! Level %d achieved!", 
								name, publicName, playerLevelData.getTotalLevel())).queue();
						return true;
					}
					else
						return false;
				}
			}
			//We didn't find the channel in the list somehow? That's... not good
			System.err.println("Orphaned guild channel???");
			channel.sendMessage("Achievement failed to save.").queue();
			return false;
		}
		catch(IOException e)
		{
			channel.sendMessage("Achievement failed to save.").queue();
			return false;
		}
	}
	
	public static String[] getAchievementList(String playerID, String guildID) throws IOException
	{
		List<String> list;
		try
		{
			list = Files.readAllLines(Paths.get("levels","achievements"+guildID+".csv"));
		}
		catch(IOException e)
		{
			System.out.println("No achievement file found for "+guildID+", creating.");
			list = new LinkedList<>();
			Files.createFile(Paths.get("levels","achievements"+guildID+".csv"));
		}
		for(String next : list)
		{
			String[] record = next.split("#");
			if(record[0].equals(playerID))
				return record;
		}
		//Didn't find it, make a default
		String[] newRecord = new String[2+AchievementType.values().length];
		newRecord[0] = playerID;
		for(int i = 2; i<newRecord.length; i++)
			newRecord[i] = "0";
		return newRecord;
	}
	
	public static void saveAchievementList(String[] playerRecord, String guildID) throws IOException
	{
		Path file = Paths.get("levels","achievements"+guildID+".csv");
		List<String> list = Files.readAllLines(file);
		boolean found = false;
		for(int i=0; i<list.size(); i++)
		{
			String[] record = list.get(i).split("#");
			if(record[0].equals(playerRecord[0]))
			{	
				list.set(i, String.join("#", playerRecord));
				found = true;
				break;
			}
		}
		if(!found)
			list.add(String.join("#", playerRecord));
		Path oldFile = Files.move(file, file.resolveSibling("achievements"+guildID+"old.csv"));
		Files.write(file, list);
		Files.delete(oldFile);
	}
}
