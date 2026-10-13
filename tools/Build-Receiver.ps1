param([switch]$Offline)

$ErrorActionPreference = 'Stop'
$taskWorkspace = Split-Path -Parent $PSScriptRoot
$taskSource = Join-Path $taskWorkspace '.tooling\receiver-source'
$taskRevision = '4ee1d58f6cf7f39f7dc0a6ef93509553401ec6aa'
$taskPatch = Join-Path $taskWorkspace 'third_party\open-headunit\receiver.patch'

function Invoke-ReceiverGit([string[]]$taskArguments) {
    & git -C $taskSource -c "safe.directory=$taskSource" @taskArguments
    if ($LASTEXITCODE -ne 0) { throw "Receiver Git command failed: $taskArguments" }
}

if (-not (Test-Path -LiteralPath $taskSource)) {
    & git clone --no-checkout --filter=blob:none https://github.com/andreknieriem/open-headunit.git $taskSource
    if ($LASTEXITCODE -ne 0) { throw 'Receiver source download failed' }
    Invoke-ReceiverGit @('fetch', '--depth', '1', 'origin', $taskRevision)
    Invoke-ReceiverGit @('checkout', '--detach', $taskRevision)
}

$taskHead = & git -C $taskSource -c "safe.directory=$taskSource" rev-parse HEAD
if ($LASTEXITCODE -ne 0 -or $taskHead -ne $taskRevision) { throw 'Receiver source must be at the pinned revision; existing work was preserved' }
& git -C $taskSource -c "safe.directory=$taskSource" apply --reverse --check $taskPatch 2>$null
if ($LASTEXITCODE -ne 0) {
    Invoke-ReceiverGit @('apply', '--check', $taskPatch)
    Invoke-ReceiverGit @('apply', $taskPatch)
}

$env:JAVA_HOME = Join-Path $taskWorkspace '.tooling\jdk\jdk-17.0.20.1+1'
$env:ANDROID_SDK_ROOT = Join-Path $taskWorkspace '.tooling\android-sdk'
$env:GRADLE_USER_HOME = Join-Path $taskWorkspace '.tooling\gradle-home'
$taskArguments = @('-p', $taskSource, 'clean', ':app:assembleGithubDebug', '-Pandroid.injected.build.abi=arm64-v8a', '-Pkotlin.incremental=false', '--no-daemon')
if ($Offline) { $taskArguments += '--offline' }
& (Join-Path $taskSource 'gradlew.bat') @taskArguments
if ($LASTEXITCODE -ne 0) { throw 'Receiver APK build failed' }
$taskApks = @(Get-ChildItem -LiteralPath (Join-Path $taskSource 'app\build') -Recurse -Filter 'com.teslahub.receiver_3.5.0-beta4-teslahub1_debug.apk')
if ($taskApks.Count -ne 1) { throw 'Expected exactly one receiver APK after a successful build' }
$taskArtifact = Join-Path $taskWorkspace '.tooling\teslahub-receiver-debug.apk'
Copy-Item -LiteralPath $taskApks[0].FullName -Destination $taskArtifact
Get-Item -LiteralPath $taskArtifact | Select-Object FullName, Length
