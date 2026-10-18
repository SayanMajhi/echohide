# Complete repository plan

This is the intended final tree, produced before implementation. `[1]` means created in Phase 1; `[2]`–`[8]` identify the phase that will create a file. Planned files are not implementations or test results. No empty implementation classes are supplied.

```text
EchoHide/
|-- .gitignore                                      [1]
|-- .gitattributes                                  [1]
|-- pom.xml                                         [1]
|-- README.md                                       [1]
|-- scripts/
|   |-- maven.ps1                                   [1] local Maven launcher
|   |-- evaluate.ps1                                [7]
|   `-- generate-demo.ps1                           [8]
|-- src/main/java/io/echohide/
|   |-- EchoHide.java                               [1] foundation CLI; GUI entry in 6
|   |-- config/ProjectConfig.java                   [1]
|   |-- audio/
|   |   |-- WavFileService.java                      [2]
|   |   |-- AudioPlayer.java                         [2]
|   |   `-- AudioRecorder.java                       [2, optional]
|   |-- crypto/
|   |   |-- PasswordKeyDeriver.java                  [3]
|   |   `-- AesGcmService.java                       [3]
|   |-- steganography/
|   |   |-- PayloadCodec.java                        [4]
|   |   `-- LsbSteganography.java                    [4]
|   |-- network/
|   |   |-- TransferProtocol.java                    [5]
|   |   |-- TcpFileSender.java                       [5]
|   |   `-- TcpFileReceiver.java                     [5]
|   |-- metrics/
|   |   |-- AudioMetrics.java                        [7]
|   |   |-- CsvResultWriter.java                     [7]
|   |   `-- ExperimentRunner.java                    [7]
|   |-- model/
|   |   |-- PcmAudio.java                            [2]
|   |   |-- EncryptedPayload.java                    [3]
|   |   |-- EmbeddingResult.java                     [4]
|   |   |-- TransferReceipt.java                     [5]
|   |   `-- ExperimentResult.java                    [7]
|   |-- exception/
|   |   |-- UnsupportedAudioException.java           [2]
|   |   |-- AuthenticationException.java             [3]
|   |   |-- InvalidPayloadException.java             [4]
|   |   |-- CapacityExceededException.java           [4]
|   |   `-- TransferException.java                   [5]
|   |-- ui/
|   |   |-- EchoHideFrame.java                       [6]
|   |   |-- SenderPanel.java                         [6]
|   |   |-- ReceiverPanel.java                       [6]
|   |   `-- MetricsPanel.java                        [6]
|   `-- util/Hashing.java                            [5]
|-- src/main/resources/echohide.properties            [1]
|-- src/test/java/io/echohide/
|   |-- config/ProjectConfigTest.java                [1]
|   |-- support/WavFixtures.java                     [2]
|   |-- audio/WavFileServiceTest.java                [2]
|   |-- crypto/AesGcmServiceTest.java                [3]
|   |-- steganography/PayloadCodecTest.java          [4]
|   |-- steganography/LsbSteganographyTest.java      [4]
|   |-- network/TcpFileTransferTest.java             [5]
|   |-- metrics/AudioMetricsTest.java               [7]
|   `-- integration/EndToEndTest.java                [8]
|-- docs/
|   |-- PHASE_1.md                                  [1]
|   |-- REQUIREMENTS.md                             [1]
|   |-- ARCHITECTURE.md                             [1]
|   |-- REPOSITORY_TREE.md                          [1]
|   |-- ROADMAP.md                                  [1]
|   |-- IMPLEMENTATION.md                           [1] design now; results added per phase
|   |-- PAYLOAD_FORMAT.md                           [1]
|   |-- NETWORK_PROTOCOL.md                         [1]
|   |-- TESTING.md                                  [1]
|   |-- EXPERIMENTS.md                              [1]
|   |-- DEMO_GUIDE.md                               [1] script now; hands-on steps in 8
|   |-- VIVA_QA.md                                  [1]
|   |-- VERIFICATION.md                             [1] actual foundation verification
|   `-- screenshots/README.md                       [1] capture policy; images in 6/8
|-- samples/README.md                               [1]
|-- samples/original/demo-mono.wav                   [8, generated]
|-- samples/original/demo-stereo.wav                 [8, generated]
|-- samples/stego/demo-mono-stego.wav                 [8, generated]
|-- samples/demo-message.txt                         [8, public demonstration text]
|-- results/README.md                               [1]
|-- results/experiments.csv                          [7, measured]
|-- .tools/apache-maven-3.9.9/                       [generated, ignored]
`-- target/                                         [generated, ignored]
```

JUnit fixtures are generated in code, so no binary test resources are needed. This is one Maven module with Java packages, not a multi-module Maven build. Only JUnit is an application test dependency; the runtime uses the JDK.
