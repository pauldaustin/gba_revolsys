param(
  [Parameter(Mandatory = $true, HelpMessage = "The version to create the release for")] 
  [string]$Version,
  [Parameter(Mandatory = $false, HelpMessage = "The branch name (e.g. main)")] 
  [string]$Target = 'main',
  [Parameter(Mandatory = $false, HelpMessage = "The branch to release")] 
  [string]$Tiltle,
  [Parameter(Mandatory = $false, HelpMessage = "The notes for the release ")] 
  [string]$Notes 

)
$ErrorActionPreference = "Stop"
$scmConfig = Get-Content "$PSScriptRoot/Config.json" | ConvertFrom-Json

Push-Location -Path "$PSScriptRoot/../.."
try {
  foreach ($repository in $scmConfig.repositories) {
    Write-Output $repository.path
    Push-Location -Path $repository.path 
    try {
      gh release create $Version --target $Target --title ($Title ?? $Version) --notes ($Notes ?? "Version: ${Version}}") 
    }
    finally {
      Pop-Location
    }
  }
}
finally {
  Pop-Location
}
