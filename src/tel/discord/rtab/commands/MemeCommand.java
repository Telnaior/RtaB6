package tel.discord.rtab.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class MemeCommand extends Command
{
	public MemeCommand()
	{
		this.name = "meme";
		this.aliases = new String[]{"jo","realluckynumber","luckyletter","om","ub","starman","previous","instantbillion",
				"peak","peep","pee","eep","noij","nioj","ni","triforce"};
		this.help = "https://niceme.me";
		this.hidden = true;
		this.guildOnly = false;
	}
	@Override
	protected void execute(CommandEvent event)
	{
		event.reply("https://niceme.me");
	}
	
	/* +----------------+
	 * |CONGRATULATIONS!|
	 * |   YOU  FOUND   |
	 * | THE  TRIFORCE! |
	 * +----------------+
	 * 
	 *         /\               
	 *        /  \             
	 *       /    \           
	 *      /      \         
	 *      --------         
	 *     /\      /\       
	 *    /  \    /  \     
	 *   /    \  /    \   
	 *  /      \/      \ 
	 *  ---------------- 
	 * 
	 * Your reward is located here: https://www.youtube.com/watch?v=dQw4w9WgXcQ 
	 */
}
