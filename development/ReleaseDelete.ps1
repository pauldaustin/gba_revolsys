param(
  [Parameter(Mandatory = $true, HelpMessage = "The version to create the release for")] 
  [string]$Version,
  [Parameter(Mandatory = $false, HelpMessage = "Delete the release without prompting")] 
  [switch]$Force 

)
$ErrorActionPreference = "Stop"
$scmConfig = Get-Content "$PSScriptRoot/Config.json" | ConvertFrom-Json

Push-Location -Path "$PSScriptRoot/../.."
$deleteArgs = @()
if ($Force.IsPresent) {
  $deleteArgs += '--yes'
}

try {
  foreach ($repository in $scmConfig.repositories) {
    Write-Output $repository.path
    Push-Location -Path $repository.path 
    try {
      gh release delete $Version --cleanup-tag @deleteArgs
    }
    finally {
      Pop-Location
    }
  }
}
finally {
  Pop-Location
}
