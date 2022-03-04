# LLJ-ZIP

A closer to the spec implementation of ZIP parsing for Java.

## Relevant ZIP information
 
**[Official spec](https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT)**

The notes and structure outlines are the basis for most of LLJ-ZIP.

**[JVM zip parsing](https://github.com/openjdk/jdk/blob/739769c8fc4b496f08a92225a12d07414537b6c0/src/java.base/share/native/libjli/parse_manifest.c#L120)**

The JVM zip reader implementation is based off this piece.

> This is a zip format reader for seekable files, **that tolerates leading and trailing garbage**, 
> and **tolerates having had internal offsets adjusted for leading garbage** _(as with Info-Zip's zip -A)_.