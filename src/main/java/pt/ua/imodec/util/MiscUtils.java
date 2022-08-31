package pt.ua.imodec.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.WriteAbortedException;
import java.util.function.Supplier;

public class MiscUtils {

    private static final Logger logger = LoggerFactory.getLogger(MiscUtils.class);

    /**
     *
     * Same as file.createNewFile, however creates parents directories if they don't exist
     *
     * @param file File to be created
     * @return Status flag
     * @throws IOException from file.createNewFile
     */
    public static boolean createNewFile(File file, boolean overwrite) throws IOException {
        File parentFile = file.getParentFile();

        if (!parentFile.exists())
            if (!parentFile.mkdirs())
                throw new IOException(String.format("Unexpected error: Could not mkdir of dir '%s'.", parentFile));

        if (overwrite && file.exists() && !file.delete())
                logger.error("Could not delete temporary file with the same name that was trying to create!");

        return file.createNewFile();
    }

    public static void sleepWhile(Supplier<Boolean> booleanSupplier) {
        while (booleanSupplier.get()) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
