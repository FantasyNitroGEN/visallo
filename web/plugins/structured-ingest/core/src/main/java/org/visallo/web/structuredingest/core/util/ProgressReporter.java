package org.visallo.web.structuredingest.core.util;

public abstract class ProgressReporter {
    public abstract void finishedRow(long row, long totalRows);
}
