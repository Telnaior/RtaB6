package tel.discord.rtab.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.internal.utils.tuple.MutableTriple;
import tel.discord.rtab.Achievement;

public class HistoryCommand extends ParsingCommand
{
    public HistoryCommand()
    {
        this.name = "history";
        this.help = "view how you have done in seasons past";
        this.guildOnly = true;
    }
	@Override
	protected void execute(CommandEvent event)
	{
		String name;
		String uID;

		//Enter own ID if no name given (to ensure match even if name changed)
		if(event.getArgs().equals(""))
		{
			name = event.getMember().getEffectiveName();
			uID = event.getAuthor().getId();
		}

		//Or search by UUID if the command user gave a mention
		else if(event.getArgs().contains("<@"))
		{
			uID = parseMention(event.getArgs());
			name = "";
		}
		//Otherwise attempt to search for the string given as a displayname to see if it matches history entries.
		else
		{
			name = event.getArgs();
			uID = null;
		}
		event.reply(getHistoryMessage(uID, name, event.getTextChannel()));
	}
	
	public String getHistoryMessage(String uID, String name, TextChannel channel)
	{
		try
		{
			String channelID = channel.getId(); //The command is flagged guild-only, so we know this won't be null
			int season = 1;
			int minRank = Integer.MAX_VALUE;
			int maxRank = 0;
			int wins = 0;
			List<MutableTriple<Integer,Integer,Long>> cashFigures = new LinkedList<>();
			//We're going to keep reading history files as long as they're there
			while(Files.exists(Paths.get("scores","history"+channelID+"s"+season+".csv")))
			{
				//Load up the next one
				List<String> list = Files.readAllLines(
						Paths.get("scores","history"+channelID+"s"+season+".csv"));
				int index;
				//If we find them, add their records to the pile
				if(uID != null)
					index = findUserInList(list,uID,false);
				else
					index = findUserInList(list,name,true);
				if(index >= 0 && index < list.size())
				{
					String[] record = list.get(index).split("#");
					cashFigures.add(MutableTriple.of(season,(index+1),Long.parseLong(record[2])));
					if(index < minRank)
						minRank = index;
					if(index > maxRank)
						maxRank = index+1;
					if(index == 0)
						wins++;
					if(uID != null)
						name = record[1];
				}
				//Then move on to the next season
				season ++;
			}
			if(season == 1) //if we didn't find any history
			{
				return "This channel has no past seasons.";
				
			}
			//So now we've gone through every past season and got a list of our scores, time to generate stats?
			StringBuilder output = new StringBuilder();
			output.append("```\n");
			if(!name.isEmpty())
				output.append("History for ").append(name).append("\n\n");
			//Loop through each season and calculate stats
			StringBuilder seasonList = new StringBuilder();
			int moneyWidth = minRank == 0 ? 17 : 13;
			int rankWidth = maxRank == 1 ? 1 : (int)Math.log10(maxRank-1)+1;
			//season stops at the first one that doesn't exist, so we subtract 1
			//and if there's no history data at all (so season 1 is the first that doesn't exist) we have a failsave to avoid div-by-0
			int seasonWidth = (int)Math.log10(season-1)+1;
			int seasonsPlayed = 0;
			long thisSeason;
			long totalCash = 0;
			long maingameCash = 0;
			long bestResult = 0;
			int veteranSeasons = 0;
			int regularSeasons = 0;
			int grinderSeasons = 0;
			for(MutableTriple<Integer,Integer,Long> nextSeason : cashFigures)
			{
				seasonsPlayed ++;
				thisSeason = nextSeason.getRight();
				totalCash += thisSeason;
				maingameCash += Math.min(1_000_000_000, thisSeason);
				if(thisSeason > bestResult)
					bestResult = thisSeason;
				seasonList.append(String.format("Season %"+seasonWidth+"d: $%,"+moneyWidth+"d"
						+ " - #%"+rankWidth+"d\n",nextSeason.getLeft(), thisSeason, nextSeason.getMiddle()));
				//Achievement Check
				if(thisSeason >= 100_000_000)
				{
					veteranSeasons ++;
					if(thisSeason >= 200_000_000)
					{
						regularSeasons ++;
						if(thisSeason >= 500_000_000)
							grinderSeasons ++;
					}
				}
			}
			//Award achievements earned
			if(uID != null)
			{
				if(veteranSeasons >= 10)
					Achievement.VETERAN.award(uID, name, channel);
				if(regularSeasons >= 5)
					Achievement.REGULAR.award(uID, name, channel);
				if(grinderSeasons >= 2)
					Achievement.GRINDER.award(uID, name, channel);
			}
			//Got the stats, attach them all on
			if(seasonsPlayed > 0)
			{
				output.append("Seasons Played: ").append(seasonsPlayed).append("\n");
				if(wins > 0)
					output.append("Seasons Won: ").append(wins).append("\n");
				else
					output.append("Best Rank: #").append(minRank + 1).append("\n");
				output.append(String.format("Total Cash Earned:  $%,d%n",totalCash));
				if(totalCash > maingameCash)
					output.append(String.format("Maingame Cash Earned: $%,d%n",maingameCash));
				output.append(String.format("Best Season Result: $%,d%n%n",bestResult));
				output.append(seasonList);
			}
			else
				output.append("No data found.");
			output.append("\n```");
			return output.toString();
		}
		catch (IOException e)
		{
			return "History file read failure.";
		}
	}
}
