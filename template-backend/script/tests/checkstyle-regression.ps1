$ErrorActionPreference = 'Stop'
# Expected: valid main/test sources and comment/string boundaries pass;
# invalid main and test sources fail with the expected Checkstyle rule names.
# SQL: MyBatis-Plus/XML mapper signatures pass; MyBatis SQL annotations fail.

function Assert-Condition {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw "Assertion failed: $Message" }
}

function Write-Utf8 {
    param([string]$Path, [string]$Content)
    [System.IO.File]::WriteAllText($Path, $Content, (New-Object System.Text.UTF8Encoding($false)))
}

$backendRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$mavenCommand = (Get-Command mvn -ErrorAction Stop).Source
$sourcePom = New-Object System.Xml.XmlDocument
$sourcePom.Load((Join-Path $backendRoot 'pom.xml'))
$namespaces = New-Object System.Xml.XmlNamespaceManager($sourcePom.NameTable)
$namespaces.AddNamespace('m', 'http://maven.apache.org/POM/4.0.0')
$plugin = $sourcePom.SelectSingleNode('/m:project/m:build/m:plugins/m:plugin[m:artifactId="maven-checkstyle-plugin"]', $namespaces)
Assert-Condition ($null -ne $plugin) 'Checkstyle plugin must be in build/plugins.'
$pluginVersion = $sourcePom.project.properties.'maven-checkstyle-plugin.version'
$engineVersion = $sourcePom.project.properties.'checkstyle.version'
$pluginXml = $plugin.OuterXml.Replace(' xmlns="http://maven.apache.org/POM/4.0.0"', '')

# Each run keeps its own fixtures and logs; no existing files are removed.
$fixtureRoot = Join-Path $backendRoot ('target/checkstyle-regression/' + [guid]::NewGuid().ToString('N'))
$mainDirectory = Join-Path $fixtureRoot 'sample/src/main/java/org/example'
$testDirectory = Join-Path $fixtureRoot 'sample/src/test/java/org/example'
$configDirectory = Join-Path $fixtureRoot 'config/checkstyle'
New-Item -ItemType Directory -Path $mainDirectory, $testDirectory, $configDirectory -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $backendRoot 'config/checkstyle/checkstyle.xml') -Destination $configDirectory

$fixturePom = @"
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>org.example</groupId><artifactId>checkstyle-regression</artifactId><version>1</version>
    <packaging>pom</packaging>
    <properties>
        <maven-checkstyle-plugin.version>$pluginVersion</maven-checkstyle-plugin.version>
        <checkstyle.version>$engineVersion</checkstyle.version>
    </properties>
    <modules><module>sample</module></modules>
    <build><plugins>$pluginXml</plugins></build>
</project>
"@
Write-Utf8 (Join-Path $fixtureRoot 'pom.xml') $fixturePom
Write-Utf8 (Join-Path $fixtureRoot 'sample/pom.xml') @'
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent><groupId>org.example</groupId><artifactId>checkstyle-regression</artifactId><version>1</version></parent>
    <artifactId>sample</artifactId>
</project>
'@

function Test-Validation {
    param([string]$Name, [bool]$ShouldPass, [string[]]$ExpectedRules = @(), [int]$ExpectedSqlViolations = -1)
    $logPath = Join-Path $fixtureRoot ($Name + '.log')
    Push-Location $fixtureRoot
    try {
        & $mavenCommand -B -ntp validate *> $logPath
        $mavenExitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }
    $output = Get-Content -LiteralPath $logPath -Raw
    if ($ShouldPass) {
        Assert-Condition ($mavenExitCode -eq 0) "$Name must pass. See $logPath"
        Assert-Condition ($output.Contains('BUILD SUCCESS')) "$Name must complete the Maven lifecycle."
    } else {
        Assert-Condition ($mavenExitCode -ne 0) "$Name must reject violations."
        foreach ($rule in $ExpectedRules) {
            Assert-Condition ($output.Contains("[$rule]")) "$Name must report $rule. See $logPath"
        }
    }
    if ($ExpectedSqlViolations -ge 0) {
        $report = New-Object System.Xml.XmlDocument
        $report.Load((Join-Path $fixtureRoot 'sample/target/checkstyle-result.xml'))
        $sqlErrors = @($report.SelectNodes('//error') | Where-Object { $_.source -eq 'com.puppycrawl.tools.checkstyle.checks.coding.MatchXpathCheck' })
        Assert-Condition ($sqlErrors.Count -eq $ExpectedSqlViolations) "$Name must report exactly $ExpectedSqlViolations SQL violations, got $($sqlErrors.Count)."
    }
    Write-Output "PASS $Name (Maven exit $mavenExitCode)"
}

$validMain = @'
package org.example;

public class Sample {
    private final long count = 1L;
    // Example text is not a statement: ; ; import sun.misc.Unsafe;
    private final String example = "import sun.misc.Unsafe; ; 1l";
}
'@
$validTest = @'
package org.example;

public class SampleTest {
    private final long expected = 2L;
}
'@
Write-Utf8 (Join-Path $mainDirectory 'Sample.java') $validMain
Write-Utf8 (Join-Path $testDirectory 'SampleTest.java') $validTest
Test-Validation 'valid-sources-and-text-boundaries' $true

$invalidMain = @'
package org.Example;

