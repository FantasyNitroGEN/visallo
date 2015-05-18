package org.visallo.rdfTripleImport;

import org.apache.commons.lang.StringEscapeUtils;

public class RdfTriple {
    private final Part first;
    private final Part second;
    private final Part third;
    private final Part forth;

    public RdfTriple(Part first, Part second, Part third, Part forth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.forth = forth;
    }

    public Part getFirst() {
        return first;
    }

    public Part getSecond() {
        return second;
    }

    public Part getThird() {
        return third;
    }

    public Part getForth() {
        return forth;
    }

    public static class Part {
        private final String line;
        private final int startIndex;
        private final int endIndex;

        public Part(String line, int startIndex, int endIndex) {
            this.line = line;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        public int getStartIndex() {
            return startIndex;
        }

        public int getEndIndex() {
            return endIndex;
        }

        protected String getLine() {
            return line;
        }

        public String toString() {
            return line.substring(getStartIndex(), getEndIndex());
        }
    }

    public static class EmptyPart extends Part {
        public EmptyPart(String line, int startIndex, int endIndex) {
            super(line, startIndex, endIndex);
        }

        public String toString() {
            return "EmptyPart{" + super.toString() + "}";
        }
    }

    public static class UriPart extends Part {
        public UriPart(String line, int startIndex, int endIndex) {
            super(line, startIndex, endIndex);
        }

        public String getUri() {
            return getLine().substring(getStartIndex() + 1, getEndIndex() - 1);
        }

        public String toString() {
            return "UriPart{" + super.toString() + "}";
        }
    }

    public static class LiteralPart extends Part {
        private final int stringStart;
        private final int stringEnd;
        private final Integer langStart;
        private final Integer langEnd;
        private final UriPart type;

        public LiteralPart(String line, int startIndex, int endIndex, int stringStart, int stringEnd, Integer langStart, Integer langEnd, UriPart type) {
            super(line, startIndex, endIndex);
            this.stringStart = stringStart;
            this.stringEnd = stringEnd;
            this.langStart = langStart;
            this.langEnd = langEnd;
            this.type = type;
        }

        public String getString() {
            String str = getLine().substring(stringStart, stringEnd);
            str = StringEscapeUtils.unescapeJava(str);
            return str;
        }

        public String getLanguage() {
            if (langStart == null || langEnd == null) {
                return "";
            }
            return getLine().substring(langStart, langEnd);
        }

        public UriPart getType() {
            return type;
        }

        public String toString() {
            return "StringPart{" + super.toString() + "}";
        }
    }
}
