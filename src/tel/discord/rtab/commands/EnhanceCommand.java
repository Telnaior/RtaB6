package tel.discord.rtab.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.jagrosh.jdautilities.command.CommandEvent;

import tel.discord.rtab.RtaBMath;
import tel.discord.rtab.board.Game;

public class EnhanceCommand extends ParsingCommand
{
	public EnhanceCommand()
    {
        this.name = "enhance";
        this.aliases = new String[]{"enh"};
        this.help = "choose minigames to enhance";
        this.guildOnly = true;
    }

	@Override
	protected void execute(CommandEvent event)
	{
		try
		{
			List<String> list = Files.readAllLines(Paths.get("scores","scores"+event.getChannel().getId()+".csv"));
			switch(event.getArgs().toUpperCase())
			{
				case "LIST":
					break;
				
				default:
					//List their enhance slot status
					int index = findUserInList(list,event.getAuthor().getId(),false);
					if(index < 0 || index >= list.size())
					{
						event.reply("You haven't played the game yet.");
						return;
					}
					String[] record = list.get(index).split("#");
					/*
					 * parse stuff into formats we can use
					 * record[11] = total lives spent
					 * record[12] = list of enhanced games
					 */
					ArrayList<Game> enhancedGames = new ArrayList<Game>();
					int livesSpent = Integer.parseInt(record[11]);
					int enhanceSlots = RtaBMath.getEnhanceCap(livesSpent);
					String savedEnhancedGames = record[12].substring(1, record[12].length() - 1); //Remove the brackets
					String[] enhancedList = savedEnhancedGames.split(",");
					for(int j=0; j<enhancedList.length; j++)
						enhancedGames.add(Game.valueOf(enhancedList[j]));
					//Now let's start building up the reply message
					StringBuilder output = new StringBuilder();
					output.append("```\n"+record[1]+"'s Enhanced Minigames:\n");
					boolean emptySlots = false;
					for(int i=0; i<enhanceSlots; i++)
					{
						if(i < enhancedGames.size())
						{
							output.append(" (*) ");
							output.append(enhancedGames.get(i).getName());
						}
						else
						{
							output.append(" ( ) Empty Slot");
							emptySlots = true;
						}
						output.append("\n");
					}
					int livesToNewSlot = (25 * (enhanceSlots+1) * (enhanceSlots+2) / 2) - livesSpent;
					output.append("     (Use "+livesToNewSlot+" more lives to open a new slot)\n\n");
					output.append("Type '!enhance list' to see the list of available enhancements.\n");
					if(emptySlots)
						output.append("Type '!enhance' followed by a minigame's name to permanently enhance that minigame.\n");
					output.append("```");
					event.reply(output.toString());
					break;
			}
		}
		catch(IOException e)
		{
			event.reply("This command must be used in a game channel.");
		}
	}

}
