package org.visallo.tools;

import org.visallo.core.exception.VisalloException;

public class RdfTripleParser {
    public static RdfTriple parseLine(String line) {
        RdfTriple.Part first = readPart(line, 0);
        RdfTriple.Part second = readPart(line, first.getEndIndex());
        RdfTriple.Part third = readPart(line, second.getEndIndex());
        RdfTriple.Part forth = null;
        if (third.getEndIndex() != line.length()) {
            forth = readPart(line, third.getEndIndex());
            if (forth.getEndIndex() != line.length()) {
                throw new VisalloException("Unexpected end of line: " + line);
            }
        }
        return new RdfTriple(first, second, third, forth);
    }

    private static RdfTriple.Part readPart(String line, int startOffset) {
        int i = skipWhitespace(line, startOffset);
        char startChar = line.charAt(i);
        if (startChar == '.') {
            return readEmptyPart(line, i);
        } else if (startChar == '<') {
            return readUriPart(line, i);
        } else if (startChar == '"') {
            return readLiteralPart(line, i);
        } else {
            throw new VisalloException("Invalid RDF line: " + line + ", unexpected character '" + startChar + "' at " + i);
        }
    }

    private static RdfTriple.Part readEmptyPart(String line, int startOffset) {
        return new RdfTriple.EmptyPart(line, startOffset, startOffset + 1);
    }

    private static RdfTriple.UriPart readUriPart(String line, int startOffset) {
        int i;
        for (i = startOffset + 1; i < line.length(); i++) {
            if (line.charAt(i) == '>') {
                return new RdfTriple.UriPart(line, startOffset, i + 1);
            }
        }
        throw new VisalloException("Unexpected end of URI: " + line);
    }

    private static RdfTriple.LiteralPart readLiteralPart(String line, int startOffset) {
        int i;
        int stringStart = startOffset + 1;
        Integer langStart = null;
        Integer langEnd = null;
        Integer stringEnd = null;
        RdfTriple.UriPart type = null;
        for (i = startOffset + 1; i < line.length(); i++) {
            if (line.charAt(i) == '\\') {
                i++;
                continue;
            }
            if (line.charAt(i) == '"') {
                stringEnd = i;
                i++;
                break;
            }
        }
        if (i < line.length()) {
            if (line.charAt(i) == '@') {
                langStart = i + 1;
                for (; i < line.length() && !Character.isWhitespace(line.charAt(i)); i++) ;
                langEnd = i;
            } else if (line.charAt(i) == '^' && line.charAt(i + 1) == '^' && line.charAt(i + 2) == '<') {
                type = readUriPart(line, i + 2);
                i = type.getEndIndex();
            }
        }
        if (stringEnd != null) {
            return new RdfTriple.LiteralPart(line, startOffset, i, stringStart, stringEnd, langStart, langEnd, type);
        }
        throw new VisalloException("Unexpected end of String: " + line);
    }

    private static int skipWhitespace(String line, int startOffset) {
        int i;
        for (i = startOffset; i < line.length(); i++) {
            if (!Character.isWhitespace(line.charAt(i))) {
                return i;
            }
        }
        return i;
    }
}
