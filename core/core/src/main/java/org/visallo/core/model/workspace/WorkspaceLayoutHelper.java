package org.visallo.core.model.workspace;

import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.StreamUtil;
import org.visallo.web.clientapi.model.GraphPosition;

import java.util.OptionalInt;

@Deprecated
public class WorkspaceLayoutHelper {
    public static GraphPosition findOpening(
            Iterable<GraphPosition> existingPositions,
            GraphPosition graphPosition,
            LayoutHints layoutHints
    ) {
        while (isOccupied(existingPositions, graphPosition, layoutHints)) {
            graphPosition = findNextPosition(graphPosition, layoutHints);
        }
        return graphPosition;
    }

    public static GraphPosition findNextPosition(
            GraphPosition graphPosition,
            LayoutHints layoutHints
    ) {
        int x = graphPosition.getX();
        int y = graphPosition.getY();
        int minX = layoutHints.getMinX() == null ? 0 : layoutHints.getMinX();
        int maxX = layoutHints.getMaxX() == null ? 0 : layoutHints.getMaxX();
        int minY = layoutHints.getMinY() == null ? 0 : layoutHints.getMinY();
        int maxY = layoutHints.getMaxY() == null ? 0 : layoutHints.getMaxY();
        boolean overflow = false;
        switch (layoutHints.getDirection()) {
            case LEFT_TO_RIGHT:
                x += layoutHints.getXSpacing();
                if (layoutHints.getMaxX() != null && x > layoutHints.getMaxX() - layoutHints.getXSpacing()) {
                    x = minX;
                    overflow = true;
                }
                break;

            case RIGHT_TO_LEFT:
                x -= layoutHints.getXSpacing();
                if (layoutHints.getMinX() != null && x < layoutHints.getMinX()) {
                    x = maxX;
                    overflow = true;
                }
                break;

            case TOP_TO_BOTTOM:
                y += layoutHints.getYSpacing();
                if (layoutHints.getMaxY() != null && y > layoutHints.getMaxY() - layoutHints.getYSpacing()) {
                    y = minY;
                    overflow = true;
                }
                break;

            case BOTTOM_TO_TOP:
                y -= layoutHints.getYSpacing();
                if (layoutHints.getMinY() != null && y < layoutHints.getMinY()) {
                    y = maxY;
                    overflow = true;
                }
                break;

            default:
                throw new VisalloException("unhandled direction: " + layoutHints.getDirection());
        }

        if (overflow) {
            switch (layoutHints.getOverflowDirection()) {
                case LEFT_TO_RIGHT:
                    x += layoutHints.getXSpacing();
                    break;
                case RIGHT_TO_LEFT:
                    x -= layoutHints.getXSpacing();
                    break;
                case TOP_TO_BOTTOM:
                    y += layoutHints.getYSpacing();
                    break;
                case BOTTOM_TO_TOP:
                    y -= layoutHints.getYSpacing();
                    break;
                default:
                    throw new VisalloException("unhandled direction: " + layoutHints.getDirection());
            }
        }

        return new GraphPosition(x, y);
    }

    public static boolean isOccupied(
            Iterable<GraphPosition> existingPositions,
            GraphPosition graphPosition,
            LayoutHints layoutHints
    ) {
        return StreamUtil.stream(existingPositions)
                .anyMatch(gp -> gp.getX() > graphPosition.getX() - layoutHints.getXSpacing()
                        && gp.getX() < graphPosition.getX() + layoutHints.getXSpacing()
                        && gp.getY() > graphPosition.getY() - layoutHints.getYSpacing()
                        && gp.getY() < graphPosition.getY() + layoutHints.getYSpacing());
    }

    public static GraphPosition findBottomLeftOpening(Iterable<GraphPosition> graphPositions) {
        int minX = findMinX(graphPositions).orElse(0);
        int maxY = findMaxY(graphPositions).orElse(0);
        LayoutHints layoutHints = new LayoutHints()
                .setDirection(LayoutHints.Direction.TOP_TO_BOTTOM, LayoutHints.Direction.LEFT_TO_RIGHT);
        return findOpening(graphPositions, new GraphPosition(minX, maxY), layoutHints);
    }

    public static GraphPosition findTopLeftOpening(Iterable<GraphPosition> graphPositions) {
        int minX = findMinX(graphPositions).orElse(0);
        int maxY = findMinY(graphPositions).orElse(0);
        LayoutHints layoutHints = new LayoutHints()
                .setDirection(LayoutHints.Direction.BOTTOM_TO_TOP, LayoutHints.Direction.LEFT_TO_RIGHT);
        return findOpening(graphPositions, new GraphPosition(minX, maxY), layoutHints);
    }

    public static OptionalInt findMinX(Iterable<GraphPosition> graphPositions) {
        return StreamUtil.stream(graphPositions)
                .mapToInt(GraphPosition::getX)
                .min();
    }

    public static OptionalInt findMaxX(Iterable<GraphPosition> graphPositions) {
        return StreamUtil.stream(graphPositions)
                .mapToInt(GraphPosition::getX)
                .max();
    }

    public static OptionalInt findMinY(Iterable<GraphPosition> graphPositions) {
        return StreamUtil.stream(graphPositions)
                .mapToInt(GraphPosition::getY)
                .min();
    }

    public static OptionalInt findMaxY(Iterable<GraphPosition> graphPositions) {
        return StreamUtil.stream(graphPositions)
                .mapToInt(GraphPosition::getY)
                .max();
    }
}
