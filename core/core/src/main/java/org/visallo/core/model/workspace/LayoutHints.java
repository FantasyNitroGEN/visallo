package org.visallo.core.model.workspace;

import org.visallo.core.exception.VisalloException;

public class LayoutHints {
    public static final int DEFAULT_SPACING = 200;
    private int xSpacing = DEFAULT_SPACING;
    private int ySpacing = DEFAULT_SPACING;
    private Integer minX;
    private Integer maxX;
    private Integer minY;
    private Integer maxY;
    private Direction direction = Direction.LEFT_TO_RIGHT;
    private Direction overflowDirection = Direction.TOP_TO_BOTTOM;

    public int getXSpacing() {
        return xSpacing;
    }

    public LayoutHints setXSpacing(int xSpacing) {
        this.xSpacing = xSpacing;
        return this;
    }

    public int getYSpacing() {
        return ySpacing;
    }

    public LayoutHints setYSpacing(int ySpacing) {
        this.ySpacing = ySpacing;
        return this;
    }

    public LayoutHints setSpacing(int spacing) {
        setXSpacing(spacing);
        setYSpacing(spacing);
        return this;
    }

    public Integer getMinX() {
        return minX;
    }

    public LayoutHints setMinX(Integer minX) {
        this.minX = minX;
        return this;
    }

    public Integer getMinY() {
        return minY;
    }

    public LayoutHints setMinY(Integer minY) {
        this.minY = minY;
        return this;
    }

    public Integer getMaxX() {
        return maxX;
    }

    public LayoutHints setMaxX(Integer maxX) {
        this.maxX = maxX;
        return this;
    }

    public Integer getMaxY() {
        return maxY;
    }

    public LayoutHints setMaxY(Integer maxY) {
        this.maxY = maxY;
        return this;
    }

    public Direction getDirection() {
        return direction;
    }

    public Direction getOverflowDirection() {
        return overflowDirection;
    }

    public LayoutHints setDirection(Direction direction, Direction overflowDirection) {
        if (direction.isHorizontal() && overflowDirection.isHorizontal()) {
            throw new VisalloException("Both direction and overflowDirection cannot be horizontal");
        }
        if (direction.isVertical() && overflowDirection.isVertical()) {
            throw new VisalloException("Both direction and overflowDirection cannot be vertical");
        }
        this.direction = direction;
        this.overflowDirection = overflowDirection;
        return this;
    }

    public enum Direction {
        LEFT_TO_RIGHT,
        RIGHT_TO_LEFT,
        TOP_TO_BOTTOM,
        BOTTOM_TO_TOP;

        public boolean isHorizontal() {
            return this == LEFT_TO_RIGHT || this == RIGHT_TO_LEFT;
        }

        public boolean isVertical() {
            return !isHorizontal();
        }
    }
}
