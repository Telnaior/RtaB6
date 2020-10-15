package tel.discord.rtab.commands;

import tel.discord.rtab.GameController;
import tel.discord.rtab.RaceToABillionBot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.internal.utils.tuple.MutablePair;

public class AnnuitiesCommand extends ParsingCommand {
	public AnnuitiesCommand()
    {
        this.name = "annuities";
		this.aliases = new String[]{"annuity","ftrots","whatevertheftrotscommandiscalled","howmuchmoneyamimaking"};
        this.help = "displays your current annuities";
        this.guildOnly = true;
    }
	
	@Override
	protected void execute(CommandEvent event)
	{
		for(GameController game : RaceToABillionBot.game)
		{
			if(game.channel.equals(event.getChannel()))
			{
				try
				{
					List<String> list = Files.readAllLines(Paths.get("scores","scores"+event.getChannel().getId()+".csv"));
					//If no name given, check it for themselves
					int index;
					if(event.getArgs() == "")
						index = findUserInList(list,event.getAuthor().getId(),false);
					//If it's a mention, search by the id of the mention
					else if(event.getArgs().startsWith("<@!"))
					{
						String mentionID = parseMention(event.getArgs());
						index = findUserInList(list,mentionID,false);
					}
					//Otherwise check it for the player named
					else
					{
						index = findUserInList(list,event.getArgs(),true);
					}
					//Tell them they're not real if they aren't
					if(index < 0 || index >= list.size())
						event.reply("User not found.");
					else
					{
						//Parse the array the same way we do when joining a game
						String[] record = list.get(index).split("#");
						String savedAnnuities = record[10];
						savedAnnuities = savedAnnuities.replaceAll("[^\\d,-]", "");
						String[] annuityList = savedAnnuities.split(",");
						LinkedList<MutablePair<Integer,Integer>> annuities = new LinkedList<>();
						for(int j=1; j<annuityList.length; j+=2)
							annuities.add(MutablePair.of(Integer.parseInt(annuityList[j-1]), Integer.parseInt(annuityList[j])));
						//Start building our response
						StringBuilder output = new StringBuilder().append("```\n"+record[1]+"'s Annuities:\n");
						if(annuities.size() == 0)
						{
							output.append("You have no annuities.\n");
						}
						else
						{
							//Run through the iterator and tally up the payments
							ListIterator<MutablePair<Integer, Integer>> iterator = annuities.listIterator();
							while(iterator.hasNext())
							{
								MutablePair<Integer,Integer> nextAnnuity = iterator.next();
								output.append(String.format("$%,d: ",nextAnnuity.getLeft()));
								if(nextAnnuity.getRight() == -1)
									output.append("For the Rest of the Season\n");
								else
									output.append(String.format("%d spaces\n", nextAnnuity.getRight()));
							}
						}
						output.append("```");
						event.reply(output.toString());
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				//We found the right channel, so
				return;
			}
		}
	}
}
