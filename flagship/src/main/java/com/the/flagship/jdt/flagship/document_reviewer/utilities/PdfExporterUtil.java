package com.the.flagship.jdt.flagship.document_reviewer.utilities;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Inflater;

import org.springframework.stereotype.Component;

// Spring: "create ONE instance of this class and manage its lifecycle"
@Component
public class PdfExporterUtil {

    // Pre-compiled regex (static = created once at class-load, not per call).
    // CHANGED (again): the previous version required a newline directly before
    // "endstream" (\r?\nendstream). That assumption broke on this PDF too —
    // ReportLab (the library that generated it) omits the trailing newline
    // when the stream's exact byte length is already known via the /Length
    // field in the dictionary, so some streams end like "...mD@~>endstream"
    // with zero characters in between.
    //
    // Real PDF parsers avoid this problem entirely by reading exactly
    // /Length bytes after "stream\n", instead of searching for the literal
    // word "endstream" — which is why libraries like PDFBox never hit this
    // bug. This regex-based approach is doing it the fragile way on purpose,
    // for learning, so it needs a workaround instead: make the newline
    // before "endstream" OPTIONAL rather than required.
    //
    //   <<              -- start of a PDF dictionary
    //   (.*?)           -- group 1: capture everything inside it (lazy)
    //   >>              -- end of the dictionary
    //   \s*stream\r?\n  -- the literal word "stream", then a newline
    //   (.*?)           -- group 2: capture the raw stream bytes (lazy)
    //   (?:\r?\n)?      -- OPTIONAL newline before endstream (non-capturing
    //                      group, "?" means zero-or-one occurrence) — this is
    //                      the actual fix for this bug
    //   endstream       -- the literal word "endstream"
    //   DOTALL          -- makes "." also match newlines, since stream bytes
    //                      are binary data full of \n and \r bytes that
    //                      aren't meant to mean "end of line" here
    private static final Pattern OBJECT_STREAM = Pattern.compile(
        "<<(.*?)>>\\s*stream\\r?\\n(.*?)(?:\\r?\\n)?endstream",
        Pattern.DOTALL
    );

    // No-arg constructor — Spring needs one (or a single @Autowired constructor)
    // to instantiate the bean. Empty here because there's nothing to inject.
    public PdfExporterUtil() {}

    // Public entry point #1: accept a file path, read bytes, delegate to the
    // byte[] overload. Stateless — the path is a parameter, not a field, so
    // this bean is safe to call concurrently (no shared mutable state to race on).
    public String extractText(String filePath) throws Exception {
        // Path.of() handles "/" vs "\" differences across operating systems.
        byte[] raw = Files.readAllBytes(Path.of(filePath)); // reads entire file into memory
        return extractText(raw);                             // delegate to the byte[] version
    }

    // Public entry point #2: the core logic.
    private String extractText(byte[] pdf) throws Exception {
        // ISO_8859_1 = 1:1 byte-to-char mapping (byte value 0-255 maps directly
        // to a char with that same code point). This matters because a PDF file
        // is a mix of plain-text structure (dictionaries, keywords like "stream")
        // and raw binary data (the compressed content itself). UTF-8 would try
        // to interpret certain byte sequences as multi-byte characters and
        // CORRUPT the binary data. ISO_8859_1 never does that — every byte
        // becomes exactly one char, unchanged, so we can safely convert back
        // to bytes later with no data loss.
        String raw = new String(pdf, StandardCharsets.ISO_8859_1);

        StringBuilder result = new StringBuilder();

        // Run the regex over the whole file. Each match is one PDF object that
        // has a dictionary followed by a stream (fonts, images, and text content
        // all look like this structurally — we don't know which is which yet).
        Matcher m = OBJECT_STREAM.matcher(raw);
        while (m.find()) {
            String dict = m.group(1);        // the dictionary text, e.g. "/Filter [ /ASCII85Decode /FlateDecode ] /Length 1162 "
            String streamBody = m.group(2);  // the raw bytes sitting between "stream\n" and "\nendstream"

            // Ask decodeStream() to figure out which filters were used (by
            // reading the dictionary) and reverse them to get plain content back.
            String content = decodeStream(dict, streamBody);

            // Not every stream is text — some are images, embedded fonts, etc.
            // decodeStream() returns null if decoding failed (see its catch
            // block), and even successfully-decoded non-text streams won't
            // contain PDF's text-drawing operators. Tj = "show text string",
            // TJ = "show text string array with positioning adjustments".
            // Only streams containing these are actual visible text content.
            if (content != null && (content.contains("Tj") || content.contains("TJ"))) {
                result.append(extractTextOperators(content)).append("\n");
            }
        }

        // .strip() removes leading/trailing whitespace (Java 11+ method).
        return result.toString().strip();
    }

