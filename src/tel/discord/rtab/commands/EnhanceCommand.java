package tel.discord.rtab.commands;

import com.jagrosh.jdautilities.command.CommandEvent;

import tel.discord.rtab.GameController;
import tel.discord.rtab.Player;
import tel.discord.rtab.RaceToABillionBot;
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
		output.append("```\n"+player.getName()+"'s Enhanced Minigames:\n");
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
		output.append("     (Use "+livesToNewSlot+" more lives to open a new slot)\n\n");
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
				output.append("\n"+next.getShortName()+" - "+next.getName()+"\n"+next.getEnhanceText()+"\n");
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
