package de.themoep.timedscripts;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * TimedScripts
 * Copyright (C) 2015 Max Lee (https://github.com/Phoenix616/)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

public class TimedScripts extends JavaPlugin {

    private ScriptManager scriptManager;

    public void onEnable() {
        saveDefaultConfig();
        scriptManager = new ScriptManager(this);
        getCommand("timedscript").setExecutor(new TimedScriptCommand(this));
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if("timedscripts".equalsIgnoreCase(cmd.getName())) {
            if(args.length > 0) {
                if(sender.hasPermission("TimedScripts.admin")) {
                    boolean stop = false;
                    boolean reload = false;
                    for(String arg : args) {
                        if("reload".equalsIgnoreCase(arg)) {
                            reload = true;
                        } else if("stop".equalsIgnoreCase(args[0])) {
                            stop = true;
                        }
                    }
                    if(stop) {
                        scriptManager.destroy();
                        sender.sendMessage(ChatColor.YELLOW + "All running scripts stopped!");
                    }
                    if(reload) {
                        reloadConfig();
                        scriptManager = new ScriptManager(this);
                        sender.sendMessage(ChatColor.GREEN + "Scripts reloaded!");
                    }
                    if(!stop && !reload) {
                        return false;
                    }
                } else {
                    sender.sendMessage("You don't have the permission TimedScripts.admin");
                }
            } else {
                sender.sendMessage(ChatColor.AQUA + "List of TimedScripts:");
                if(getScriptManager().getScripts().size() > 0) {
                    for(TimedScript script : getScriptManager().getScripts()) {
                        sender.sendMessage(" " + script.getName() + " by " + script.getCreatorName());
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "None");
                }
            }
        }
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if(!"timedscripts".equalsIgnoreCase(cmd.getName())) {
            return null;
        }
        return args.length == 0 || (args.length == 1 && "reload".startsWith(args[0].toLowerCase())) ? Collections.singletonList("reload") : new ArrayList<String>();
    }

}