    // NEW METHOD: this is the piece that fixes your "0 characters extracted" bug.
    //
    // PDF filters can be STACKED — applied one after another when the file was
    // created. Your test PDF's dictionary says:
    //     /Filter [ /ASCII85Decode /FlateDecode ]
    // Filters in that array are listed in ENCODING order (the order they were
    // applied when the PDF was written): the original text was first
    // FlateDecode-compressed (zlib/DEFLATE), and THEN the compressed bytes were
    // run through ASCII85 encoding (which turns arbitrary bytes into safe
    // printable ASCII characters — useful for embedding binary data in a
    // text-based format like PDF).
    //
    // To reverse this, you must undo the filters in the OPPOSITE order they
    // were applied — last-applied filter gets undone first:
    //   1. ASCII85-decode the stream body -> gives you back the compressed bytes
    //   2. Flate-inflate those bytes -> gives you back the original content
    private String decodeStream(String dict, String streamBody) {
        try {
            // Start by assuming the stream body, converted back to raw bytes,
            // is what we have to work with (this covers the case where NO
            // filters are present at all).
            byte[] bytes = streamBody.getBytes(StandardCharsets.ISO_8859_1);

            // Check the dictionary text for the filter name. This is a simple
            // substring check rather than a full PDF dictionary parser — good
            // enough for learning purposes, but a real parser (like PDFBox's)
            // would properly tokenize the dictionary instead of string-matching.
            if (dict.contains("ASCII85Decode")) {
                // Undo ASCII85 first, since it was applied LAST during encoding.
                bytes = decodeAscii85(streamBody);
            }

            if (dict.contains("FlateDecode")) {
                // Undo Flate second (it was applied FIRST during encoding,
                // so it's the last thing we undo). inflate() expects the
                // compressed bytes we just got back from ASCII85 decoding
                // (or the raw stream bytes, if there was no ASCII85 layer).
                return inflate(bytes);
            }

            // No FlateDecode in this dictionary — whatever bytes we have at
            // this point (possibly already ASCII85-decoded) are the final
            // content as-is.
            return new String(bytes, StandardCharsets.ISO_8859_1);

        } catch (Exception e) {
            // Some streams will legitimately fail here — e.g. images encoded
            // with DCTDecode (JPEG) or fonts with binary formats this code
            // doesn't understand at all. Rather than crash the whole extraction,
            // treat "couldn't decode this one" as "skip it, it's probably not
            // text anyway."
            return null;
        }
    }

