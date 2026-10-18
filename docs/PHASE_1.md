# Phase 1 delivery

## A Goal

Establish the synopsis-based requirements, mandatory/optional boundary, package responsibilities, exact payload and transfer formats, acceptance matrix and buildable Java 21 foundation. The synopsis is reference material; the pasted master prompt is the user's instruction. This phase implements no audio, crypto, LSB, network or GUI services.

## B Files

Repository root: `C:/Users/sayan/.codex/.chatgpt-projects/g-p-6acffa50b6e481919e25a9ac73145ccc/EchoHide`.

Every path below is relative to that root; combine it with the root for the exact absolute path. All are newly created files:

```text
.gitattributes
.gitignore
pom.xml
README.md
scripts/maven.ps1
src/main/java/io/echohide/EchoHide.java
src/main/java/io/echohide/config/ProjectConfig.java
src/main/resources/echohide.properties
src/test/java/io/echohide/config/ProjectConfigTest.java
docs/PHASE_1.md
docs/REQUIREMENTS.md
docs/ARCHITECTURE.md
docs/REPOSITORY_TREE.md
docs/ROADMAP.md
docs/IMPLEMENTATION.md
docs/PAYLOAD_FORMAT.md
docs/NETWORK_PROTOCOL.md
docs/TESTING.md
docs/EXPERIMENTS.md
docs/DEMO_GUIDE.md
docs/VIVA_QA.md
docs/VERIFICATION.md
docs/screenshots/README.md
samples/README.md
results/README.md
```

`.git/` was initialized with branch `main`; no commits or remote were created. `.tools/` and `target/` are generated and ignored. [Complete final tree](REPOSITORY_TREE.md) also lists later files with phase labels. The source synopsis and synced `sources/` were untouched.

## C Implementation

The listed Java files contain the complete, compilable Phase 1 implementation with packages/imports. `ProjectConfig` validates public settings at construction and loads a bundled properties resource. The entry point prints help or performs the configuration check. Maven builds an executable JAR. No empty method bodies, fake audio functionality or unfinished backend stubs are included. Future service method contracts are specified in [architecture](ARCHITECTURE.md).

## D Explanation

Keep encryption independent of hiding, and both independent of transport. WAV validation must protect container boundaries and sample encoding. The versioned hidden header contains the parameters required for extraction and decryption; GCM authenticates those parameters after bounded parsing. TCP needs application framing because it exposes an ordered stream, not one message per write. SHA-256 verifies matching file bytes, while the receiver ACK confirms that a verified file was published.

The [payload format](PAYLOAD_FORMAT.md) explains one-bit changes, synchronization, boundaries, MP3 fragility and capacity arithmetic. The [network protocol](NETWORK_PROTOCOL.md) defines sizes, EOF, ACKs, naming and error handling. These are contracts for later phases, not evidence they work yet.

## E Execution and project initialization

The Maven project is already initialized directly through `pom.xml` and standard `src/main`/`src/test` directories. An archetype generator is unnecessary and would introduce unrelated generated files. On this supplied copy, run:

```powershell
Set-Location 'C:\Users\sayan\.codex\.chatgpt-projects\g-p-6acffa50b6e481919e25a9ac73145ccc\EchoHide'
java -version
javac -version
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\maven.ps1 clean verify
java -jar .\target\echohide-0.1.0-SNAPSHOT.jar --check
java -jar .\target\echohide-0.1.0-SNAPSHOT.jar --help
git status --short
```

To initialize a new empty location before copying the Phase 1 files (choose a destination that does not already exist):

```powershell
$projectDestination = Join-Path $env:USERPROFILE 'EchoHide'
if (Test-Path -LiteralPath $projectDestination) { throw 'Choose a new project destination.' }
New-Item -ItemType Directory -Path $projectDestination | Out-Null
Set-Location -LiteralPath $projectDestination
git init -b main
New-Item -ItemType Directory -Path 'src/main/java/io/echohide/config','src/main/resources','src/test/java/io/echohide/config','scripts','docs','samples','results' | Out-Null
# Copy the delivered pom.xml, source, script, and documentation files into this tree.
# Then execute the build command above from the new project root.
```

Do not run Maven archetype generation over the supplied repository. Git identity configuration is needed only if you choose to commit; the project does not assume your name or email.

## F Testing

Expected: Maven compiles Java 21, 16 foundation test invocations pass, JAR `--check` prints validated public settings with exit 0, `--help` exits 0, and unknown arguments exit 2. These checks were executed; [verification](VERIFICATION.md) records observed results. Core functional tests remain scheduled in [testing](TESTING.md).

## G Completion checklist

- [x] Synopsis read and requirements reconciled with the user's refinements.
- [x] Mandatory/optional/future boundaries and acceptance criteria documented.
- [x] Final tree and package dependency/sequence diagrams supplied.
- [x] Module contracts and versioned binary layouts defined.
- [x] Roadmap contains phase estimates, acceptance criteria and commit suggestions.
- [x] Maven project, Java entry point and public configuration implemented.
- [x] Actual build, 16 JUnit tests and executable checks passed.
- [x] Generated outputs ignored; no user/synced source files changed.
- [x] Future backend work explicitly marked planned.
- [x] Stop before Phase 2 pending user confirmation.

## H Suggested Git commit

`chore: establish EchoHide requirements and Java 21 foundation`

After reviewing the repository, you may use:

```powershell
git add .gitattributes .gitignore pom.xml README.md scripts src docs samples results
git commit -m 'chore: establish EchoHide requirements and Java 21 foundation'
```

These commit commands are suggestions; no commit has been made. The pause comes directly from the user's pasted request: "After Phase 1, stop and wait for my confirmation before proceeding to Phase 2."
