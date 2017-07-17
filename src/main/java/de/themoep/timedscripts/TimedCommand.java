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
    private Map<String, Variable> variables = new HashMap<String, Variable>();

    public TimedCommand(String command) {
        command = command.trim();
        if(command.startsWith("/")) {
            command = command.substring(1);
        }
        Pattern pattern = Pattern.compile("\\%(\\w+?)(=(.+?)|)\\%");
        Matcher matcher = pattern.matcher(command);
        while(matcher.find()) {
            Variable var = new Variable(matcher.group(1));
            if(matcher.groupCount() >= 3) {
                var.setDefault(matcher.group(3));
            }
            variables.put(var.getName(), var);
        }
        this.command = command;
    }

    /**
     * Get the command string without any variables replaced
     * @return The pure command string
     */
    public String getCommand() {
        try {
            return getCommand(null);
        } catch (MissingVariableException ignored) {
            return command;
        }
    }

    /**
     * Get the command string with variables in it replaced
     * @param replacements The variable values
     * @return The command string; passing null will just get the unreplaced string
     */
    public String getCommand(Map<String, String> replacements) throws MissingVariableException {
        String returnCommand = command;
        if(replacements != null) {
            for(Variable var : variables.values()) {
                String value = replacements.get(var.getName());
                if (value == null) {
                    value = var.getDefault();
                }
                if (value == null) {
                    throw new MissingVariableException("No value nor defualt value set for variable " + var.getName());
                }
                value = Utils.replaceReplacements(value, replacements);
                returnCommand = returnCommand.replaceAll("\\%" + var.getName() + "(=.+?|)\\%", value);
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
        for(Variable var : variables.values()) {
            if(!inSet(var.getName(), replacements) && !var.hasDefault()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get a variable that is in this command
     * @param name The name of the variable (case insensitive)
     * @return The Variable or <tt>null</tt> if not found
     */
    public Variable getVariable(String name) {
        return variables.get(name.toLowerCase());
    }

    /**
     * Check whether or not a string is in a set case insensitive
     * @param string The string to check
     * @param set The set to search through
     * @return Whether or not a string is in the set (case insensitive)
     */
    private boolean inSet(String string, Set<String> set) {
        if(set.contains(string))
            return true;
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
    public Set<Variable> getVariables() {
        return new HashSet<Variable>(variables.values());
    }

    @Override
    public String toString() {
        return command;
    }

    private class Variable {
        private final String name;
        private String defaultValue = null;

        public Variable(String name) {
            this.name = name.toLowerCase();
        }

        public Variable(String name, String defaultValue) {
            this(name);
            this.defaultValue = defaultValue;
        }

        public String getName() {
            return name;
        }

        public String getDefault() {
            return defaultValue;
        }

        public boolean hasDefault() {
            return getDefault() != null;
        }

        public void setDefault(String defaultValue) {
            this.defaultValue = defaultValue;
        }
    }

    public class MissingVariableException extends Exception {
        public MissingVariableException(String message) {
            super(message);
        }
    }
}
