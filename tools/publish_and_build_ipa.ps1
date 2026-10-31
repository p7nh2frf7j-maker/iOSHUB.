# 一键把 ioshub 送到 GitHub，让 macos-15 runner 编出未签名 IPA，再自动下载回来。
#
# 前提（两件）：
#   1. 本机能访问 api.github.com（现在被墙，需要开代理 / VPN）
#   2. 一个 GitHub Personal Access Token，至少要有 repo 权限
#      Fine-grained token: Repository access = All repositories，权限 Contents: Read and write
#                          + Actions: Read and write
#      Classic token:     勾 repo + workflow
#
# 用法：
#   pwsh -File tools\publish_and_build_ipa.ps1 -Token "ghp_xxxx"
#   pwsh -File tools\publish_and_build_ipa.ps1 -Token "ghp_xxxx" -Proxy "http://127.0.0.1:7890"
#   pwsh -File tools\publish_and_build_ipa.ps1 -Token "ghp_xxxx" -Owner myname -Repo ioshub -Private
#
param(
    [Parameter(Mandatory = $true)][string]$Token,
    [string]$Owner = "",
    [string]$Repo = "ioshub",
    [string]$Proxy = "",
    [string]$Branch = "main",
    [string]$Workflow = "ios.yml",
    [switch]$Private,
    [string]$Workspace = ""
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
$api = "https://api.github.com"
$headers = @{
    Authorization        = "Bearer $Token"
    Accept               = "application/vnd.github+json"
    "X-GitHub-Api-Version" = "2022-11-28"
    "User-Agent"         = "ioshub-pack"
}

if (-not $Workspace) {
    $Workspace = Split-Path -Parent $PSScriptRoot
}
$Workspace = (Resolve-Path $Workspace).Path
Write-Host "工作区: $Workspace" -ForegroundColor DarkGray

function Invoke-GitHub {
    param(
        [string]$Method = "GET",
        [string]$Path,
        $Body = $null
    )
    $arg = @{
        Method    = $Method
        Uri       = "$api$Path"
        Headers   = $headers
        TimeoutSec = 60
    }
    if ($Proxy -ne "") { $arg["Proxy"] = $Proxy }
    if ($null -ne $Body) {
        $arg["Body"] = ($Body | ConvertTo-Json -Depth 20 -Compress)
        $arg["ContentType"] = "application/json; charset=utf-8"
    }
    try {
        return Invoke-RestMethod @arg
    }
    catch {
        $code = 0
        if ($null -ne $_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode }
        throw "GitHub API 失败: $Method $Path HTTP=$code $($_.ErrorDetails.Message)"
    }
}

Write-Host "== 1/7 校验 token ==" -ForegroundColor Cyan
$me = Invoke-GitHub -Path "/user"
Write-Host "   已登录: $($me.login)"
if (-not $Owner) { $Owner = $me.login }

Write-Host "== 2/7 确认仓库 ==" -ForegroundColor Cyan
$repoExists = $true
try {
    $null = Invoke-GitHub -Path "/repos/$Owner/$Repo"
}
catch {
    $repoExists = $false
}
if (-not $repoExists) {
    $newBody = @{ name = $Repo; private = [bool]$Private; auto_init = $false; description = "iOS HUB - SwiftUI app + GitHub Actions IPA builder" }
    $null = Invoke-GitHub -Method POST -Path "/user/repos" -Body $newBody
    Write-Host "   已创建仓库 $Owner/$Repo"
}
else {
    Write-Host "   仓库已存在，直接复用"
}

Write-Host "== 3/7 收集源码 ==" -ForegroundColor Cyan
$dirs = @("iOSHUB", "iOSHUB.xcodeproj", ".github")
$single = @("project.yml", "README.md")
$files = @()
foreach ($d in $dirs) {
    $full = Join-Path $Workspace $d
    if (Test-Path $full) {
        $item = Get-Item -LiteralPath $full -Force
        if ($item.PSIsContainer) {
            $files += Get-ChildItem -LiteralPath $full -Recurse -File -Force
        }
        else {
            $files += $item
        }
    }
}
foreach ($s in $single) {
    $full = Join-Path $Workspace $s
    if (Test-Path $full) { $files += Get-Item -LiteralPath $full -Force }
}

$tree = @()
foreach ($f in $files) {
    $rel = $f.FullName.Substring($Workspace.Length + 1).Replace("\", "/")
    if ($rel -match "\.DS_Store$") { continue }
    $b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($f.FullName))
    $tree += @{ path = $rel; mode = "100644"; type = "blob"; content = $b64 }
}
if ($tree.Count -eq 0) { throw "没收集到任何文件，检查 -Workspace 参数" }
Write-Host "   共 $($tree.Count) 个文件"

Write-Host "== 4/7 上传 blob tree ==" -ForegroundColor Cyan
$treeResp = Invoke-GitHub -Method POST -Path "/repos/$Owner/$Repo/git/trees" -Body @{ tree = $tree }
Write-Host "   tree sha: $($treeResp.sha)"

Write-Host "== 5/7 建提交并推分支 ==" -ForegroundColor Cyan
$commitResp = Invoke-GitHub -Method POST -Path "/repos/$Owner/$Repo/git/commits" -Body @{
    message = "iOS HUB: import $( $tree.Count ) files"
    tree    = $treeResp.sha
    parents = @()
}
$refPath = "/repos/$Owner/$Repo/git/refs/heads/$Branch"
$refMade = $true
try {
    $null = Invoke-GitHub -Method POST -Path "/repos/$Owner/$Repo/git/refs" -Body @{ ref = "refs/heads/$Branch"; sha = $commitResp.sha }
}
catch {
    $refMade = $false
}
if (-not $refMade) {
    $null = Invoke-GitHub -Method PATCH -Path $refPath -Body @{ sha = $commitResp.sha; force = $true }
}
Write-Host "   已推送到 $Branch : $($commitResp.sha.Substring(0, 8))"
Write-Host "   仓库地址: https://github.com/$Owner/$Repo"

Write-Host "== 6/7 触发 macOS 编译 ==" -ForegroundColor Cyan
$dispatched = $false
$attempt = 0
while ((-not $dispatched) -and ($attempt -lt 12)) {
    $attempt = $attempt + 1
    try {
        $null = Invoke-GitHub -Method POST -Path "/repos/$Owner/$Repo/actions/workflows/$Workflow/dispatches" -Body @{ ref = $Branch }
        $dispatched = $true
    }
    catch {
        Write-Host "   工作流还没注册好, 5 秒后重试 (第 $attempt 次 / 共 12 次)"
        Start-Sleep -Seconds 5
    }
}
if (-not $dispatched) {
    throw "无法触发工作流, 请到 https://github.com/$Owner/$Repo/actions 手动点 Run workflow"
}

Write-Host "   已触发，等待 runner 接单…"
Start-Sleep -Seconds 10
$runId = 0
$found = $false
$wait = 0
while ((-not $found) -and ($wait -lt 30)) {
    $wait = $wait + 1
    $runs = Invoke-GitHub -Path "/repos/$Owner/$Repo/actions/runs?event=workflow_dispatch&per_page=5"
    $run = $runs.workflow_runs | Where-Object { $_.head_branch -eq $Branch } | Select-Object -First 1
    if ($null -ne $run) {
        $runId = $run.id
        $found = $true
    }
    else {
        Start-Sleep -Seconds 5
    }
}
if (-not $found) { throw "等不到运行记录, 请到 https://github.com/$Owner/$Repo/actions 查看" }
Write-Host "   run id: $runId"

$done = $false
$round = 0
while ((-not $done) -and ($round -lt 90)) {
    $round = $round + 1
    Start-Sleep -Seconds 20
    $cur = Invoke-GitHub -Path "/repos/$Owner/$Repo/actions/runs/$runId"
    Write-Host "   [$round] 状态: $($cur.status) / $($cur.conclusion)"
    if ($cur.status -eq "completed") { $done = $true }
}
if (-not $done) { throw "编译超时, 请到 https://github.com/$Owner/$Repo/actions 查看" }

$final = Invoke-GitHub -Path "/repos/$Owner/$Repo/actions/runs/$runId"
if ($final.conclusion -ne "success") {
    Write-Host "编译失败，结论: $($final.conclusion)" -ForegroundColor Red
    $jobs = Invoke-GitHub -Path "/repos/$Owner/$Repo/actions/runs/$runId/jobs"
    foreach ($j in $jobs.jobs) {
        Write-Host "  job: $($j.name) => $($j.conclusion)"
        foreach ($st in $j.steps) { Write-Host "     - $($st.name): $($st.conclusion)" }
    }
    Write-Host "  详情: $($final.html_url)"
    exit 1
}

Write-Host "== 7/7 下载 IPA ==" -ForegroundColor Cyan
$arts = Invoke-GitHub -Path "/repos/$Owner/$Repo/actions/runs/$runId/artifacts"
$art = $arts.artifacts | Where-Object { $_.name -eq "iOSHUB-unsigned-ipa" } | Select-Object -First 1
if ($null -eq $art) { throw "没找到 artifact, 请到 $($final.html_url) 手动下载" }

$zipPath = Join-Path $env:TEMP "ioshub-artifact.zip"
$dl = @{ Uri = "$api/repos/$Owner/$Repo/actions/artifacts/$($art.id)/zip"; Headers = $headers; OutFile = $zipPath; TimeoutSec = 300 }
if ($Proxy -ne "") { $dl["Proxy"] = $Proxy }
Invoke-WebRequest @dl

$tmpDir = Join-Path $env:TEMP "ioshub-artifact"
if (Test-Path $tmpDir) { Remove-Item $tmpDir -Recurse -Force }
Expand-Archive -Path $zipPath -DestinationPath $tmpDir -Force
$ipa = Get-ChildItem -LiteralPath $tmpDir -Recurse -Filter "*.ipa" | Select-Object -First 1
if ($null -eq $ipa) { throw "压缩包里没有 .ipa" }

$outDir = Join-Path $Workspace "dist"
if (-not (Test-Path $outDir)) { $null = New-Item -ItemType Directory -Path $outDir }
$dest = Join-Path $outDir "iOSHUB-unsigned.ipa"
Copy-Item -LiteralPath $ipa.FullName -Destination $dest -Force
$sizeMb = [math]::Round((Get-Item -LiteralPath $dest).Length / 1MB, 2)
Write-Host ""
Write-Host "完成: $dest ($sizeMb MB)" -ForegroundColor Green
Write-Host "运行页面: $($final.html_url)"
Write-Host ""
Write-Host "下一步: 用 Sideloadly 把它加你的 Apple ID 自签安装 (7 天续签), 或丢进 TrollStore 永久安装。"
