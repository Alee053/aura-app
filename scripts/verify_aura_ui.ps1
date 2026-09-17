param(
    [string]$Serial = 'emulator-5556',
    [switch]$SkipBuild,
    [string]$TestClass = 'com.programovil.aura.ui.AuraVisualTest',
    [string]$Label = 'phone',
    [string]$Locale = 'en'
)
$ErrorActionPreference = 'Stop'
if (-not $Serial.StartsWith('emulator-')) { throw 'This fixture workflow is limited to an emulator, never a physical device.' }
$taskRoot = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $taskRoot
$sdkPath = if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
$adbPath = Join-Path $sdkPath 'platform-tools/adb.exe'
if (-not $SkipBuild) {
    & .\gradlew.bat :composeApp:assembleDebug :composeApp:testDebugUnitTest :composeApp:assembleDebugAndroidTest --max-workers=2 --no-daemon -Pkotlin.compiler.execution.strategy=in-process --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Build or unit tests failed.' }
}
$deviceState = & $adbPath -s $Serial get-state
if ($deviceState.Trim() -ne 'device') { throw 'Start the read-only QA emulator before running this script.' }
& $adbPath -s $Serial shell cmd connectivity airplane-mode enable
& $adbPath -s $Serial shell svc wifi disable
& $adbPath -s $Serial install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
if ($LASTEXITCODE -ne 0) { throw 'Application installation failed.' }
& $adbPath -s $Serial install -r composeApp/build/outputs/apk/androidTest/debug/composeApp-debug-androidTest.apk
if ($LASTEXITCODE -ne 0) { throw 'Test installation failed.' }
$qaPath = Join-Path $taskRoot 'docs/ui-redesign/qa'
$testOutput = & $adbPath -s $Serial shell am instrument -w -e class $TestClass -e aura.locale $Locale -e aura.label $Label com.programovil.aura.test/com.programovil.aura.ui.AuraUiTestRunner 2>&1
$testOutput | Out-File -LiteralPath (Join-Path $qaPath "instrumentation-$Label.txt") -Encoding utf8
$capturesPath = Join-Path $qaPath 'captures/ui-qa'
New-Item -ItemType Directory -Path $capturesPath -Force | Out-Null
& $adbPath -s $Serial pull /sdcard/Android/data/com.programovil.aura/files/ui-qa/. $capturesPath
$testOutput | Select-Object -Last 15
if (($testOutput -join "`n") -notmatch 'OK \(\d+ tests?\)') { throw 'Instrumentation failed. Read the saved output and captures.' }
