package pt.ua.imodec.util.validators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class CodecsInstalledValidator implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(CodecsInstalledValidator.class);

    /**
     * Verifies if all the codec tools are in the running machine
     *
     * @return True if the codecs are available in your local machine
     */
    public static boolean validate() {

        String commandToCheckPresence = "which cjxl djxl cavif avif_decode cwebp dwebp";

        try {
            Process checkProcess = Runtime.getRuntime().exec(commandToCheckPresence);
            checkProcess.waitFor();
            return checkProcess.exitValue() == 0;
        } catch (InterruptedException ignored) {
            logger.warn("Codec checking process was interrupted!");
        } catch (IOException ignored) {
            logger.warn("IOError occurred.");
        }
        return false;
    }

}
