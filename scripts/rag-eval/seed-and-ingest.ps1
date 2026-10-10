# Seed eval tenants, users, upload corpus, wait READY, publish
param(
    [string]$BaseUrl = 'http://localhost:8080',
    [switch]$SkipIfStateExists,
    [int]$ReadyTimeoutSec = 300
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'EvalApi.ps1')

$root = Get-EvalRepoRoot
$manifestPath = Join-Path $root 'docs/rag-eval/manifest.json'
$corpusDir = Join-Path $root 'docs/rag-eval/corpus'
$stateDir = Join-Path $root 'docs/rag-eval/state'
$statePath = Join-Path $stateDir 'ingested.json'

if (-not (Test-Path $manifestPath)) { throw "Missing manifest: $manifestPath" }
if ($SkipIfStateExists -and (Test-Path $statePath)) {
    Write-Host "State exists, skip: $statePath"
    exit 0
}

$manifest = Get-Content -Raw -Encoding UTF8 $manifestPath | ConvertFrom-Json
$password = $manifest.password

New-Item -ItemType Directory -Force -Path $stateDir | Out-Null

$platformToken = Get-EvalAccessToken -BaseUrl $BaseUrl -Username 'admin' -Password '123456'

$tenantAdminTokens = @{}
foreach ($tenant in $manifest.tenants) {
    $email = $tenant.admin.email
    $display = $tenant.admin.displayName
    $regToken = Register-EvalTenant -BaseUrl $BaseUrl -TenantCode $tenant.code -TenantName $tenant.name `
        -Email $email -Password $password -DisplayName $display
    if ($regToken) {
        Write-Host "Registered tenant $($tenant.code)"
        $adminToken = $regToken
    } else {
        Write-Host "Tenant $($tenant.code) exists, login admin"
        $adminToken = Get-EvalAccessToken -BaseUrl $BaseUrl -Username $email -Password $password
    }
    $tenantAdminTokens[$tenant.code] = $adminToken

    foreach ($invite in $tenant.invites) {
        try {
            $raw = Invite-EvalUser -BaseUrl $BaseUrl -AdminToken $adminToken -Email $invite.email -Role $invite.role
            Accept-EvalInvitation -BaseUrl $BaseUrl -Token $raw -Password $password -DisplayName $invite.displayName
            Write-Host "  User $($invite.email) ($($invite.role))"
        } catch {
            if ($_.Exception.Message -match 'CONFLICT|duplicate|409') {
                Write-Host "  Skip invite $($invite.email) (may exist)"
            } else {
                throw
            }
        }
    }
}

$uploaded = @{}
foreach ($doc in $manifest.documents) {
    $filePath = Join-Path $corpusDir $doc.file
    if (-not (Test-Path $filePath)) { throw "Missing corpus: $filePath" }

    $roles = @()
    if ($doc.PSObject.Properties.Name -contains 'audienceRoles' -and $doc.audienceRoles) {
        $roles = @($doc.audienceRoles)
    }

    if ($doc.scope -eq 'platform') {
        $token = $platformToken
    } elseif ($doc.scope -eq 'tenant') {
        $token = $tenantAdminTokens[$doc.tenantCode]
        if (-not $token) { throw "Unknown tenantCode: $($doc.tenantCode)" }
    } else {
        throw "Unknown scope: $($doc.scope)"
    }

    Write-Host "Upload $($doc.file) ..."
    $view = Upload-EvalDocument -BaseUrl $BaseUrl -Token $token -FilePath $filePath `
        -Category $doc.category -AudienceRoles $roles
    $ready = Wait-EvalDocumentReady -BaseUrl $BaseUrl -Token $token -DocumentId $view.id -TimeoutSec $ReadyTimeoutSec
    Publish-EvalDocument -BaseUrl $BaseUrl -Token $token -DocumentId $view.id | Out-Null
    $uploaded[$doc.file] = @{
        id        = $view.id
        tenantId  = $ready.tenantId
        published = $true
        status    = 'READY'
    }
    Write-Host "  -> id=$($view.id) READY published"
}

$state = @{
    baseUrl    = $BaseUrl
    ingestedAt = (Get-Date).ToString('o')
    documents  = $uploaded
    tenants    = @($manifest.tenants | ForEach-Object { $_.code })
}
$state | ConvertTo-Json -Depth 6 | Set-Content -Encoding UTF8 $statePath
Write-Host "Done. State: $statePath"
