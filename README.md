[![Release](https://jitpack.io/v/umjammer/mp3spi.svg)](https://jitpack.io/#umjammer/mp3spi)
[![Java CI](https://github.com/umjammer/mp3spi/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/mp3spi/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/mp3spi/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/umjammer/mp3spi/actions/workflows/codeql-analysis.yml)
![Java](https://img.shields.io/badge/Java-17-b07219)
[![Parent](https://img.shields.io/badge/Parent-vavi--sound--sandbox-pink)](https://github.com/umjammer/vavi-sound-sandbox)

# MP3SPI

<img alt="mp3 logo" src="https://github.com/umjammer/mp3spi/assets/493908/b718b78d-15c6-4356-a5ca-fca63ad7ffcb" width=160 /><sub><a href="https://www.iis.fraunhofer.de/de/ff/amm/unterhaltungselektronik/mp3.html">🅮 fraunhofer</a></sub>

MP3 Java Sound SPI.

- decider is powered by [jlayer](https://github.com/umjammer/jlayer)
- encoder is powered by [java-lame](https://github.com/nwaldispuehl/java-lame)

Both are in pure Java.

## Install

 * [maven](https://jitpack.io/#umjammer/mp3spi)

## Usage

 * [decode](src/test/java/Test3.java)
 * [encode](src/test/java/Test4.java)

## References

 * [Java Sound Programmer Guide](https://docs.oracle.com/javase/8/docs/technotes/guides/sound/programmer_guide/contents.html)

## TODO

 * ~~encoder using https://github.com/nwaldispuehl/java-lame~~
 * rename project as vavi-sound-mp3
 * out source tag parser (use like vavi-util-tag)
 * ~~out source version~~
 * jsidplay2:jump3r is the origin of java-lame (replace?)

----

# Original

JavaZOOM 1999-2005

Project Homepage :<br/>
[http://www.javazoom.net/mp3spi/mp3spi.html](https://web.archive.org/web/20210108055829/http://www.javazoom.net/mp3spi/mp3spi.html)

JAVA and MP3 online Forums :<br/>
[http://www.javazoom.net/services/forums/index.jsp](https://web.archive.org/web/20041010053627/http://www.javazoom.net/services/forums/index.jsp)

## DESCRIPTION

MP3SPI is a SPI (Service Provider Interface) that adds MP3 support for JavaSound.
It allows to play MPEG 1/2/2.5 Layer 1/2/3 files thanks to underlying [JLayer](https://github.com/umjammer/jlayer)
and [Tritonus](https://github.com/umjammer/tritonus) libraries. This is a non-commercial project and anyone can add his
contribution. MP3SPI is licensed under LGPL (see [LICENSE](LICENSE.txt)).

## FAQ

### How to install MP3SPI ?

 * https://github.com/umjammer/mp3spi/packages/1067666
 * this project uses github packages. add a personal access token to `~/.m2/settings.xml`
 * see https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry

### Do I need JMF to run MP3SPI player ?

  No, JMF is not required. You just need a JVM JavaSound 1.0 compliant.
  (i.e. JVM1.3 or higher). However, MP3SPI is not JMF compliant.

### Does MP3SPI support streaming ?

  Yes, it has been successfully tested with SHOUTCast and ICEcast streaming servers.

### Does MP3SPI support MPEG 2.5 ?

  Yes, MP3SPI includes same features as JLayer.

### Does MP3SPI support VBR ?

  Yes, It supports XING and VBRI VBR header too.

### How to get ID3v2 tags from MP3SPI API ?

  MP3SPI exposes many audio properties such as ID3v1/v2 frames, VBR, bitrate ...
  See online examples from MP3SPI homepage to learn how to get them.
  MP3SPI supports most used ID3v1.0, v1.1, v2.2, v2.3, v2.4 tags.

### How to skip frames to have a seek feature ?

  Call `skip(long bytes)` on `AudioInputStream`.

### How to run jUnit tests ?

  Run `mvn test`. You can update `src/test/resources/test.mp3.properties` file
  with the audio properties of the MP3 you want to use for testing.

### How to enable debug/traces for the MP3SPI ?

  Set the following system variable : `"tritonus.TraceAudioFileReader=true"`
  It means `java.exe -Dtritonus.TraceAudioFileReader=true your.package.Player`

### How to contact MP3SPI developers ?

  Try to post a thread on Java&MP3 online forums at :
  http://www.javazoom.net/services/forums/index.jsp
  You can also contact us at mp3spi@javazoom.net for contributions.
  or [me](https://github.com/umjammer/mp3spi/issues).

### How to specify mp3 tag's encoding

  Set the system property `javazoom.spi.mpeg.encoding`. e.g `javazoom.spi.mpeg.encoding=MS932`