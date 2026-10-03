param(
    [Parameter(Mandatory = $true)][string]$InputDocx,
    [Parameter(Mandatory = $true)][string]$OutputDirectory,
    [Parameter(Mandatory = $true)][string]$PopplerPath
)

$ErrorActionPreference = 'Stop'
$inputPath = (Resolve-Path -LiteralPath $InputDocx).Path
$poppler = (Resolve-Path -LiteralPath $PopplerPath).Path
if ([System.IO.Path]::GetExtension($inputPath) -ne '.docx') { throw 'Only DOCX input is supported.' }
$renderDirectory = Join-Path ([System.IO.Path]::GetFullPath($OutputDirectory)) ([guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $renderDirectory -Force | Out-Null
$pdfPath = Join-Path $renderDirectory 'preview.pdf'
$word = $null
$document = $null
$canQuit = $false
try {
    $word = New-Object -ComObject Word.Application
    $canQuit = $word.Documents.Count -eq 0
    if ($canQuit) { $word.Visible = $false }
    $document = $word.Documents.Open($inputPath, $false, $true, $false)
    $document.ExportAsFixedFormat($pdfPath, 17)
} finally {
    if ($null -ne $document) {
        $document.Close(0)
        [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($document)
    }
    if ($null -ne $word) {
        if ($canQuit) { $word.Quit() }
        [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($word)
    }
}
if (-not (Test-Path -LiteralPath $pdfPath)) { throw 'Word did not produce a PDF.' }
& $poppler -r 120 -png $pdfPath (Join-Path $renderDirectory 'page')
if ($LASTEXITCODE -ne 0) { throw 'PDF rasterization failed.' }
$pageCount = @(Get-ChildItem -LiteralPath $renderDirectory -Filter 'page-*.png').Count
if ($pageCount -eq 0) { throw 'No page images were generated.' }
Write-Output "DOCX_RENDER_PAGES: $pageCount"
Write-Output "DOCX_RENDER_DIRECTORY: $renderDirectory"
