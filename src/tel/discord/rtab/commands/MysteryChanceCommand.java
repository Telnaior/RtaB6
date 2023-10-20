package tel.discord.rtab.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import tel.discord.rtab.RtaBMath;

public class MysteryChanceCommand extends Command
{
	public MysteryChanceCommand()
	{
		this.name = "mysterychance";
		this.aliases = new String[]{"chance"};
		this.help = "randomises your score according to the mystery chance!";
		this.guildOnly = false;
		this.cooldown = 15;
	}
	@Override
	protected void execute(CommandEvent event)
	{
		int bananaCount = (int)(RtaBMath.random()*5)+1;
		event.reply("Congratulations, your new score is " + "\uD83C\uDF4C".repeat(bananaCount) + "!");
		
		/* "normal" code preserved below
		//95% chance of getting a nice score
		if(RtaBMath.random() < 0.95)
		{
			int newScore = (int)Math.pow(RtaBMath.random()*9+1,9);
			//5% chance of making it negative, because that's funny
			if(RtaBMath.random() < 0.05)
				newScore *= -1;
			//And tell them the result!
			event.reply(String.format("Congratulations, your new score is **$%,d**!", newScore));
		}
		else
		{
			event.reply("Your new score is **shinty-six**. That's Numberwang!");
		}
		*/
	}
}