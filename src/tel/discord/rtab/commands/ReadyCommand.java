package tel.discord.rtab.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import tel.discord.rtab.RaceToABillionBot;
import tel.discord.rtab.SuperBotChallenge;

public class ReadyCommand extends Command
{
	public ReadyCommand()
	{
		this.name = "ready";
		this.help = "Indicate your readiness to play a round in the Super Bot Challenge";
		this.guildOnly = false;
	}
	@Override
	protected void execute(CommandEvent event)
	{
		//Just find the right channel and pass the player id on to the appropriate method
		for(SuperBotChallenge challenge : RaceToABillionBot.challenge)
		{
			if(challenge.channel.equals(event.getChannel()))
			{
				if(challenge.loadingHumanGame)
					challenge.readyUp(event.getAuthor().getId());
				else
					challenge.searchForHumanGame(event.getAuthor().getId());
			}
		}
	}
}