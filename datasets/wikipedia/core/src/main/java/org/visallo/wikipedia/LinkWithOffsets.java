package org.visallo.wikipedia;

public interface LinkWithOffsets {
    String getLinkTargetWithoutHash();

    int getStartOffset();

    int getEndOffset();
}
