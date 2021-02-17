package tel.discord.rtab.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.internal.utils.tuple.ImmutablePair;
import net.dv8tion.jda.internal.utils.tuple.Pair;
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
		if(event.getArgs().equals(""))
		{
			name = event.getMember().getEffectiveName();
			uID = event.getAuthor().getId();
		}
		else if(event.getArgs().startsWith("<@"))
		{
			uID = parseMention(event.getArgs());
			name = "";
		}
		else
		{
			name = event.getArgs();
			uID = null;
		}
		String output = getHistoryMessage(uID, name, event.getTextChannel());
		event.reply(output);
	}
	public String getHistoryMessage(String uID, String name, TextChannel channel)
	{
		try
		{
			String channelID = channel.getId(); //The command is flagged guild-only, so we know this won't be null
			int season = 1;
			int minRank = Integer.MAX_VALUE;
			int wins = 0;
			List<Pair<Integer,Long>> cashFigures = new LinkedList<>();
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
					cashFigures.add(ImmutablePair.of(season,Long.parseLong(record[2])));
					if(index < minRank)
						minRank = index;
					if(index == 0)
						wins++;
					if(uID != null)
						name = record[1];
				}
				//Then move on to the next season
				season ++;
			}
			//So now we've gone through every past season and got a list of our scores, time to generate stats?
			StringBuilder output = new StringBuilder();
			output.append("```\n");
			if(!name.equals(""))
				output.append("History for "+name+"\n\n");
			//Loop through each season and calculate stats
			StringBuilder seasonList = new StringBuilder();
			int moneyWidth = minRank == 0 ? 17 : 13;
			int seasonsPlayed = 0;
			long thisSeason;
			long totalCash = 0;
			long maingameCash = 0;
			long bestResult = 0;
			int veteranSeasons = 0;
			int regularSeasons = 0;
			int grinderSeasons = 0;
			for(Pair<Integer,Long> nextSeason : cashFigures)
			{
				seasonsPlayed ++;
				thisSeason = nextSeason.getRight();
				totalCash += thisSeason;
				maingameCash += Math.min(1_000_000_000, thisSeason);
				if(thisSeason > bestResult)
					bestResult = thisSeason;
				seasonList.append(String.format("Season %1$d: $%2$,"+moneyWidth+"d\n",nextSeason.getLeft(),thisSeason));
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
				output.append("Seasons Played: "+seasonsPlayed+"\n");
				if(wins > 0)
					output.append("Seasons Won: "+wins+"\n");
				else
					output.append("Best Rank: #"+(minRank+1)+"\n");
				output.append(String.format("Total Cash Earned:  $%,d\n",totalCash));
				if(totalCash > maingameCash)
					output.append(String.format("Maingame Cash Earned: $%,d\n",maingameCash));
				output.append(String.format("Best Season Result: $%,d\n\n",bestResult));
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
