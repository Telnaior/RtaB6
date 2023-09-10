package tel.discord.rtab.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import tel.discord.rtab.GameController;
import tel.discord.rtab.Player;
import tel.discord.rtab.RaceToABillionBot;


public class HiddenCommandCommand extends Command //How meta
{
	public HiddenCommandCommand()
    {
        this.name = "hiddencommand";
        this.aliases = new String[]{"hc"};
        this.help = "remind yourself what hidden command you possess";
        this.guildOnly = true;
    }
	@Override
	protected void execute(CommandEvent event)
	{
		//Start by checking to see if they're in-game, and read from their player-file instead
		Player player = null;
		//Find the channel
		GameController controller = null;
		for(GameController next : RaceToABillionBot.game)
			if(next.channel.getId().equals(event.getChannel().getId()))
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
		//At this point we've found them, so just prompt the reminder method
		player.remindHiddenCommand(true);
	}
}
