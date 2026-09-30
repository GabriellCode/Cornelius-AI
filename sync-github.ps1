# sync-github.ps1 - Sincronização Automática com o GitHub para o Cornelius AI
param(
    [string]$CommitMessage = ""
)

$ErrorActionPreference = "Stop"
$appDir = "E:\APP"
$logFile = "$appDir\github_sync.log"

function Log-Msg($msg) {
    $time = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $line = "[$time] $msg"
    Write-Host $line
    Add-Content -Path $logFile -Value $line -ErrorAction SilentlyContinue
}

Log-Msg "================================================="
Log-Msg "Iniciando sincronização automática com o GitHub..."

# 1. Carregar configurações de forma segura (sem expor segredos no repositório)
$token = ""
$repoFullName = "GabriellCode/Cornelius-AI"

$configFile = "$HOME\.cornelius\config.json"
if (Test-Path $configFile) {
    try {
        $json = Get-Content $configFile -Raw | ConvertFrom-Json
        if ($json.githubToken) { $token = $json.githubToken }
        if ($json.githubRepo) { $repoFullName = $json.githubRepo }
    } catch {}
}

if (-not $token) {
    $token = $env:GITHUB_TOKEN
}

if (-not $token) {
    $tokenFile = "$HOME\.cornelius\github.token"
    if (Test-Path $tokenFile) {
        $token = (Get-Content $tokenFile -Raw).Trim()
    }
}

if (-not $token) {
    Log-Msg "[ERRO] Token do GitHub não configurado no arquivo $configFile nem na variável GITHUB_TOKEN."
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Accept"        = "application/vnd.github.v3+json"
    "User-Agent"    = "Cornelius-Deployer"
}

# 2. Obter usuário e validar token
try {
    $user = Invoke-RestMethod -Uri "https://api.github.com/user" -Headers $headers -Method Get
    Log-Msg "Autenticado como: $($user.login) | Repositório: $repoFullName"
} catch {
    Log-Msg "[ERRO] Falha ao autenticar no GitHub: $($_.Exception.Message)"
    exit 1
}

# 3. Obter estado atual da branch main e da árvore remota
$parentCommitSha = $null
$remoteBlobs = @{}

try {
    $refRes = Invoke-RestMethod -Uri "https://api.github.com/repos/$repoFullName/git/refs/heads/main" -Headers $headers -Method Get
    $parentCommitSha = $refRes.object.sha
    $commitRes = Invoke-RestMethod -Uri "https://api.github.com/repos/$repoFullName/git/commits/$parentCommitSha" -Headers $headers -Method Get
    $treeSha = $commitRes.tree.sha
    $remoteTreeRes = Invoke-RestMethod -Uri "https://api.github.com/repos/$repoFullName/git/trees/$treeSha?recursive=1" -Headers $headers -Method Get
    foreach ($item in $remoteTreeRes.tree) {
        if ($item.type -eq "blob") {
            $remoteBlobs[$item.path] = $item.sha
        }
    }
    Log-Msg "Árvore remota carregada ($($remoteBlobs.Count) arquivos existentes no GitHub)."
} catch {
    Log-Msg "Aviso: Criando nova base para a branch main."
}

# 4. Mapear arquivos locais respeitando .gitignore e excluindo binários e logs
$ignorePatterns = @(
    '\.class$', '\\Backend\\bin\\', '\\bin\\', '\.jar$', '\.war$', '\.ear$',
    '\.log$', '\.pid$', 'discord_bot\.log$', 'out\.log$', 'error\.log$', 'github_sync\.log$',
    'sources\.txt$', '\.tmp$', '\.bak$', '\\dist\\', '\\dist-windows-portable\\',
    '\\Cornelius-Windows-Portable\\', '\.zip$', '\.msi$', '\.exe$', '\\Cornelius_Vault\\',
    '\.czip$', '\\Android\\\.gradle\\', '\\Android\\build\\', '\\Android\\app\\build\\',
    '\\Android\\\.idea\\', 'local\.properties$', '\.apk$', '\.aab$', '\\\.gradle\\',
    '\\build\\', '\\\.idea\\', '\.iml$', '\\\.vscode\\', 'test_script\.js$', '\\\.git\\',
    '\.env$', 'cornelius\.properties$'
)

$localFiles = Get-ChildItem -Path $appDir -Recurse -File | Where-Object {
    $ignore = $false
    foreach ($pat in $ignorePatterns) {
        if ($_.FullName -match $pat) { $ignore = $true; break }
    }
    -not $ignore
}

Log-Msg "Arquivos locais válidos para sincronização: $($localFiles.Count)"

