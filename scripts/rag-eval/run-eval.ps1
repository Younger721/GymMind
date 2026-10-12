# Run Search + Harness eval from queries.json
param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$QueriesPath,
    [switch]$SearchOnly,
    [switch]$SkipHarness,
    [int]$HarnessRetries = 2
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'EvalApi.ps1')

$root = Get-EvalRepoRoot
if (-not $QueriesPath) {
    $QueriesPath = Join-Path $root 'docs/rag-eval/queries.json'
}
$reportDir = Join-Path $root 'docs/rag-eval/reports'
New-Item -ItemType Directory -Force -Path $reportDir | Out-Null

$cfg = Get-Content -Raw -Encoding UTF8 $QueriesPath | ConvertFrom-Json
$k = [int]$cfg.k
if ($k -le 0) { $k = 10 }

function Test-SegmentHit {
    param($Segments, [string[]]$Anchors, [string[]]$DocNames)
    $textBlob = ($Segments | ForEach-Object { "$($_.text) $($_.documentName) $($_.knowledgeKey)" }) -join "`n"
    $anchorHits = @($Anchors | Where-Object { $_ -and ($textBlob -match [regex]::Escape($_)) })
    $docHits = @($DocNames | Where-Object {
            $_ -and ($Segments.documentName -contains $_)
        })
    return @{
        anchorHits = $anchorHits
        docHits    = $docHits
    }
}

function Test-ItemRelevant {
    param([string]$Text, [string[]]$Anchors, [string[]]$DocNames, [string]$DocumentName)
    if ($Anchors) {
        foreach ($anchor in $Anchors) {
            if ($anchor -and $Text -and ($Text -match [regex]::Escape($anchor))) {
                return $true
            }
        }
    }
    if ($DocNames -and $DocumentName -and ($DocNames -contains $DocumentName)) {
        return $true
    }
    return $false
}

function Count-RelevantTopItems {
    param($TopItems, [string[]]$Anchors, [string[]]$DocNames, [string]$Channel)
    $count = 0
    foreach ($item in $TopItems) {
        if ($Channel -eq 'search') {
            if (Test-ItemRelevant -Text "$($item.text) $($item.documentName)" -Anchors $Anchors -DocNames $DocNames -DocumentName $item.documentName) {
                $count++
            }
        } else {
            if (Test-ItemRelevant -Text "$($item.excerpt) $($item.documentName)" -Anchors $Anchors -DocNames $DocNames -DocumentName $item.documentName) {
                $count++
            }
        }
    }
    return $count
}

function Test-CitationHit {
    param($Citations, [string[]]$Anchors, [string[]]$DocNames)
    $textBlob = ($Citations | ForEach-Object { "$($_.excerpt) $($_.documentName) $($_.knowledgeKey)" }) -join "`n"
    $anchorHits = @($Anchors | Where-Object { $_ -and ($textBlob -match [regex]::Escape($_)) })
    $docHits = @($DocNames | Where-Object {
            $_ -and ($Citations.documentName -contains $_)
        })
    return @{
        anchorHits = $anchorHits
        docHits    = $docHits
    }
}

