# Rename Shizuku -> custom package (anti-detection). ASCII only (PS 5.1 safe).
# Version 2.2 (2026-09-02):
#   v2.1: skip BinderContainer.java in content-replace (keep moe.shizuku.api package line untouched)
#   v2.2: copy step excludes build products (build/out/.gradle/.idea/.kotlin) to avoid stale
#         artifacts/mapping mixing into incremental builds; add leftover moe.shizuku self-test at end
#         (protected protocol values will show up - review output manually).
# Usage:
#   .\rename_shizuku.ps1 -Src "C:\path\Shizuku" -Dst "C:\out\custom" -NewPkg "com.abc.helper"
param(
    [Parameter(Mandatory = $true)][string]$Src,
    [Parameter(Mandatory = $true)][string]$Dst,
    [Parameter(Mandatory = $true)][string]$NewPkg,
    [string]$NewServer = "",
    [string]$NewProc = ""
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $Src)) { throw "Source dir not found: $Src" }
if (-not ($NewPkg -match '^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$')) {
    throw "NewPkg must be a valid package, e.g. com.abc.helper"
}
if (Test-Path $Dst) {
    if (Get-ChildItem -Path $Dst -Force | Select-Object -First 1) { throw "Dest dir not empty: $Dst" }
} else {
    New-Item -ItemType Directory -Path $Dst -Force | Out-Null
}
if ($NewServer -eq "") { $NewServer = "$NewPkg.server" }
if ($NewProc -eq "")   { $NewProc = ($NewPkg.Split('.')[-1]) + "_server" }

Write-Host "==> rename_shizuku.ps1 v2.2 | target pkg: $NewPkg"