    // NEW METHOD: Adobe ASCII85 decoder, written from scratch.
    //
    // How ASCII85 encoding works (so decoding makes sense):
    // - Input bytes are grouped 4 at a time.
    // - Each group of 4 bytes (a 32-bit number, max value ~4.29 billion) is
    //   converted into 5 base-85 "digits" (since 85^5 is just over 4.29 billion,
    //   5 digits is enough to represent any 32-bit value).
    // - Each base-85 digit (0-84) is then shown as a printable ASCII character
    //   by adding 33 (the character '!') — so digit 0 becomes '!', digit 84
    //   becomes 'u'.
    // - Special case: a group of 4 zero bytes is shown as just the single
    //   character 'z', as a size optimization (four zero bytes are common).
    // - The encoded stream ends with the two characters "~>".
    //
    // Decoding reverses each of these steps.
    private byte[] decodeAscii85(String encoded) throws Exception {
        // Strip the "~>" end-of-data marker, and remove any whitespace/newlines
        // that might have been inserted for line-wrapping (PDF writers often
        // wrap long encoded lines for readability, and that whitespace is NOT
        // part of the actual data).
        String s = encoded.replace("~>", "").replaceAll("\\s+", "");

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Holds up to 5 base-85 digit values (0-84) as we build up one group.
        int[] group = new int[5];
        int count = 0; // how many digits we've collected into `group` so far

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            // 'z' shorthand: only valid at the START of a group (count == 0).
            // It represents an entire group of 4 zero bytes with no further
            // decoding needed.
            if (c == 'z' && count == 0) {
                out.write(0);
                out.write(0);
                out.write(0);
                out.write(0);
                continue; // move to the next character, don't fall through below
            }

            // Convert the printable character back to its base-85 digit value.
            // '!' is ASCII 33 and represents digit 0, so subtracting 33 gives
            // us the actual digit value (0-84).
            group[count++] = c - 33;

            // Once we have a full group of 5 digits, convert it to 4 output bytes.
            if (count == 5) {
                writeGroup(out, group, 5);
                count = 0; // reset for the next group
            }
        }

        // PDF binary data very rarely divides evenly into groups of 4 bytes
        // (which is 5 encoded characters), so there's often a leftover partial
        // group at the very end of the stream.
        if (count > 0) {
            // Per the ASCII85 spec, pad the missing digit slots with the
            // maximum digit value (84, i.e. character 'u') before computing —
            // this is a standard part of the algorithm's handling of partial
            // final groups, not a guess.
            for (int i = count; i < 5; i++) {
                group[i] = 84;
            }
            writeGroup(out, group, count); // count = how many bytes are actually real
        }

