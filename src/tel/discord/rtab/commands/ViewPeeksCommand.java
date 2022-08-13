package tel.discord.rtab.commands;

import tel.discord.rtab.GameController;
import tel.discord.rtab.Player;
import tel.discord.rtab.RaceToABillionBot;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class ViewPeeksCommand extends Command {
	public ViewPeeksCommand()
	{
		this.name = "viewpeeks";
		this.help = "See what spaces have been peeked so far";
		this.aliases = new String[] {"peeks","peeklist","listpeeks"};
	}
	@Override
	protected void execute(CommandEvent event) {
		for(GameController game : RaceToABillionBot.game)
		{
			if(game.channel.equals(event.getChannel()))
			{
				StringBuilder output = new StringBuilder();
				output.append("**Current Peeks**\n");
				for(Player nextPlayer : game.players)
				{
					output.append(nextPlayer.printPeeks()).append("\n");
				}
				event.reply(output.toString());
				//We found the right channel, so
				return;
			}
		}
	}

}
