package hawox.uquest.commands;


import hawox.uquest.UQuest;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Cmd_reloadquestconfig implements CommandExecutor{
	private final UQuest plugin;
	
	public Cmd_reloadquestconfig(UQuest plugin){
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		boolean process = false;
		if(sender instanceof Player){
			player = (Player)sender;
			}else{
				//Console can use this command
				process = true;
			}

		if(player != null){
			if(plugin.isUsePermissions() == true){
				try{
				if(UQuest.getPermissions().has(player, "uQuest.CanReloadQuestConfig")){
					process = true;
				}
				}catch(NoClassDefFoundError ncdfe){
					//they don't have permissions so disable it plugin wide
					plugin.setUsePermissions(false);
					System.err.println(UQuest.pluginNameBracket() + " Failed to access Permissions plugin. Disabling support for it.");
				}
			}//Ops can use it too! Just in case we're not using permissions.
			if(player.isOp()){
				process = true;
			}
		}
		
		//Actual command starts here
		if(process == true){
			plugin.readConfig();
			
			sender.sendMessage(UQuest.pluginNameBracket() + " I hope you didn't change anything under 'Database' or 'PluginSupport'!!!");
			sender.sendMessage(UQuest.pluginNameBracket() + " These will not reconfigure mid runtime and may cause UNDESIRED RESULTS!!!");
			
			sender.sendMessage(UQuest.pluginNameBracket() + " uQuest's config has been reloaded.");
			
			if(player != null){
				System.out.println(UQuest.pluginNameBracket() + " " + player.getName() + " reloaded uQuest's config.");
			}
	    }else{
	    	player.sendMessage(ChatColor.RED + "You don't have permission to use that!");
	    }
		return true;
	}
}