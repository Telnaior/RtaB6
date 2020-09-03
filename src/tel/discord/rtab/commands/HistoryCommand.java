package tel.discord.rtab.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.internal.utils.tuple.ImmutablePair;
import net.dv8tion.jda.internal.utils.tuple.Pair;

public class HistoryCommand extends ParsingCommand {
    public HistoryCommand()
    {
        this.name = "history";
        this.help = "view how you have done in seasons past";
    }
	@Override
	protected void execute(CommandEvent event) {
		try
		{
			String name;
			String uID;
			if(event.getArgs().equals(""))
			{
				name = event.getMember().getEffectiveName();
				uID = event.getAuthor().getId();
			}
			else if(event.getArgs().startsWith("<@!"))
			{
				uID = parseMention(event.getArgs());
				name = event.getGuild().getMemberById(uID).getEffectiveName();
			}
			else
			{
				name = event.getArgs();
				uID = null;
			}
			int season = 1;
			int minRank = Integer.MAX_VALUE;
			List<Pair<Integer,Long>> cashFigures = new LinkedList<>();
			//We're going to keep reading history files as long as they're there
			while(Files.exists(Paths.get("scores","history"+event.getChannel().getId()+"s"+season+".csv")))
			{
				//Load up the next one
				List<String> list = Files.readAllLines(
						Paths.get("scores","history"+event.getChannel().getId()+"s"+season+".csv"));
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
				}
				//Then move on to the next season
				season ++;
			}
			//So now we've gone through every past season and got a list of our scores, time to generate stats?
			StringBuilder output = new StringBuilder();
			output.append("```\n");
			output.append("History for "+name+"\n\n");
			//Loop through each season and calculate stats
			StringBuilder seasonList = new StringBuilder();
			int moneyWidth = minRank == 0 ? 17 : 13;
			int seasonsPlayed = 0;
			int wins = 0;
			long totalCash = 0;
			long maingameCash = 0;
			long bestResult = 0;
			for(Pair<Integer,Long> nextSeason : cashFigures)
			{
				seasonsPlayed ++;
				if(nextSeason.getRight() >= 1_000_000_000)
					wins ++;
				totalCash += nextSeason.getRight();
				maingameCash += Math.min(1_000_000_000, nextSeason.getRight());
				if(nextSeason.getRight() > bestResult)
					bestResult = nextSeason.getRight();
				seasonList.append(String.format("Season %1$d: $%2$,"+moneyWidth+"d\n",nextSeason.getLeft(),nextSeason.getRight()));
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
				if(wins > 0)
					output.append(String.format("Maingame Cash Earned: $%,d\n",maingameCash));
				output.append(String.format("Best Season Result: $%,d\n\n",bestResult));
				output.append(seasonList);
			}
			else
				output.append("No data found.");
			output.append("\n```");
			event.reply(output.toString());
		} catch (IOException e)
		{
			event.reply("This command must be used in a game channel.");
		}
	}

}
