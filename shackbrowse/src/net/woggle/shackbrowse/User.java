package net.woggle.shackbrowse;

import java.util.Arrays;
import java.util.HashSet;

public class User {

    private final static HashSet<String> MODS = new HashSet<String>(Arrays.asList(new String[] { "bitchesbecrazy",    "degenerate",    "EvilDolemite",    "hirez",    "Morgin",    "ninjase",    "Portax",    "redfive",    "Serpico74",    "thaperfectdrug" }));
    private final static HashSet<String> EMPLOYEES = new HashSet<String>(Arrays.asList(new String[] { "the man with the briefcase", "SporkyReeve",    "Daniel_Perez",    "Joshua Hawkins",    "Brittany Vincent",    "beardedaxe",    "GBurke59",    "plonkus",    "hammersuit",    "staymighty", "shugamom" }));

    public static Boolean isEmployee(String userName)
    {
        return EMPLOYEES.contains(userName.toLowerCase());
    }

    public static Boolean isModerator(String userName)
    {
        return MODS.contains(userName.toLowerCase());
    }

}
