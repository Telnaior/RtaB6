package tel.discord.rtab.commands.mod;

import tel.discord.rtab.GameController;
import tel.discord.rtab.RaceToABillionBot;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.Permission;

public class ResetCommand extends Command
{
	public ResetCommand()
	{
		this.name = "reset";
		this.help = "resets the game state, in case something gets bugged";
		this.hidden = true;
		this.userPermissions = new Permission[] {Permission.BAN_MEMBERS};
	}
	@Override
	protected void execute(CommandEvent event)
	{
		for(GameController game : RaceToABillionBot.game)
		{
			if(game.channel.equals(event.getChannel()))
			{
				game.reset();
				return;
			}
		}
	}
}