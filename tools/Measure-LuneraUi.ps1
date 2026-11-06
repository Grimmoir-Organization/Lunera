param(
    [ValidateSet('Start', 'Finish')]
    [string]$Action = 'Start',
    [string]$OutputDirectory = '.\lunera-ui-capture'
)

$ErrorActionPreference = 'Stop'
$packageName = 'org.grimmoir.lunera'
if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    throw 'Add Android platform-tools to PATH first.'
}
adb get-state
if ($LASTEXITCODE -ne 0) { throw 'Connect and authorize one Android device.' }
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null

if ($Action -eq 'Start') {
    adb shell dumpsys gfxinfo $packageName reset | Out-Null
    adb shell getprop ro.product.model | Set-Content (Join-Path $OutputDirectory 'device.txt')
    adb shell dumpsys package $packageName |
        Select-String 'versionCode=|versionName=' |
        Set-Content (Join-Path $OutputDirectory 'version.txt')
    Get-Date -Format o | Set-Content (Join-Path $OutputDirectory 'started.txt')
    Write-Host 'Now repeat the startup and navigation actions from the recording.'
    Write-Host 'When finished, run this script with -Action Finish and the same output directory.'
} else {
    adb shell dumpsys gfxinfo $packageName framestats |
        Set-Content (Join-Path $OutputDirectory 'frame-stats.txt')
    $appProcess = (adb shell pidof $packageName | Out-String).Trim()
    if ($appProcess -match '^\d+$') {
        adb logcat -d --pid=$appProcess -v threadtime |
            Set-Content (Join-Path $OutputDirectory 'app-logcat.txt')
    }
    Get-Date -Format o | Set-Content (Join-Path $OutputDirectory 'finished.txt')
    Write-Host "Saved frame statistics and available app logs to $OutputDirectory."
}