function Measure-Query {
    param($QueryRow, $Channel, $Segments, $Citations, $ErrorMsg)
    $expectedAnchors = @()
    if ($QueryRow.PSObject.Properties.Name -contains 'expectedAnchors' -and $QueryRow.expectedAnchors) {
        $expectedAnchors = @($QueryRow.expectedAnchors)
    }
    $expectedDocs = @()
    if ($QueryRow.PSObject.Properties.Name -contains 'expectedDocuments' -and $QueryRow.expectedDocuments) {
        $expectedDocs = @($QueryRow.expectedDocuments)
    }
    $forbiddenAnchors = @()
    if ($QueryRow.PSObject.Properties.Name -contains 'forbiddenAnchors' -and $QueryRow.forbiddenAnchors) {
        $forbiddenAnchors = @($QueryRow.forbiddenAnchors)
    }
    $forbiddenDocs = @()
    if ($QueryRow.PSObject.Properties.Name -contains 'forbiddenDocuments' -and $QueryRow.forbiddenDocuments) {
        $forbiddenDocs = @($QueryRow.forbiddenDocuments)
    }

    $top = @(
        if ($Channel -eq 'search') { @($Segments | Select-Object -First $k) }
        else { @($Citations | Select-Object -First $k) }
    )
    $topCount = @($top).Count

    $hit = if ($Channel -eq 'search') {
        Test-SegmentHit -Segments $top -Anchors $expectedAnchors -DocNames $expectedDocs
    } else {
        Test-CitationHit -Citations $top -Anchors $expectedAnchors -DocNames $expectedDocs
    }

    $recallDenom = [Math]::Max(1, $expectedAnchors.Count)
    $anchorRecallAtK = if ($expectedAnchors.Count -eq 0) { $null } else { $hit.anchorHits.Count / $recallDenom }

    $relevantInTop = if ($expectedAnchors.Count -eq 0) { 0 } else {
        Count-RelevantTopItems -TopItems $top -Anchors $expectedAnchors -DocNames $expectedDocs -Channel $Channel
    }
    $chunkPrecisionAtK = if ($expectedAnchors.Count -eq 0) { $null } else {
        if ($topCount -eq 0) { 0 } else { $relevantInTop / $topCount }
    }
    # 与经典 IR 一致：相关 chunk 召回率（标注为期望锚点/文档，Top-K 中命中条数 / 期望锚点数）
    $chunkRecallAtK = if ($expectedAnchors.Count -eq 0) { $null } else {
        [Math]::Min(1.0, $relevantInTop / $recallDenom)
    }

    $leakAnchors = @()
    $leakDocs = @()
    if ($forbiddenAnchors.Count -gt 0 -or $forbiddenDocs.Count -gt 0) {
        $pool = if ($Channel -eq 'search') { $Segments } else { $Citations }
        if ($Channel -eq 'search') {
            $blob = ($pool | ForEach-Object { "$($_.text) $($_.documentName)" }) -join "`n"
            $leakAnchors = @($forbiddenAnchors | Where-Object { $blob -match [regex]::Escape($_) })
            $leakDocs = @($forbiddenDocs | Where-Object { $pool.documentName -contains $_ })
        } else {
            $blob = ($pool | ForEach-Object { "$($_.excerpt) $($_.documentName)" }) -join "`n"
            $leakAnchors = @($forbiddenAnchors | Where-Object { $blob -match [regex]::Escape($_) })
            $leakDocs = @($forbiddenDocs | Where-Object { $pool.documentName -contains $_ })
        }
    }
    $permissionLeak = ($leakAnchors.Count + $leakDocs.Count) -gt 0

    $pass = $true
    if ($ErrorMsg) { $pass = $false }
    if ($permissionLeak) { $pass = $false }
    if ($expectedAnchors.Count -gt 0 -and $hit.anchorHits.Count -lt $expectedAnchors.Count) { $pass = $false }
    if ($expectedAnchors.Count -eq 0 -and $forbiddenAnchors.Count -gt 0 -and $permissionLeak) { $pass = $false }

    return [ordered]@{
        id              = $QueryRow.id
        user            = $QueryRow.user
        channel         = $Channel
        question        = $QueryRow.question
        pass            = $pass
        anchorRecallAtK = $anchorRecallAtK
        chunkRecallAtK  = $chunkRecallAtK
        chunkPrecisionAtK = $chunkPrecisionAtK
        recallAtK       = $anchorRecallAtK
        precisionAtK    = $chunkPrecisionAtK
        expectedAnchors = $expectedAnchors
        hitAnchors      = $hit.anchorHits
        permissionLeak  = $permissionLeak
        leakAnchors     = $leakAnchors
        leakDocuments   = $leakDocs
        topCount        = $topCount
        error           = $ErrorMsg
    }
}

$tokenCache = @{}
function Get-UserToken {
    param([string]$UserKey)
    if ($tokenCache.ContainsKey($UserKey)) { return $tokenCache[$UserKey] }
    $u = $cfg.users.$UserKey
    if (-not $u) { throw "queries.json missing user: $UserKey" }
    $token = Get-EvalAccessToken -BaseUrl $BaseUrl -Username $u.username -Password $u.password
    $tokenCache[$UserKey] = $token
    return $token
}

