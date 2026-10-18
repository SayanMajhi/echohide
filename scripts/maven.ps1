# Repository-local launcher, not the Apache Maven Wrapper.
# Java 21 must already be available. Maven is downloaded once under .tools.
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$toolsRoot = Join-Path $projectRoot '.tools'
$mavenVersion = '3.9.9'
$expectedHash = '8beac8d11ef208f1e2a8df0682b9448a9a363d2ad13ca74af43705549e72e74c9378823bf689287801cbbfc2f6ea9596201d19ccacfdfb682ee8a2ff4c4418ba'
$mavenHome = Join-Path $toolsRoot "apache-maven-$mavenVersion"
$mavenCommand = Join-Path $mavenHome 'bin\mvn.cmd'
if (-not (Test-Path -LiteralPath $mavenCommand)) {
    New-Item -ItemType Directory -Path $toolsRoot -Force | Out-Null
    $archivePath = Join-Path $toolsRoot "apache-maven-$mavenVersion-bin.zip"
    if (-not (Test-Path -LiteralPath $archivePath)) {
        $downloadPath = Join-Path $toolsRoot ([guid]::NewGuid().ToString() + '.download')
        try {
            Invoke-WebRequest -UseBasicParsing -Uri "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$mavenVersion/apache-maven-$mavenVersion-bin.zip" -OutFile $downloadPath
            $actualHash = (Get-FileHash -LiteralPath $downloadPath -Algorithm SHA512).Hash
            if ($actualHash -ine $expectedHash) { throw 'Maven archive SHA-512 mismatch.' }
            Move-Item -LiteralPath $downloadPath -Destination $archivePath
        } finally {
            if (Test-Path -LiteralPath $downloadPath) { Remove-Item -LiteralPath $downloadPath }
        }
    }
    if ((Get-FileHash -LiteralPath $archivePath -Algorithm SHA512).Hash -ine $expectedHash) {
        throw 'Cached Maven archive SHA-512 mismatch. Inspect .tools before retrying.'
    }
    Expand-Archive -LiteralPath $archivePath -DestinationPath $toolsRoot
}
Push-Location $projectRoot
try {
    & $mavenCommand @args
    $mavenExitCode = $LASTEXITCODE
} finally {
    Pop-Location
}
exit $mavenExitCode