# ---- 0. Copy project: exclude .git and build-product dirs (stale artifacts/mapping would
#      corrupt incremental builds of the renamed copy) ----
Write-Host "==> copy $Src -> $Dst (excl. build/out/.gradle/.idea/.kotlin)"
$srcRoot = $Src.TrimEnd('\')
robocopy $srcRoot $Dst /E /XD .git build out .gradle .idea .kotlin /NFL /NDL /NJH /NJS /NP | Out-Null
if ($LASTEXITCODE -ge 8) { throw "robocopy failed with exit code $LASTEXITCODE" }
$LASTEXITCODE = 0

$OldPkg        = "moe.shizuku"
$OldServer     = "moe.shizuku.server"
$OldManager    = "moe.shizuku.privileged.api"
$OldProc       = "shizuku_server"
$OldStarter    = "moe.shizuku.starter"
$OldApi        = "moe.shizuku.api"
$OldManagerPkg = "moe.shizuku.manager"

Write-Host "==> rename $OldPkg -> $NewPkg (server $OldServer -> $NewServer, proc $OldProc -> $NewProc)"

# ---- 1.5 Protected protocol values: these MUST stay unchanged, otherwise the custom
#      server cannot push binder to DT's official ShizukuProvider (EXTRA key / ACTION /
#      V3 meta-data must match on both ends). Tokenize before global replace, restore after.
$protected = @(
    'moe.shizuku.api.action.BINDER_RECEIVED',
    'moe.shizuku.privileged.api.intent.extra.BINDER',
    'moe.shizuku.client.V3_SUPPORT',
    'moe.shizuku.client.V3_REQUIRES_ROOT',
    # BinderContainer is the Parcelable class DT's *official* ShizukuProvider casts to when receiving the
    # pushed binder. If renamed (-> <NewPkg>.api.BinderContainer), DT loads it by that class name and the
    # provider's cast to moe.shizuku.api.BinderContainer throws ClassCastException, breaking the binder push.
    # Keep the FQN AND the source file unrenamed (moe/shizuku/api/BinderContainer.java).
    'moe.shizuku.api.BinderContainer'
)
$toTokens = @{}
$i = 0
foreach ($p in $protected) {
    $token = "__PROTECTED_${i}__"
    $toTokens[$p] = $token
    $i++
}

# ---- 1. Replace file contents ----
$exts = @('.java', '.kt', '.aidl', '.xml', '.gradle', '.gradle.kts', '.pro', '.cpp', '.h', '.md', '.txt')
$files = Get-ChildItem -Path $Dst -Recurse -File -ErrorAction SilentlyContinue |
    Where-Object {
        $_.FullName -notmatch '\\.git\\|\\build\\|\\out\\|\\\.gradle\\' -and
        $_.Extension -and ($exts -contains $_.Extension.ToLower())
    }

$count = 0
foreach ($f in $files) {
    # v2.1: BinderContainer.java keeps its moe.shizuku.api package declaration untouched.
    # Content-replace would rewrite its `package moe.shizuku.api;` line (not covered by the
    # FQN token below), breaking the 4 referencing files that import the protected FQN.
    if ($f.Name -eq 'BinderContainer.java') { continue }
    $content = Get-Content -Path $f.FullName -Raw -Encoding UTF8
    if ($null -eq $content) { continue }
    if ($content -notmatch [regex]::Escape($OldPkg) -and $content -notmatch [regex]::Escape($OldProc) -and $content -notmatch 'moe/shizuku') { continue }
    $new = $content
    # protect: token first so the global replace below won't touch protocol values
    foreach ($p in $protected) { if ($new -match [regex]::Escape($p)) { $new = $new.Replace($p, $toTokens[$p]) } }
    $new = $new.Replace($OldServer, $NewServer)
    $new = $new.Replace($OldManager, $NewPkg)
    $new = $new.Replace($OldStarter, "$NewPkg.starter")
    $new = $new.Replace($OldApi, "$NewPkg.api")
    $new = $new.Replace($OldManagerPkg, "$NewPkg.manager")
    $new = $new.Replace($OldPkg, $NewPkg)
    # native (jni .cpp/.h) JNI class paths are slash-separated: moe/shizuku/... -> <NewPkg.with dots->slashes>. / ...
    $new = $new.Replace('moe/shizuku', $NewPkg.Replace('.', '/'))
    $new = $new.Replace($OldProc, $NewProc)
    $new = $new.Replace("shizuku.library.path", "$($NewPkg.Split('.')[-1]).library.path")
    # restore protected protocol values
    foreach ($p in $protected) { $new = $new.Replace($toTokens[$p], $p) }
    if ($new -ne $content) {
        [System.IO.File]::WriteAllText($f.FullName, $new, (New-Object System.Text.UTF8Encoding($false)))
        $count++
    }
}
Write-Host "==> replaced $count files (protected $($protected.Count) protocol values)"

# ---- 2. Move directories moe/shizuku/** -> NewPkg/** ----
$srcDirs = @(
    'api\aidl\src\main\aidl',
    'api\provider\src\main\java',
    'api\shared\src\main\java',
    'starter\src\main\java',
    'server\src\main\java',
    'manager\src\main\java',
    'common\src\main\java'
)
# BinderContainer must stay at moe/shizuku/api (its FQN is protected, see $protected). Keep it aside
# so the directory move below doesn't relocate it under <NewPkg>.api.
$bcRel = 'api\provider\src\main\java\moe\shizuku\api\BinderContainer.java'
$bcPath = Join-Path $Dst $bcRel
$bcTmp = Join-Path $Dst 'BinderContainer.java.__keep'
if (Test-Path $bcPath) { Move-Item -Path $bcPath -Destination $bcTmp -Force }

foreach ($d in $srcDirs) {
    $oldDir = Join-Path $Dst "$d\moe\shizuku"
    if (Test-Path $oldDir) {
        $newBase = Join-Path $Dst $d
        $dest    = Join-Path $newBase ($NewPkg.Replace('.', '\'))
        New-Item -ItemType Directory -Path $dest -Force | Out-Null
        Get-ChildItem -Path $oldDir -Force | ForEach-Object {
            Copy-Item -Path $_.FullName -Destination $dest -Recurse -Force
        }
        Remove-Item -Path $oldDir -Recurse -Force
        Write-Host "==> moved moe/shizuku -> $NewPkg (under $d)"
    }
}

# Restore BinderContainer.java to its original (unrenamed) package location
if (Test-Path $bcTmp) {
    New-Item -ItemType Directory -Path (Split-Path $bcPath) -Force | Out-Null
    Move-Item -Path $bcTmp -Destination $bcPath -Force
    Write-Host "==> kept moe/shizuku/api/BinderContainer.java (unrenamed)"
}

# ---- 3. Self-test: scan leftover moe.shizuku mentions ----
# Expected hits (no action needed):
#   - BinderContainer.java is skipped by name, so it never shows up here;
#   - protected protocol values (BINDER_RECEIVED / EXTRA_BINDER / V3_SUPPORT / V3_REQUIRES_ROOT /
#     references to moe.shizuku.api.BinderContainer) intentionally keep the original spelling;
#   - out\mapping\* never shows up (build/out excluded at copy step).
$leftover = Get-ChildItem -Path $Dst -Recurse -File -Include *.java,*.kt,*.aidl,*.xml,*.cpp,*.h,*.pro -ErrorAction SilentlyContinue |
    Select-String -Pattern 'moe\.shizuku|moe/shizuku|shizuku_server' -List -ErrorAction SilentlyContinue
$leftover | ForEach-Object { Write-Host "  leftover> $($_.Path -replace [regex]::Escape($Dst), '.')" }
if ($leftover) {
    Write-Host "==> self-test: $($leftover.Count) file(s) still mention moe.shizuku (review above; protected protocol values expected)"
} else {
    Write-Host "==> self-test OK: no moe.shizuku leftover"
}

Write-Host "==> done. Output: $Dst"
