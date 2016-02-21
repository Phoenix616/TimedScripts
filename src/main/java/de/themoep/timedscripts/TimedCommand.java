package de.themoep.timedscripts;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class TimedCommand {
    private final String command;
    private Set<String> variables = new HashSet<String>();

    public TimedCommand(String command) {
        command = command.trim();
        if(command.startsWith("/")) {
            command = command.substring(1);
        }
        Pattern pattern = Pattern.compile("\\%(\\w+?)\\%");
        Matcher matcher = pattern.matcher(command);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        this.command = command;
    }

    /**
     * Get the command string without any variables replaced
     * @return The pure command string
     */
    public String getCommand() {
        return getCommand(null);
    }

    /**
     * Get the command string with variables in it replaced
     * @param replacements The variable values
     * @return The command string; passing null or an empty Map will just get the unreplaced string
     */
    public String getCommand(Map<String, String> replacements) {
        String returnCommand = command;
        if(replacements != null) {
            for(Map.Entry<String, String> repl : replacements.entrySet()) {
                String key = repl.getKey();
                for(String var : variables) {
                    if(key.toLowerCase().equals(var.toLowerCase())) {
                        key = var;
                        break;
                    }
                }
                returnCommand = returnCommand.replace("%" + key + "%", repl.getValue());
            }
        }
        return returnCommand.replace("\\%", "%");
    }

    /**
     * Check whether or not all variables are represent in the replacement map
     * @param replacements A map of replacements
     * @return <tt>true</tt> if all variables have replacements; <tt>false</tt> if not
     */
    public boolean checkVariables(Map<String, String> replacements) {
        return checkVariables(replacements.keySet());
    }

    /**
     * Check whether or not all variables are represent in the replacement set
     * @param replacements A set of replacement variables
     * @return <tt>true</tt> if all variables have replacements; <tt>false</tt> if not
     */
    private boolean checkVariables(Set<String> replacements) {
        for(String var : variables) {
            if(!inSet(var, replacements)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check whether or not a string is in a set case insensitive
     * @param string The string to check
     * @param set The set to search through
     * @return Whether or not a string is in the set (case insensitive)
     */
    private boolean inSet(String string, Set<String> set) {
        for(String s : set) {
            if(s.equalsIgnoreCase(string)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a set of variables that are in this command
     * @return A set of variables
     */
    public Set<String> getVariables() {
        return new HashSet<String>(variables);
    }

    @Override
    public String toString() {
        return command;
    }
}
