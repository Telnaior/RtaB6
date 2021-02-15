package tel.discord.rtab.commands.mod;

import tel.discord.rtab.GameController;
import tel.discord.rtab.RaceToABillionBot;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.Permission;

public class GridListCommand extends Command
{
	public GridListCommand()
	{
		this.name = "gridlist";
		this.help = "See the list of spaces on the board (can only use when not in game)";
		this.hidden = true;
		this.userPermissions = new Permission[] {Permission.BAN_MEMBERS};
	}
	@Override
	protected void execute(CommandEvent event) {
		for(GameController game : RaceToABillionBot.game)
		{
			if(game.channel.equals(event.getChannel()))
			{
				//Hardcoded exception so we can still use this in the testing channel lol
				if(game.findPlayerInGame(event.getAuthor().getId()) != -1 && !game.channel.getId().equals("466545561743654924"))
					event.reply("You can't view the grid list for a game you're in!");
				else
					event.replyInDm(game.gridList(false));
				//We found the right channel, so
				return;
			}
		}
	}

}
