# RAG eval HTTP helpers (Windows PowerShell 5.1+ / PowerShell 7+)
param()

function Get-EvalRepoRoot {
    $scriptDir = Split-Path -Parent $PSScriptRoot
    return Split-Path -Parent $scriptDir
}

function Invoke-EvalApi {
    param(
        [string]$Method = 'GET',
        [string]$Url,
        [string]$Token,
        [object]$Body
    )
    $headers = @{ Accept = 'application/json' }
    if ($Token) { $headers['Authorization'] = "Bearer $Token" }

    try {
        if ($Body -ne $null) {
            return Invoke-RestMethod -Method $Method -Uri $Url -Headers $headers `
                -ContentType 'application/json; charset=utf-8' -Body ($Body | ConvertTo-Json -Depth 8)
        }
        return Invoke-RestMethod -Method $Method -Uri $Url -Headers $headers
    } catch {
        $resp = $_.Exception.Response
        if ($resp -and $resp.StatusCode.value__) {
            $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
            $detail = $reader.ReadToEnd()
            throw "HTTP $($resp.StatusCode.value__) $Url : $detail"
        }
        throw
    }
}

function Get-EvalAccessToken {
    param(
        [string]$BaseUrl,
        [string]$Username,
        [string]$Password
    )
    $login = Invoke-EvalApi -Method POST -Url "$BaseUrl/api/v1/auth/login" -Body @{
        username = $Username
        password = $Password
    }
    if (-not $login.success) { throw "Login failed: $($login.message)" }
    return $login.data.accessToken
}

function Register-EvalTenant {
    param(
        [string]$BaseUrl,
        [string]$TenantCode,
        [string]$TenantName,
        [string]$Email,
        [string]$Password,
        [string]$DisplayName
    )
    try {
        $result = Invoke-EvalApi -Method POST -Url "$BaseUrl/api/v1/auth/register" -Body @{
            tenantCode  = $TenantCode
            tenantName  = $TenantName
            email       = $Email
            password    = $Password
            displayName = $DisplayName
        }
        if (-not $result.success) { throw $result.message }
        return $result.data.accessToken
    } catch {
        if ($_.Exception.Message -match '409|CONFLICT|duplicate') {
            return $null
        }
        throw
    }
}

function Invite-EvalUser {
    param(
        [string]$BaseUrl,
        [string]$AdminToken,
        [string]$Email,
        [string]$Role
    )
    $result = Invoke-EvalApi -Method POST -Url "$BaseUrl/api/v1/users/invitations" -Token $AdminToken -Body @{
        email = $Email
        role  = $Role
    }
    if (-not $result.success) { throw "Invite failed: $($result.message)" }
    return $result.data.rawToken
}

function Accept-EvalInvitation {
    param(
        [string]$BaseUrl,
        [string]$Token,
        [string]$Password,
        [string]$DisplayName
    )
    $result = Invoke-EvalApi -Method POST -Url "$BaseUrl/api/v1/auth/invitations/accept" -Body @{
        token       = $Token
        password    = $Password
        displayName = $DisplayName
    }
    if (-not $result.success) { throw "Accept invitation failed: $($result.message)" }
}

function Upload-EvalDocument {
    param(
        [string]$BaseUrl,
        [string]$Token,
        [string]$FilePath,
        [string]$Category,
        [string[]]$AudienceRoles = @()
    )
    Add-Type -AssemblyName System.Net.Http
    $fileItem = Get-Item -LiteralPath $FilePath
    $client = New-Object System.Net.Http.HttpClient
    $client.DefaultRequestHeaders.Authorization = `
        New-Object System.Net.Http.Headers.AuthenticationHeaderValue('Bearer', $Token)
    $multipart = New-Object System.Net.Http.MultipartFormDataContent
    $stream = [System.IO.File]::OpenRead($FilePath)
    try {
        $filePart = New-Object System.Net.Http.StreamContent($stream)
        $filePart.Headers.ContentType = `
            [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse('application/octet-stream')
        $multipart.Add($filePart, 'file', $fileItem.Name) | Out-Null
        $multipart.Add([System.Net.Http.StringContent]::new('TENANT'), 'visibility') | Out-Null
        $multipart.Add([System.Net.Http.StringContent]::new($Category), 'category') | Out-Null
        foreach ($role in $AudienceRoles) {
            $multipart.Add([System.Net.Http.StringContent]::new($role), 'audienceRoles') | Out-Null
        }
        $response = $client.PostAsync("$BaseUrl/api/v1/knowledge/documents", $multipart).GetAwaiter().GetResult()
        $payload = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) {
            throw "Upload failed HTTP $([int]$response.StatusCode): $payload"
        }
        $result = $payload | ConvertFrom-Json
        if (-not $result.success) { throw "Upload failed: $($result.message)" }
        return $result.data
    } finally {
        $stream.Dispose()
        $client.Dispose()
    }
}

function Wait-EvalDocumentReady {
    param(
        [string]$BaseUrl,
        [string]$Token,
        [long]$DocumentId,
        [int]$TimeoutSec = 300
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $doc = Invoke-EvalApi -Method GET -Url "$BaseUrl/api/v1/knowledge/documents/$DocumentId" -Token $Token
        if (-not $doc.success) { throw $doc.message }
        $status = $doc.data.status
        if ($status -eq 'READY') { return $doc.data }
        if ($status -eq 'FAILED') { throw "Document $DocumentId ingest FAILED" }
        Start-Sleep -Seconds 3
    }
    throw "Timeout waiting for document $DocumentId READY (${TimeoutSec}s)"
}

function Publish-EvalDocument {
    param(
        [string]$BaseUrl,
        [string]$Token,
        [long]$DocumentId
    )
    $result = Invoke-EvalApi -Method PATCH `
        -Url "$BaseUrl/api/v1/knowledge/documents/$DocumentId/publication?published=true" -Token $Token
    if (-not $result.success) { throw "Publish failed: $($result.message)" }
    return $result.data
}

function Invoke-EvalSearch {
    param(
        [string]$BaseUrl,
        [string]$Token,
        [string]$Query
    )
    $encoded = [uri]::EscapeDataString($Query)
    $result = Invoke-EvalApi -Method GET -Url "$BaseUrl/api/v1/knowledge/search?q=$encoded" -Token $Token
    if (-not $result.success) { throw $result.message }
    return @($result.data)
}

function Invoke-EvalHarnessChat {
    param(
        [string]$BaseUrl,
        [string]$Token,
        [string]$Question,
        [string]$SessionId = 'rag-eval-session'
    )
    $result = Invoke-EvalApi -Method POST -Url "$BaseUrl/api/v1/ai/harness/chat" -Token $Token -Body @{
        sessionId = $SessionId
        question  = $Question
        plugins   = @('knowledge_search')
        history   = @()
    }
    if (-not $result.success) { throw $result.message }
    return $result.data
}
