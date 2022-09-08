package pt.ua.imodec.util.validators;

public class OSValidator implements Validator {

    private static final String OS = System.getProperty("os.name").toLowerCase();

    private static final boolean IS_WINDOWS = OS.contains("win");
    private static final boolean IS_LINUX = OS.contains("nux") || OS.contains("nix") || OS.contains("aix");
    private static final boolean IS_MAC = OS.contains("mac");
    private static final boolean IS_SOLARIS = OS.contains("sunos");

    private static final boolean IS_WINDOWS_SUPPORTED = false;
    private static final boolean IS_LINUX_SUPPORTED = true;
    private static final boolean IS_MAC_SUPPORTED = false;
    private static final boolean IS_SOLARIS_SUPPORTED = false;

    public static boolean validate() {

        if (IS_WINDOWS)
            return IS_WINDOWS_SUPPORTED;

        if (IS_LINUX)
            return IS_LINUX_SUPPORTED;

        if (IS_MAC)
            return IS_MAC_SUPPORTED;

        if (IS_SOLARIS)
            return IS_SOLARIS_SUPPORTED;

        return false;

    }
}