import sun.misc.Unsafe;

public class bad_name {
    private long count = 1l;
    public void update() {
TABcount++; count++;
        ;
    }
}
'@
Write-Utf8 (Join-Path $mainDirectory 'Sample.java') ($invalidMain.Replace('TAB', "`t"))
Test-Validation 'reject-main-violations' $false @('PackageName', 'TypeName', 'OuterTypeFilename', 'IllegalImport', 'UpperEll', 'FileTabCharacter', 'OneStatementPerLine', 'EmptyStatement')

Write-Utf8 (Join-Path $mainDirectory 'Sample.java') $validMain
Write-Utf8 (Join-Path $testDirectory 'SampleTest.java') ($validTest.Replace('2L', '2l'))
Test-Validation 'reject-test-violations' $false @('UpperEll')

Write-Utf8 (Join-Path $testDirectory 'SampleTest.java') $validTest
Test-Validation 'restored-sources-pass' $true

# These are static-analysis fixtures, not compilation or database tests.
$allowedSqlSource = @'
package org.example;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.vo.SysUserVo;

@Mapper
public interface Sample extends BaseMapperPlus<SysUser, SysUserVo> {
    // Handwritten SQL for this signature belongs in SampleMapper.xml.
    java.util.List<SysUserVo> selectReport(@Param("status") String status);

    default long countByStatus(String status) {
        return selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getStatus, status));
    }

    // Text-only examples: @Select("SELECT 1"), @org.apache.ibatis.annotations.Delete(...)
    String EXAMPLE = "@UpdateProvider(type = Demo.class)";
}
'@
Write-Utf8 (Join-Path $mainDirectory 'Sample.java') $allowedSqlSource
Write-Utf8 (Join-Path $mainDirectory 'OtherAnnotations.java') @'
package org.example;

import org.apache.ibatis.annotations.*;
import org.example.ui.Select;

@Select
@org.example.ui.Delete
public class OtherAnnotations {
}
'@
Test-Validation 'allow-plus-xml-signatures-and-non-sql-annotations' $true @() 0

$sqlFixturePaths = @()
$sqlAnnotations = @('Select', 'Insert', 'Update', 'Delete', 'SelectProvider', 'InsertProvider', 'UpdateProvider', 'DeleteProvider', 'SelectKey')
foreach ($annotation in $sqlAnnotations) {
    foreach ($style in @('Explicit', 'Wildcard', 'Qualified')) {
        $typeName = "Sql${style}${annotation}Mapper"
        $importLine = switch ($style) {
            'Explicit' { "import org.apache.ibatis.annotations.$annotation;" }
            'Wildcard' { 'import org.apache.ibatis.annotations.*;' }
            'Qualified' { '' }
        }
        $annotationName = if ($style -eq 'Qualified') { "org.apache.ibatis.annotations.$annotation" } else { $annotation }
        $annotationArguments = if ($annotation.EndsWith('Provider')) {
            'type = Object.class, method = "sql"'
        } elseif ($annotation -eq 'SelectKey') {
            'statement = "SELECT 1", keyProperty = "id", before = true, resultType = Long.class'
        } else {
            '"SELECT 1"'
        }
        $fixturePath = Join-Path $mainDirectory ($typeName + '.java')
        Write-Utf8 $fixturePath @"
package org.example;

$importLine

public interface $typeName {
    @$annotationName(
        $annotationArguments
    )
    long query();
}
"@
        $sqlFixturePaths += $fixturePath
    }
}

# Repeatable containers must be rejected even when empty or imported directly.
foreach ($style in @('Explicit', 'Qualified', 'NestedImport')) {
    $typeName = "Sql${style}ContainerMapper"
    $importLine = switch ($style) {
        'Explicit' { 'import org.apache.ibatis.annotations.Select;' }
        'Qualified' { '' }
        'NestedImport' { 'import org.apache.ibatis.annotations.Select.List;' }
    }
    $annotationName = switch ($style) {
        'Explicit' { 'Select.List' }
        'Qualified' { 'org.apache.ibatis.annotations.Select.List' }
        'NestedImport' { 'List' }
    }
    $fixturePath = Join-Path $mainDirectory ($typeName + '.java')
    Write-Utf8 $fixturePath @"
package org.example;

$importLine

public interface $typeName {
    @$annotationName({})
    long query();
}
"@
    $sqlFixturePaths += $fixturePath
}
Test-Validation 'reject-sql-annotations-in-all-import-styles' $false @('MatchXpath') 30

# Preserve fixtures for inspection while removing them from the Java source scan.
foreach ($fixturePath in $sqlFixturePaths) {
    Move-Item -LiteralPath $fixturePath -Destination ($fixturePath + '.fixture')
}
Write-Utf8 (Join-Path $testDirectory 'SampleTest.java') @'
package org.example;

public interface SampleTest {
    @org.apache.ibatis.annotations.Select("SELECT 1")
    long query();
}
'@
Test-Validation 'reject-sql-annotations-in-test-sources' $false @('MatchXpath') 1

Write-Utf8 (Join-Path $testDirectory 'SampleTest.java') $validTest
Test-Validation 'restored-sql-sources-pass' $true @() 0
Write-Output 'PASS all 8 Checkstyle regression cases (30 SQL annotation variants)'
Write-Output "Logs: $fixtureRoot"
