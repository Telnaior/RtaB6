package tel.discord.rtab.commands.mod;

import tel.discord.rtab.GameController;
import tel.discord.rtab.RaceToABillionBot;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.Permission;

public class DemoCommand extends Command {

	public DemoCommand()
	{
		this.name = "demo";
		this.help = "starts a game with four bots";
		this.hidden = true;
		this.userPermissions = new Permission[] {Permission.MANAGE_SERVER};
	}
	@Override
	protected void execute(CommandEvent event)
	{
		int playerCount = 4;
		if(!event.getArgs().equals(""))
			playerCount = Integer.parseInt(event.getArgs());
		for(GameController game : RaceToABillionBot.game)
		{
			if(game.channel.equals(event.getChannel()))
			{
				if(game.runDemo != 0)
					game.demoMode.cancel(true);
				for(int i=0; i<playerCount; i++)
				{
					game.addRandomBot();
				}
				game.startTheGameAlready();
				//We found the right channel, so
				return;
			}
		}
	}
}