package org.visallo.core.cmdline.converters;

import com.beust.jcommander.IStringConverter;
import org.visallo.core.model.workQueue.Priority;

public class WorkQueuePriorityConverter implements IStringConverter<Priority> {
    @Override
    public Priority convert(String s) {
        return Priority.safeParse(s);
    }
}
