[![Maven Package](https://github.com/umjammer/mp3spi/actions/workflows/maven-publish.yml/badge.svg)](https://github.com/umjammer/mp3spi/actions/workflows/maven-publish.yml) [![Java CI with Maven](https://github.com/umjammer/mp3spi/workflows/Java%20CI%20with%20Maven/badge.svg)](https://github.com/umjammer/mp3spi/actions) [![Parent](https://img.shields.io/badge/Parent-vavi--sound--sandbox-pink)](https://github.com/umjammer/vavi-sound-sandbox)

# MP3SPI

 JavaZOOM 1999-2005

 Project Homepage :<br/>
   http://www.javazoom.net/mp3spi/mp3spi.html 

 JAVA and MP3 online Forums :<br/>
   http://www.javazoom.net/services/forums/index.jsp

----

## DESCRIPTION

MP3SPI is a SPI (Service Provider Interface) that adds MP3 support for JavaSound.
It allows to play MPEG 1/2/2.5 Layer 1/2/3 files thanks to underlying [JLayer](https://github.com/umjammer/jlayer)
and [Tritonus](https://github.com/umjammer/tritonus) libraries. This is a non-commercial project and anyone can add his
contribution. MP3SPI is licensed under LGPL (see [LICENSE.txt](LICENSE.txt)).


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

### How much memory/CPU MP3SPI needs to run ?

  Here are our benchmark notes :

  * Heap use range : 1380KB to 1900KB - 370 classes loaded.
  * Footprint :

|memory|environment|
|---|---|
| ~8MB |under WinNT4/Win2K + J2SE 1.3 (Hotspot) |
| ~10MB |under WinNT4/Win2K + J2SE 1.4.1 (Hotspot) |

  * CPU usage :

|cpu|environment|
|---|---|
| ~12%| under PIII 800Mhz/WinNT4+J2SE 1.3 (Hotspot) |
| ~8% | under PIII 1Ghz/Win2K+J2SE 1.3.1 (Hotspot) |
| ~12% | under PIII 1Ghz/Win2K+J2SE 1.4.1 (Hotspot) |
|  ~1% | under PIII 1Ghz/Win2K+J2SE 1.5.0 (Hotspot) |

### How to enable debug/traces for the MP3SPI ?
  Set the following system variable : `"tritonus.TraceAudioFileReader=true"`
  It means `java.exe -Dtritonus.TraceAudioFileReader=true your.package.Player`

### How to contact MP3SPI developers ?

  Try to post a thread on Java&MP3 online forums at :
  http://www.javazoom.net/services/forums/index.jsp
  You can also contact us at mp3spi@javazoom.net for contributions.
  or [me](https://github.com/umjammer/mp3spi/issues).

## KNOWN PROBLEMS

99% of MP3 plays well with JLayer but some (1%) return an `ArrayIndexOutOfBoundsException`
while playing. It might come from invalid audio frames.

When source is not a file, this cannot deal id3v1 tag which file size is larger than 20M bytes.

### Workaround

Just `try`/`catch` `ArrayIndexOutOfBoundsException` in your code to skip
non-detected invalid frames.