        return out.toByteArray();
    }

    // Helper for decodeAscii85(): converts one group of base-85 digits back
    // into raw bytes.
    //
    // Math: 5 base-85 digits represent one 32-bit number, computed as:
    //   value = d0*85^4 + d1*85^3 + d2*85^2 + d3*85^1 + d4*85^0
    // The loop below computes this incrementally instead of using explicit
    // powers of 85, which is mathematically the same thing (each iteration
    // multiplies the running total by 85 and adds the next digit).
    private void writeGroup(ByteArrayOutputStream out, int[] group, int validCount) {
        long value = 0;
        for (int i = 0; i < 5; i++) {
            value = value * 85 + group[i];
        }

        // Split the 32-bit value back into its 4 individual bytes, most
        // significant byte first (big-endian) — this matches how the bytes
        // were originally grouped during encoding.
        byte[] bytes = new byte[]{
            (byte) (value >> 24),
            (byte) (value >> 16),
            (byte) (value >> 8),
            (byte) value
        };

        // A full group of 5 encoded digits represents exactly 4 real bytes.
        // A partial final group of N digits (N < 5) represents only
        // (N - 1) real bytes — the padding digits we added in decodeAscii85()
        // exist purely to make the math work, they aren't real output data.
        out.write(bytes, 0, validCount == 5 ? 4 : validCount - 1);
    }

    // Decompresses  
    // No external library needed for this part — it's built into the JDK.
    private String inflate(byte[] input) throws Exception {
        // new Inflater() = expect a 2-byte zlib header (the standard case,
        // which is what FlateDecode in PDFs uses).
        // new Inflater(true) would mean "raw DEFLATE, no header" — rare, not
        // needed here.
        Inflater inflater = new Inflater();
        inflater.setInput(input);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096]; // 4 KB read chunk, reused each loop iteration

        while (!inflater.finished()) {
            int n = inflater.inflate(buf); // fills buf, returns how many bytes were written
            if (n == 0 && inflater.needsInput()) {
                // Safety valve: if inflate() produced zero bytes AND says it
                // needs more input we don't have, break instead of looping
                // forever.
                break;
            }
            out.write(buf, 0, n);
        }
        inflater.end(); // releases native (non-Java-heap) resources held by zlib

        return new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
    }

    // Parses PDF text-showing operators out of a decompressed content stream.
    // PDF text drawing looks like:
    //   (Hello World) Tj             -- show one string
    //   [(He) -50 (llo)] TJ          -- show an array of string fragments,
    //                                    with kerning/spacing numbers between them
    private String extractTextOperators(String content) {
        StringBuilder sb = new StringBuilder();

        // Case 1: single string + Tj
        // \\(              -- literal opening parenthesis
        // (.*?)            -- group 1: the text itself (lazy match)
        // (?<!\\\\)        -- negative lookbehind: the character right before
        //                     this position must NOT be a backslash — this
        //                     stops the match from ending early on an escaped
        //                     \) inside the string (e.g. "Section 1\)")
        // \\)              -- literal closing parenthesis
        // \\s*Tj           -- optional whitespace, then the operator "Tj"
        Matcher tj = Pattern.compile("\\((.*?)(?<!\\\\)\\)\\s*Tj").matcher(content);
        while (tj.find()) {
            sb.append(unescape(tj.group(1))).append(' ');
        }

        // Case 2: array of strings + TJ
        // \\[(.*?)\\]\\s*TJ  -- capture everything inside the square brackets
        Matcher tjArr = Pattern.compile("\\[(.*?)\\]\\s*TJ").matcher(content);
        while (tjArr.find()) {
            // Inside the brackets there can be multiple (string) fragments
            // interleaved with plain numbers (kerning adjustments, e.g. -50).
            // We only care about the string fragments, so run a second regex
            // just on the captured bracket contents.
            Matcher inner = Pattern.compile("\\((.*?)(?<!\\\\)\\)").matcher(tjArr.group(1));
            while (inner.find()) {
                sb.append(unescape(inner.group(1)));
            }
        }

        return sb.toString();
    }

    // PDF string escape sequences -> actual characters.
    // Order matters here: "\\\\" (an escaped backslash) must be unescaped LAST,
    // otherwise something like "\\n" (an escaped backslash followed by the
    // letter n) could be incorrectly unescaped as a newline character instead.
    // CHANGED: added octal escape handling (\ddd, 1-3 digits). PDF strings can
    // embed any raw byte value this way — e.g. \277 means the single byte 0277
    // octal (191 decimal, 0xBF hex). Without handling this, sequences like
    // \277 or \177 pass through as the literal characters '\', '2', '7', '7'
    // instead of becoming the one character they actually represent.
    private String unescape(String s) {
        // return s.replace("\\n", "\n")
        //          .replace("\\r", "\r")
        //          .replace("\\t", "\t")
        //          .replace("\\(", "(")
        //          .replace("\\)", ")")
        //          .replace("\\\\", "\\");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            // Only enter escape-handling if this char is a backslash AND
            // there's at least one more character after it to look at.
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);

                // Octal escape: backslash followed by a digit 0-7.
                if (next >= '0' && next <= '7') {
                    int value = 0;  // the byte value we're building up
                    int digits = 0; // how many octal digits consumed so far
                    int j = i + 1;  // scan position, starting at the first digit

                    // Consume up to 3 octal digits, per the PDF spec (an
                    // octal escape is never more than 3 digits long).
                    while (j < s.length() && digits < 3
                            && s.charAt(j) >= '0' && s.charAt(j) <= '7') {
                        value = value * 8 + (s.charAt(j) - '0'); // shift left in base 8, add next digit
                        j++;
                        digits++;
                    }

                    result.append((char) value); // the single byte/char this sequence represents
                    i = j - 1; // -1 because the outer for-loop's i++ will advance past it next iteration
                    continue;
                }

                // Named escapes (fixed, single-character meanings).
                switch (next) {
                    case 'n': result.append('\n'); i++; continue;
                    case 'r': result.append('\r'); i++; continue;
                    case 't': result.append('\t'); i++; continue;
                    case '(': result.append('('); i++; continue;
                    case ')': result.append(')'); i++; continue;
                    case '\\': result.append('\\'); i++; continue;
                    default:
                        // Per the PDF spec, an unrecognized backslash sequence
                        // just means "drop the backslash, keep the character."
                        result.append(next);
                        i++;
                        continue;
                }
            }

            // Not part of an escape sequence — copy the character as-is.
            result.append(c);
        }

        return result.toString();
    }
}