function Get-GitBlobSha {
    param([byte[]]$bytes)
    $header = [System.Text.Encoding]::ASCII.GetBytes("blob $($bytes.Length)`0")
    $all = New-Object byte[] ($header.Length + $bytes.Length)
    [Buffer]::BlockCopy($header, 0, $all, 0, $header.Length)
    [Buffer]::BlockCopy($bytes, 0, $all, $header.Length, $bytes.Length)
    $sha1 = [System.Security.Cryptography.SHA1]::Create()
    $hash = $sha1.ComputeHash($all)
    return (-join ($hash | ForEach-Object { "{0:x2}" -f $_ }))
}

$treeItems = @()
$uploadedCount = 0
$reusedCount = 0

foreach ($file in $localFiles) {
    $relPath = $file.FullName.Substring($appDir.Length).TrimStart('\', '/').Replace('\', '/')
    $bytes = [System.IO.File]::ReadAllBytes($file.FullName)
    $localSha = Get-GitBlobSha $bytes

    $blobSha = $null
    if ($remoteBlobs.ContainsKey($relPath) -and $remoteBlobs[$relPath] -eq $localSha) {
        # Arquivo idêntico ao remoto, reutiliza blob sem gastar rede
        $blobSha = $localSha
        $reusedCount++
    } else {
        # Arquivo novo ou alterado, enviar blob
        $b64 = [Convert]::ToBase64String($bytes)
        $blobBody = @{
            content  = $b64
            encoding = "base64"
        } | ConvertTo-Json

        $newBlob = Invoke-RestMethod -Uri "https://api.github.com/repos/$repoFullName/git/blobs" -Headers $headers -Method Post -Body $blobBody
        $blobSha = $newBlob.sha
        $uploadedCount++
        Log-Msg "-> Enviado ($uploadedCount): $relPath"
    }

    $mode = if ($relPath.EndsWith(".sh") -or $relPath -eq "gradlew" -or $relPath -eq "Android/gradlew") { "100755" } else { "100644" }
    $treeItems += @{
        path = $relPath
        mode = $mode
        type = "blob"
        sha  = $blobSha
    }
}

Log-Msg "Blobs prontos: $uploadedCount enviados, $reusedCount mantidos idênticos."

if ($uploadedCount -eq 0 -and $remoteBlobs.Count -eq $localFiles.Count) {
    Log-Msg "Tudo já está 100% atualizado no GitHub! Nenhuma alteração pendente."
    Log-Msg "Link: https://github.com/$repoFullName"
    exit 0
}

# 5. Criar Git Tree
Log-Msg "Criando árvore Git no GitHub..."
$treeBody = @{ tree = $treeItems } | ConvertTo-Json -Depth 5
$treeRes = Invoke-RestMethod -Uri "https://api.github.com/repos/$repoFullName/git/trees" -Headers $headers -Method Post -Body $treeBody

# 6. Criar Commit
$nowStr = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$msg = if ($CommitMessage -and $CommitMessage.Trim()) { $CommitMessage.Trim() } else { "chore(sync): auto-sync from Cornelius app ($nowStr)" }

$commitData = @{
    message = $msg
    tree    = $treeRes.sha
}
if ($parentCommitSha) {
    $commitData["parents"] = @( $parentCommitSha )
}

$commitBody = $commitData | ConvertTo-Json
$newCommit = Invoke-RestMethod -Uri "https://api.github.com/repos/$repoFullName/git/commits" -Headers $headers -Method Post -Body $commitBody
Log-Msg "Commit criado: $($newCommit.sha) ('$msg')"

# 7. Atualizar branch 'main'
$refUpdateBody = @{
    sha   = $newCommit.sha
    force = $true
} | ConvertTo-Json

try {
    $refRes = Invoke-RestMethod -Uri "https://api.github.com/repos/$repoFullName/git/refs/heads/main" -Headers $headers -Method Patch -Body $refUpdateBody
    Log-Msg "Branch 'main' atualizada com sucesso para $($newCommit.sha)!"
} catch {
    Log-Msg "Criando ref refs/heads/main..."
    $refCreateBody = @{
        ref = "refs/heads/main"
        sha = $newCommit.sha
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "https://api.github.com/repos/$repoFullName/git/refs" -Headers $headers -Method Post -Body $refCreateBody | Out-Null
}

Log-Msg "================================================="
Log-Msg "SINCRONIZACAO COM GITHUB CONCLUIDA COM SUCESSO!"
Log-Msg "Link: https://github.com/$repoFullName"
Log-Msg "================================================="
