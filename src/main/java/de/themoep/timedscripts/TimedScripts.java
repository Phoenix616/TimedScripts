package de.themoep.timedscripts;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

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
        this.scriptManager = new ScriptManager(this);
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if("timedscripts".equalsIgnoreCase(cmd.getName())) {
            return false;
        } else if ("timedscript".equalsIgnoreCase(cmd.getName())){
            if(args.length > 0) {
                if("run".equalsIgnoreCase(args[0])) {
                    if(args.length > 1) {
                        if(getScriptManager().runScript(sender, args[1])) {
                            sender.sendMessage(ChatColor.GREEN + "Started script " + ChatColor.YELLOW + args[1]);
                        } else {
                            sender.sendMessage(ChatColor.RED + "Could not find a script by the name of " + ChatColor.YELLOW + args[1]);
                        }
                    } else {
                        sender.sendMessage("Usage: /" + label + " run <scriptname> [<var=value> ...]");
                    }
                }
            }
            return false;
        }
        return false;
    }

}
