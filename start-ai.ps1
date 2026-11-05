param([switch]$Configure)
$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot 'server')
try {
    if (-not (Get-Command node -ErrorAction SilentlyContinue)) { throw 'Instale Node.js 20 ou superior antes de continuar.' }
    if (-not (Test-Path -LiteralPath 'node_modules')) {
        & npm.cmd ci
        if ($LASTEXITCODE -ne 0) { throw 'Não foi possível instalar as dependências.' }
    }
    if ($Configure -or (-not (Test-Path -LiteralPath '.env') -and (-not $env:BORBACAR_TOKEN -or (-not $env:GEMINI_API_KEY -and -not $env:ANTHROPIC_API_KEY)))) {
        & npm.cmd run setup
        if ($LASTEXITCODE -ne 0) { throw 'Configuração não concluída.' }
    }
    & npm.cmd start
} finally { Pop-Location }
