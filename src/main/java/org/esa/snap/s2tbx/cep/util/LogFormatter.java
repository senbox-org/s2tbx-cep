package org.esa.snap.s2tbx.cep.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Simple formatter class for log messages.
 *
 * @author COsmin Cara
 */
public class LogFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {
        String level = record.getLevel().getName();
        String buffer = formatTime(record.getMillis()) +
                "\t" +
                "[" + level + "]" +
                "\t" + (level.length() < 6 ? "\t" : "") +
                record.getMessage() +
                "\n";
        return buffer;
    }

    private String formatTime(long millis) {
        SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date resultDate = new Date(millis);
        return date_format.format(resultDate);
    }
}