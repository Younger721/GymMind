# CI：校验 rag-eval 固定语料与 queries 结构（无需启动后端）
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'EvalApi.ps1')

$root = Get-EvalRepoRoot
$queriesPath = Join-Path $root 'docs/rag-eval/queries.json'
$manifestPath = Join-Path $root 'docs/rag-eval/manifest.json'
$corpusDir = Join-Path $root 'docs/rag-eval/corpus'

if (-not (Test-Path $queriesPath)) { throw "missing $queriesPath" }
if (-not (Test-Path $manifestPath)) { throw "missing $manifestPath" }
if (-not (Test-Path $corpusDir)) { throw "missing $corpusDir" }

$queries = Get-Content -Raw -Encoding UTF8 $queriesPath | ConvertFrom-Json
$manifest = Get-Content -Raw -Encoding UTF8 $manifestPath | ConvertFrom-Json

if (-not $queries.queries -or $queries.queries.Count -lt 1) {
    throw 'queries.json must contain at least one query'
}
if (-not $manifest.tenants -or $manifest.tenants.Count -lt 1) {
    throw 'manifest.json must define tenants'
}
foreach ($doc in $manifest.documents) {
    $name = if ($doc.fileName) { $doc.fileName } else { $doc.file }
    $path = Join-Path $corpusDir $name
    if (-not (Test-Path $path)) {
        throw "corpus file missing: $name"
    }
}

Write-Host "rag-eval fixtures OK ($($queries.queries.Count) queries, $($manifest.documents.Count) corpus files)"
