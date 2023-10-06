package tel.discord.rtab.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import tel.discord.rtab.RtaBMath;

public class LuckyNumberCommand extends Command
{
	public LuckyNumberCommand()
	{
		this.name = "luckynumber";
		this.aliases = new String[]{"lucky"};
		this.help = "gives you a random number";
		this.guildOnly = false;
		this.cooldown = 15;
	}
	@Override
	protected void execute(CommandEvent event)
	{
		//80% chance of getting the (officially designated) luckiest of all numbers
		if(RtaBMath.random() < 0.8)
		{
			//10% chance of giving it as a reaction instead of a message
			if(RtaBMath.random() < 0.1)
			{
				//Github breaks the emoji if I don't escape it lol
				event.getMessage().addReaction(Emoji.fromUnicode("\u0039\uFE0F\u20E3")).queue();
			}
			else
			{
				event.reply("Your lucky number is "+(RtaBMath.random() < 0.055 ? ":nine:" : "9")+".");
			}
		}
		//0.02% chance you "win"
		//And no, source-code readers, you can't cheat. :P (But you can grind it out in private if you really want)
		else if(RtaBMath.random() < 0.001)
		{
			int secretCode = (int) (RtaBMath.random() * 1000000);
			event.reply("Congratulations, you win! "
					+ String.format("Quote %06d to @telna to add a new lucky number to the bot!",secretCode));
			System.out.printf("Secret Lucky Number Code: %06d%n",secretCode);
		}
		//Otherwise pick one of the extra texts
		//Here be spoilers!
		else
		{
			int chosenText = (int) (RtaBMath.random() * 11);
			switch (chosenText) {
				case 0 -> event.reply("Your lucky number is -1.");
				case 1 -> event.reply("Your lucky number is \u03C0.");
				case 2 ->
						event.reply("Your lucky number is _\\*flips a coin*_ " + (RtaBMath.random() < 0.5 ? "heads" : "tails") + ".");
				case 3 -> {
					event.reply("Your lucky number is a **BOMB**.");
					event.reply("It goes " + (RtaBMath.random() < 0.04 ? "_\\*fizzle*_." : "**BOOM**. $250,000 penalty."));
				}
				case 4 -> event.reply("This fortune cookie is delicious! Unfortunately, you ate the lucky number.");
				case -1 ->
						event.reply("You found the triforce! Go here to claim it: <https://www.youtube.com/watch?v=3KANI2dpXLw>");
				case 5 -> event.reply("Your lucky number is " + RtaBMath.random() + ".");
				case 6 -> event.reply("Come on, lucky seven! Oops, wrong game.");
				case 7 -> event.reply("Your lucky number is ~~9~~ SEVEN IT'S 7 YOUR LUCKY NUMBER IS 7 #TEAM7");
				case 8 -> event.reply("Your lucky number is C4.");
				case 9 -> //Added by aug
						event.reply("Your lucky number is 69420. :smirk:");
				case 10 -> //Added by ATMunn
						event.reply("Your lucky number is -1/12.");
			}
		}
	}
}
