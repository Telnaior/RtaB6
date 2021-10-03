package tel.discord.rtab.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.jagrosh.jdautilities.command.CommandEvent;

import tel.discord.rtab.GameController;
import tel.discord.rtab.Player;
import tel.discord.rtab.RaceToABillionBot;
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
		//First let's check if they've picked a game to enhance
		String gameName = event.getArgs();
		//Run through the list of games to find the one they asked for
		for(Game game : Game.values())
		{
			if((!game.isBonus() || event.isOwner()) && gameName.equalsIgnoreCase(game.getShortName()))
			{
				enhanceGame(event, game);
				return;
			}
		}
		//If we didn't find it, let's figure out what they do intend and go from there
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
					if(enhancedList[0].length() > 0)
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
	
	void enhanceGame(CommandEvent event, Game game)
	{
		//First of all, is the game actually in this season?
		if(game.getWeight(4) <= 0)
		{
			event.reply("That minigame is not available this season.");
			return;
		}
		//Find the channel
		GameController controller = null;
		for(GameController next : RaceToABillionBot.game)
			if(next.channel.equals(event.getChannel()))
			{
				controller = next;
				break;
			}
		if(controller == null)
		{
			event.reply("This is not a game channel.");
			return;
		}
		//Check that they're in game currently
		if(controller.players.size() <= 0)
		{
			event.reply("You must be in-game in order to enhance a minigame.");
			return;
		}
		boolean playerFound = false;
		for(Player next : controller.players)
			if(next.uID.equals(event.getAuthor().getId()))
			{
				playerFound = true;
				if(next.getEnhanceCap() <= next.enhancedGames.size())
				{
					event.reply("You currently have no open enhance slots.");
					return;
				}
				for(Game nextGame : next.enhancedGames)
				{
					if(game == nextGame)
					{
						event.reply("You have already enhanced that minigame.");
						return;
					}
				}
				//Yay they actually met all the conditions was that so hard
				next.enhancedGames.add(game);
				event.reply("Minigame enhanced!");
			}
		if(!playerFound)
		{
			event.reply("You must be in-game in order to enhance a minigame.");
			return;
		}
	}
}
