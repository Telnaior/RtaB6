package tel.discord.rtab.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        this.aliases = new String[]{"enh","enhancement","enhancements","ench","enchant","enchantment","enchantments"};
        this.help = "choose minigames to enhance";
        this.guildOnly = true;
    }

	@Override
	protected void execute(CommandEvent event)
	{
		//Check if they just want the list
		if(event.getArgs().equalsIgnoreCase("list"))
		{
			sendEnhanceList(event);
			return;
		}
		//Next check if they've picked a game to enhance
		String gameName = event.getArgs();
		//Run through the list of games to find the one they asked for
		for(Game game : Game.values())
		{
			if(gameName.equalsIgnoreCase(game.getShortName()) || gameName.equalsIgnoreCase(game.getName()))
			{
				enhanceGame(event, game);
				return;
			}
		}
		//If they don't seem to be enhancing a minigame, just send them their status
		//Start by checking to see if they're in-game, and read from their player-file instead
		Player player = null;
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
			event.reply("This command must be used in a game channel.");
			return;
		}
		for(Player next : controller.players)
			if(next.uID.equals(event.getAuthor().getId()))
			{
				player = next;
				break;
			}
		//If they aren't in-game, get their data by generating their player object
		if(player == null)
		{
			player = new Player(event.getMember(), controller, null);
		}
		//Now let's start building up the reply message
		StringBuilder output = new StringBuilder();
		//List their enhance slot status
		output.append("```\n").append(player.getName()).append("'s Enhanced Minigames:\n");
		boolean emptySlots = false;
		for(int i=0; i<player.getEnhanceCap(); i++)
		{
			if(i < player.enhancedGames.size())
			{
				output.append(" (*) ");
				output.append(player.enhancedGames.get(i).getName());
			}
			else
			{
				output.append(" ( ) Empty Slot");
				emptySlots = true;
			}
			output.append("\n");
		}
		int livesToNewSlot = (25 * (player.getEnhanceCap()+1) * (player.getEnhanceCap()+2) / 2) - player.totalLivesSpent;
		if(player.newbieProtection > 0)
			output.append("     (Finish your newbie protection, then use ").append(livesToNewSlot).append(" lives to open a new slot)\n\n");
		else
			output.append("     (Use ").append(livesToNewSlot).append(" more lives to open a new slot)\n\n");
		output.append("Type '!enhance list' to see the list of available enhancements.\n");
		if(emptySlots)
			output.append("Type '!enhance' followed by a minigame's name to permanently enhance that minigame.\n");
		output.append("```");
		event.reply(output.toString());
	}
	
	void sendEnhanceList(CommandEvent event)
	{
		StringBuilder output = new StringBuilder();
		output.append("```\nENHANCEABLE MINIGAMES:\n");
		for(Game next : Game.values())
		{
			if(next.getWeight(4) > 0)
			{
				output.append("\n").append(next.getShortName()).append(" - ").append(next.getName()).append("\n").append(next.getEnhanceText()).append("\n");
			}
		}
		output.append("```");
		event.replyInDm(output.toString());
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
			event.reply("This command must be used in a game channel.");
			return;
		}
		//Check that they're in game currently, or that there's no game running
		if(controller.players.size() <= 0)
		{
			//If there's no game running, find them in the savefile
			try
			{
				List<String> list = Files.readAllLines(Paths.get("scores","scores"+event.getChannel().getId()+".csv"));
				int index = findUserInList(list,event.getAuthor().getId(),false);
				if(index < 0 || index >= list.size())
				{
					event.reply("You currently have no open enhance slots.");
					return;
				}
				String[] record = list.get(index).split("#");
				/*
				 * record[11] = total lives spent
				 * record[12] = list of enhanced minigames
				 * (this is copied directly from the player initialisation file)
				 */
				int totalLivesSpent = Integer.parseInt(record[11]);
				ArrayList<Game> enhancedGames = new ArrayList<>();
				String savedEnhancedGames = record[12].substring(1, record[12].length() - 1); //Remove the brackets
				String[] enhancedList = savedEnhancedGames.split(",");
				if(enhancedList[0].length() > 0)
                    for (String s : enhancedList) enhancedGames.add(Game.valueOf(s.trim()));
				//Do the obvious checks
				if(RtaBMath.getEnhanceCap(totalLivesSpent) <= enhancedGames.size())
				{
					event.reply("You currently have no open enhance slots.");
					return;
				}
				for(Game nextGame : enhancedGames)
				{
					if(game == nextGame)
					{
						event.reply("You have already enhanced that minigame.");
						return;
					}
				}
				//Enhance it!
				enhancedGames.add(game);
				event.reply("Minigame enhanced!");
				//Now replace the record in the list
				StringBuilder updatedLine = new StringBuilder();
				for(int i=0; i<11; i++)
				{
					updatedLine.append(record[i]);
					updatedLine.append("#");
				}
				updatedLine.append(totalLivesSpent);
				updatedLine.append("#");
				updatedLine.append(enhancedGames);
				list.set(index, updatedLine.toString());
				//And save it
				Path file = Paths.get("scores","scores"+event.getChannel().getId()+".csv");
				Path oldFile = Files.move(file, file.resolveSibling("scores"+event.getChannel().getId()+"old.csv"));
				Files.write(file, list);
				Files.delete(oldFile);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
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
			event.reply("There is a game currently in progress; please wait until it is finished to enhance.");
			return;
		}
	}
}
