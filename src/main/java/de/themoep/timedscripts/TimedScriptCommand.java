package de.themoep.timedscripts;

import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * TimedScripts
 * Copyright (C) 2016 Max Lee (https://github.com/Phoenix616/)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Mozilla Public License as published by
 * the Mozilla Foundation, version 2.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Mozilla Public License v2.0 for more details.
 * <p/>
 * You should have received a copy of the Mozilla Public License v2.0
 * along with this program. If not, see <http://mozilla.org/MPL/2.0/>.
 */
public class TimedScriptCommand implements CommandExecutor {
    private final TimedScripts plugin;

    public TimedScriptCommand(TimedScripts plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            sender.sendMessage(ChatColor.YELLOW + "TimedScripts command usage:");
            sender.sendMessage("/timedscripts - Lists all scripts");
            for(Action action : Action.values()) {
                sender.sendMessage(action.getUsage(label));
            }
            return true;
        }

        try {
            Action action = Action.valueOf(args[0].toUpperCase());
            if(args.length == 1) {
                sender.sendMessage(ChatColor.RED + "Usage: " + action.getUsage(label));
                return true;
            }
            TimedScript script = plugin.getScriptManager().getScript(args[1]);
            if(action == Action.CREATE) {
                if(!sender.hasPermission("TimedScripts.command.create")) {
                    sender.sendMessage(ChatColor.RED + "You don't have the permissions TimedScripts.command.create");
                    return true;
                }
                plugin.getScriptManager().newScript(args[1], sender);
                return true;
            }
            if(script == null) {
                sender.sendMessage(ChatColor.RED + "Could not find a script by the name of " + ChatColor.YELLOW + args[1]);
                return true;
            }
            List<String> argList = new ArrayList<String>();
            argList.addAll(Arrays.asList(args).subList(2, args.length));

            if(!runAction(sender, action, script, argList.toArray(new String[argList.size()]))) {
                sender.sendMessage(ChatColor.RED + "Usage: " + action.getUsage(label));
            }
        } catch(IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "The action " + args[0].toUpperCase() + " is unknown!");
            return false;
        }
        return true;
    }

    private boolean runAction(CommandSender sender, Action action, TimedScript script, String[] args) {
        String perm = "TimedScripts.command." + action.toString().toLowerCase();
        if(!sender.hasPermission(perm)) {
            sender.sendMessage(ChatColor.RED + "You don't have the permissions " + perm);
            return true;
        }
        if(action == Action.EDIT) {
            if(args.length == 0) {
                return false;
            }
            try {
                EditAction editAction = EditAction.valueOf(args[0]);
                try {
                    if(args.length == 1) {
                        sender.sendMessage(ChatColor.RED + "Usage: " + editAction.getUsage());
                        return true;
                    }
                    double time = Double.valueOf(args[1]);

                    List<String> argList = new ArrayList<String>();
                    argList.addAll(Arrays.asList(args).subList(2, args.length));
                    if(!runEditAction(sender, editAction, time, script, argList.toArray(new String[argList.size()]))) {
                        sender.sendMessage(ChatColor.RED + "Usage: " + editAction.getUsage());
                    }

                } catch(NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Error: " + args[1] + " is not a valid double number input!");
                    sender.sendMessage(ChatColor.RED + "Usage: " + editAction.getUsage());
                }
            } catch(IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "The action " + args[0].toUpperCase() + " is unknown!");
                return false;
            }

        } else if(action == Action.RUN) {
            if(sender instanceof Player && !((Player) sender).getUniqueId().equals(script.getCreatorId()) && !sender.hasPermission("TimedScripts.command.run." + script.getName().toLowerCase())) {
                sender.sendMessage(ChatColor.RED + "You don't have the permissions to run other people's scripts! (TimedScripts.command.run.others)");
                return true;
            }
            plugin.getScriptManager().runScript(sender, script);
            sender.sendMessage(ChatColor.GREEN + "Started script " + ChatColor.YELLOW + script.getName());

        } else if(action == Action.INFO) {
            int commandCount = 0;
            for(List<TimedCommand> commands : script.getCommands().values()) {
                commandCount += commands.size();
            }
            sender.sendMessage(new String[]{
                    ChatColor.GREEN + "Info for script " + ChatColor.YELLOW + script.getName() + ChatColor.GREEN + ":",
                    ChatColor.GREEN + "Creator: " + ChatColor.YELLOW + script.getCreatorName() + ChatColor.GREEN + "(" + script.getCreatorId() + ")",
                    ChatColor.GREEN + "Contains " + ChatColor.YELLOW + commandCount + ChatColor.GREEN + " command" + (commandCount != 1 ? "s" : "") + " at " + ChatColor.YELLOW + script.getCommands().size() + ChatColor.GREEN + " different times!"
            });

        } else if(action == Action.VIEW) {
            if(script.getCommands().size() == 0) {
                sender.sendMessage(ChatColor.RED + "The script " + ChatColor.YELLOW + script.getName() + ChatColor.RED + " does not have any commands defined yet!");
                return true;
            }
            List<String> commandList = new ArrayList<String>();
            double time = -1;
            if(args.length > 0) {
                try {
                    time = Double.valueOf(args[0]);
                    List<TimedCommand> commands = script.getCommands(time);
                    if(commands == null || commands.size() == 0) {
                        sender.sendMessage(ChatColor.RED + "The script " + ChatColor.YELLOW + script.getName() + ChatColor.RED + " does not have any commands at " + ChatColor.YELLOW + time);
                        return true;
                    }
                    for(int i = 0; i < commands.size(); i++) {
                        TimedCommand command = commands.get(i);
                        commandList.add(ChatColor.YELLOW + "#" + i + " " + ChatColor.DARK_GRAY + Utils.formatTime(time) + ": " + ChatColor.GRAY + command);
                    }
                } catch(NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Error: " + args[0] + " is not a valid double number input!");
                    return true;
                }
            } else {
                for(Map.Entry<Double, List<TimedCommand>> entry : script.getCommands().entrySet()) {
                    for(int i = 0; i < entry.getValue().size(); i++) {
                        TimedCommand command = entry.getValue().get(i);
                        commandList.add(ChatColor.YELLOW + "#" + i + " " + ChatColor.DARK_GRAY + Utils.formatTime(entry.getKey()) + ": " + ChatColor.GRAY + command);
                    }
                }
            }
            sender.sendMessage(ChatColor.GREEN + "Commands of " + ChatColor.YELLOW + script.getName() + ChatColor.GREEN + (time == -1 ? ":" : " at " + Utils.formatTime(time) + ":"));
            sender.sendMessage(commandList.toArray(new String[commandList.size()]));

        } else if(action == Action.SAVE) {
            if(script.save()) {
                sender.sendMessage(ChatColor.GREEN + "Script " + ChatColor.YELLOW + script.getName() + ChatColor.GREEN + " saved!");
            } else {
                sender.sendMessage(ChatColor.RED + "An error occurred while saving the script " + script.getName() + "! Please take a look at the exception in the log.");
            }

        } else {
            sender.sendMessage(ChatColor.RED + "Action " + action + " is not implemented yet!");
        }
        return true;
    }

    private enum Action {
        CREATE,
        EDIT("[add <time> <cmd>|set <time> <#> <cmd>|remove <time> [<#>]]"),
        RUN("[<var=value> ...]"),
        INFO,
        VIEW("[<time>]"),
        SAVE,
        DELETE;

        private final String usage;

        Action() {
            this.usage = toString().toLowerCase();
        }

        Action(String usage) {
            this.usage = toString().toLowerCase() + " <scriptname> " + usage;
        }

        public String getUsage() {
            return getUsage("timedscript");
        }

        public String getUsage(String label) {
            return "/" + label + " " + usage;
        }
    }

    private boolean runEditAction(CommandSender sender, EditAction action, double time, TimedScript script, String[] args) {
        if(sender instanceof Player && !((Player) sender).getUniqueId().equals(script.getCreatorId()) && !sender.hasPermission("TimedScripts.command.edit.others")) {
            sender.sendMessage(ChatColor.RED + "You don't have the permissions to edit other people's scripts! (TimedScripts.command.edit.others)");
            return true;
        }

        if(action == EditAction.ADD) {
            if(args.length == 0) {
                return false;
            }
            String command = StringUtils.join(args, ' ');
            if(script.addCommand(time, command)) {
                sender.sendMessage(ChatColor.GREEN + "Added the following command to script " + ChatColor.YELLOW + script.getName() + ChatColor.GREEN + " at position " + (script.getCommands(time).size() - 1) + " :");
            } else {
                sender.sendMessage(ChatColor.RED + "Error while trying to add command to script " + ChatColor.YELLOW + script.getName() + ChatColor.RED + "! Take a look at the log for the exact error!");
                script.getCommands(time).remove(script.getCommands(time).size() - 1);
            }
            sender.sendMessage(ChatColor.DARK_GRAY + Utils.formatTime(time) + ": " + ChatColor.GRAY + command);

        } else if(action == EditAction.SET) {
            if(args.length < 2) {
                return false;
            }
            try {
                int i = Integer.parseInt(args[0]);
                StringBuilder commandString = new StringBuilder(args[1]);
                for(int j = 2; j < args.length; j++) {
                    commandString.append(" ").append(args[j]);
                }
                TimedCommand command = script.setCommand(time, i, commandString.toString());
                if(command != null) {
                    sender.sendMessage(ChatColor.GREEN + "Set the command for script " + ChatColor.YELLOW + script.getName() + ChatColor.GREEN + " at position " + i + " :");
                    sender.sendMessage(ChatColor.YELLOW + "Old #" + i + " " + ChatColor.DARK_GRAY + Utils.formatTime(time) + ": " + ChatColor.GRAY + command);
                    sender.sendMessage(ChatColor.YELLOW + "New #" + i + " " + ChatColor.DARK_GRAY + Utils.formatTime(time) + ": " + ChatColor.GRAY + commandString);
                } else {
                    sender.sendMessage(ChatColor.RED + "Error while setting the command. Are you sure there was one at position " + ChatColor.YELLOW + i + ChatColor.RED + "? Maybe try using " + EditAction.ADD.getUsage() + " instead.");
                }

            } catch(NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Error: " + args[0] + " is not a valid integer number input!");
                return false;
            }

        } else if(action == EditAction.REMOVE) {
            if(args.length == 0) {
                return false;
            }
            try {
                int i = Integer.parseInt(args[0]);
                TimedCommand command = script.removeCommand(time, i);
                if(command != null) {
                    sender.sendMessage(ChatColor.GREEN + "Removed the following command from script " + ChatColor.YELLOW + script.getName() + ChatColor.GREEN + ":");
                    sender.sendMessage(ChatColor.YELLOW + "#" + i + " " + ChatColor.DARK_GRAY + Utils.formatTime(time) + ": " + ChatColor.GRAY + command);
                } else {
                    sender.sendMessage(ChatColor.RED + "The script " + ChatColor.YELLOW + script.getName() + ChatColor.RED + " does not have a command at the " + ChatColor.YELLOW + i + "." + ChatColor.RED + " position at " + ChatColor.YELLOW + time + ChatColor.RED + " seconds!");
                }
            } catch(NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Error: " + args[0] + " is not a valid integer number input!");
                return false;
            }

        } else {
            sender.sendMessage(ChatColor.RED + "Edit action " + action + " is not implemented yet!");
        }
        return true;
    }

    private enum EditAction {
        ADD("<time> <cmd>"),
        SET("<time> <#> <cmd>"),
        REMOVE("<time> <#>");

        private final String usage;

        EditAction() {
            this.usage = toString().toLowerCase();
        }

        EditAction(String usage) {
            this.usage = toString().toLowerCase() + " " + usage;
        }

        public String getUsage() {
            return "/timedscript edit <scriptname> " + usage;
        }
    }
}
