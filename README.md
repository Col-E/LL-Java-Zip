# LLJ-ZIP

A closer to the spec implementation of ZIP parsing for Java.

## Relevant ZIP information
 
**[Official spec](https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT)**

The notes and structure outlines are the basis for most of LLJ-ZIP.

**[JVM zip parsing](https://github.com/openjdk/jdk/blob/739769c8fc4b496f08a92225a12d07414537b6c0/src/java.base/share/native/libjli/parse_manifest.c#L120)**

The JVM zip reader implementation is based off this piece.

> This is a zip format reader for seekable files, **that tolerates leading and trailing garbage**, 
> and **tolerates having had internal offsets adjusted for leading garbage** _(as with Info-Zip's zip -A)_.

But that's not all it does. That's just what that one comment says. Some other fun quirks of the JVM zip parser:

- The end central directory entry is found by scanning from the end of the file, rather than from the beginning.
- The central directory values are authoritative. Names/values defined by the local file headers are ignored.
- The file data of local file headers is not size bound by the file header's compressed size field. Instead, it uses the central directory header's declared size.
- Class names are allowed to end in trailing `/` which most tools interpret as directories.