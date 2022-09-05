package pt.ua.imodec.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
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

    public static Map<String, Number> getOptions(String formatId) {
        Map<String, Map<String, Number>> options = getOptions();

        return options.get(formatId);
    }

    public static Map<String, Map<String, Number>> getOptions() {
        Yaml yaml = new Yaml();

        InputStream inputStream = MiscUtils.class
                .getClassLoader()
                .getResourceAsStream("encoding-options.yaml");

        return yaml.load(inputStream);
    }

    public static Number gracefulCast(Number number, Class<? extends Number> toType) {
        if (Float.class.equals(toType))
            return number.floatValue();
        else if (Double.class.equals(toType))
            return number.doubleValue();
        else if (Integer.class.equals(toType))
            return number.intValue();
        else if (Short.class.equals(toType))
            return number.shortValue();
        else if (Byte.class.equals(toType))
            return number.byteValue();
        logger.error("Invalid type to cast to!");
        return number;
    }
}