$rows = @()
$alignmentRows = @()
foreach ($q in $cfg.queries) {
    Write-Host "[$($q.id)] $($q.user) :: $($q.question)"
    $token = Get-UserToken -UserKey $q.user

    $segments = @()
    $searchErr = $null
    try {
        $segments = Invoke-EvalSearch -BaseUrl $BaseUrl -Token $token -Query $q.question
    } catch {
        $searchErr = $_.Exception.Message
    }
    $rows += Measure-Query -QueryRow $q -Channel 'search' -Segments $segments -ErrorMsg $searchErr
    $searchChunkIds = @($segments | ForEach-Object { $_.id })

    if (-not $SearchOnly -and -not $SkipHarness) {
        $chat = $null
        $hErr = $null
        for ($attempt = 0; $attempt -le $HarnessRetries; $attempt++) {
            try {
                $chat = Invoke-EvalHarnessChat -BaseUrl $BaseUrl -Token $token -Question $q.question `
                    -SessionId ("rag-eval-{0}" -f $q.id)
                $hErr = $null
                break
            } catch {
                $hErr = $_.Exception.Message
                if ($attempt -lt $HarnessRetries) { Start-Sleep -Seconds 5 }
            }
        }
        $citations = @()
        if ($chat -and $chat.citations) { $citations = @($chat.citations) }
        $rows += Measure-Query -QueryRow $q -Channel 'harness' -Citations $citations -ErrorMsg $hErr
        $harnessChunkIds = @($citations | ForEach-Object { $_.chunkId })
        $aligned = (-not $searchErr) -and (-not $hErr) -and (
            (($searchChunkIds -join '|') -eq ($harnessChunkIds -join '|'))
        )
        $alignmentRows += [ordered]@{
            id               = $q.id
            channelAligned   = $aligned
            searchChunkIds   = $searchChunkIds
            harnessChunkIds  = $harnessChunkIds
        }
    }
}

function Summarize-Channel {
    param([string]$Name)
    $subset = @($rows | Where-Object { $_.channel -eq $Name })
    $withExpected = @($subset | Where-Object { $_.expectedAnchors.Count -gt 0 })
    $recallValues = @($withExpected | Where-Object { $null -ne $_.anchorRecallAtK } | ForEach-Object { [double]$_.anchorRecallAtK })
    $avgAnchorRecall = if ($recallValues.Count -gt 0) {
        ($recallValues | Measure-Object -Average).Average
    } else { $null }
    $precValues = @($withExpected | Where-Object { $null -ne $_.chunkPrecisionAtK } | ForEach-Object { [double]$_.chunkPrecisionAtK })
    $avgChunkPrecision = if ($precValues.Count -gt 0) {
        ($precValues | Measure-Object -Average).Average
    } else { $null }
    $leaks = @($subset | Where-Object { $_.permissionLeak })
    $passed = @($subset | Where-Object { $_.pass })
    return [ordered]@{
        channel           = $Name
        queries           = $subset.Count
        passCount         = $passed.Count
        passRate          = if ($subset.Count -gt 0) { $passed.Count / $subset.Count } else { 0 }
        avgAnchorRecallAtK = $avgAnchorRecall
        avgChunkPrecisionAtK = $avgChunkPrecision
        avgRecallAtK      = $avgAnchorRecall
        permissionLeaks   = $leaks.Count
        failedIds         = @($subset | Where-Object { -not $_.pass } | ForEach-Object { $_.id })
    }
}

$misaligned = @($alignmentRows | Where-Object { -not $_.channelAligned })
$summary = [ordered]@{
    generatedAt = (Get-Date).ToString('o')
    baseUrl     = $BaseUrl
    k           = $k
    search      = Summarize-Channel 'search'
    harness     = if ($SearchOnly -or $SkipHarness) { $null } else { Summarize-Channel 'harness' }
    channelAlignment = if ($SearchOnly -or $SkipHarness) { $null } else {
        [ordered]@{
            queriesCompared = $alignmentRows.Count
            alignedCount    = @($alignmentRows | Where-Object { $_.channelAligned }).Count
            misalignedCount = $misaligned.Count
            misalignedIds   = @($misaligned | ForEach-Object { $_.id })
            details         = $alignmentRows
        }
    }
    details     = $rows
}

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$jsonPath = Join-Path $reportDir "eval-$stamp.json"
$mdPath = Join-Path $reportDir "eval-$stamp.md"
$summary | ConvertTo-Json -Depth 8 | Set-Content -Encoding UTF8 $jsonPath

$md = @"
# RAG eval report $stamp

- BaseUrl: $BaseUrl
- K: $k

## Search

| metric | value |
|--------|-------|
| pass | $($summary.search.passCount) / $($summary.search.queries) |
| avg AnchorRecall@K | $($summary.search.avgAnchorRecallAtK) |
| avg ChunkPrecision@K | $($summary.search.avgChunkPrecisionAtK) |
| PermissionLeak | $($summary.search.permissionLeaks) |

"@
if ($summary.harness) {
    $md += @"

## Harness (citations)

| metric | value |
|--------|-------|
| pass | $($summary.harness.passCount) / $($summary.harness.queries) |
| avg AnchorRecall@K | $($summary.harness.avgAnchorRecallAtK) |
| avg ChunkPrecision@K | $($summary.harness.avgChunkPrecisionAtK) |
| PermissionLeak | $($summary.harness.permissionLeaks) |

"@
    if ($summary.channelAlignment) {
        $md += @"

## Search vs Harness chunkId 对齐

| metric | value |
|--------|-------|
| aligned | $($summary.channelAlignment.alignedCount) / $($summary.channelAlignment.queriesCompared) |
| misaligned ids | $($summary.channelAlignment.misalignedIds -join ', ') |

"@
    }
}
$md += "`nJSON: $jsonPath"
Set-Content -Encoding UTF8 -Path $mdPath -Value $md

Write-Host ""
Write-Host "Report: $mdPath"
Write-Host "JSON: $jsonPath"
if ($summary.search.permissionLeaks -gt 0) {
    Write-Host "Search permission leak detected" -ForegroundColor Red
    exit 2
}
if ($summary.search.passCount -lt $summary.search.queries) {
    Write-Host "Some Search cases failed; see JSON details" -ForegroundColor Yellow
    exit 1
}
exit 0
