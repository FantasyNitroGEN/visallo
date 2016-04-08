package org.visallo.core.util;

import org.vertexium.Element;
import org.vertexium.query.Query;

import java.util.Arrays;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This class contains methods for working with {@link java.util.stream.Stream}.
 */
public class StreamUtil {

    private StreamUtil() {
    }

    /**
     * Create a {@link java.util.stream.Stream} containing the results of executing the queries, in order. The results
     * are not loaded into memory first.
     */
    public static Stream<Element> stream(Query... queries) {
        return Arrays.stream(queries)
                .map(query -> StreamSupport.stream(query.elements().spliterator(), false))
                .reduce(Stream::concat)
                .orElseGet(Stream::empty);
    }

    /**
     * Create a {@link java.util.stream.Stream} containing the contents of the iterables, in order.  The contents
     * are not loaded into memory first.
     */
    @SafeVarargs
    public static <T> Stream<T> stream(Iterable<T>... iterables) {
        return Arrays.stream(iterables)
                .map(iterable -> StreamSupport.stream(iterable.spliterator(), false))
                .reduce(Stream::concat)
                .orElseGet(Stream::empty);
    }
}
