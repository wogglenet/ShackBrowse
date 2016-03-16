package net.woggle.shackbrowse;

import java.util.Arrays;
import java.util.HashSet;

public class User {

    private final static HashSet<String> MODS = new HashSet<String>(Arrays.asList(new String[] { "ajax", "megara9", "morgin", "bitchesbecrazy", "frozen pixel", "eonix", "hirez", "helvetica", "thekidd", "zakk", "edgewise", "loioshdwaggie", "multisync", "rauol duke", "deathlove", "evildolemite", "redfive", "thaperfectdrug", "woddemandred", "edgewise", "sgtsanity" }));
    private final static HashSet<String> EMPLOYEES = new HashSet<String>(Arrays.asList(new String[] { "the man with the briefcase", "staymighty", "hammersuit", "shacknews", "aaron linde", "jeff mattas", "garnett lee", "brian leahy", "ackbar2020", "xavdematos", "shugamom" }));

    public static Boolean isEmployee(String userName)
    {
        return EMPLOYEES.contains(userName.toLowerCase());
    }

    public static Boolean isModerator(String userName)
    {
        return MODS.contains(userName.toLowerCase());
    }

}
