# Test script to verify job recommendation quality
$sampleResume = Get-Content "data/samples/sample_resume.txt" -Raw

Write-Host "🚀 Starting Job Recommendation Quality Test" -ForegroundColor Green
Write-Host "=" * 50

# Test API recommendations
Write-Host "`n🔍 Testing API recommendations with sample resume..." -ForegroundColor Yellow

$body = @{
    resumeText = $sampleResume
    topK = 5
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/recommend" -Method POST -Body $body -ContentType "application/json"
    
    Write-Host "✅ API returned recommendations successfully" -ForegroundColor Green
    Write-Host "Total results: $($response.totalResults)"
    Write-Host "Query: $($response.query)"
    
    Write-Host "`n📊 RECOMMENDATIONS ANALYSIS:" -ForegroundColor Cyan
    Write-Host "=" * 50
    
    # Expected keywords from the resume
    $expectedKeywords = @("java", "spring", "spring boot", "microservices", "react", "postgresql", "docker", "kubernetes", "aws", "gcp", "software engineer", "developer", "backend", "full stack", "api", "rest", "database")
    $resumeLower = $sampleResume.ToLower()
    
    $goodMatches = 0
    
    for ($i = 0; $i -lt $response.recommendations.Count; $i++) {
        $rec = $response.recommendations[$i]
        $title = $rec.title
        $company = $rec.company
        $score = $rec.score
        $snippet = $rec.snippet
        
        # Check for keyword matches
        $jobText = "$title $snippet".ToLower()
        $keywordMatches = $expectedKeywords | Where-Object { $jobText.Contains($_) }
        
        # Determine if this is a good match
        $isGoodMatch = ($keywordMatches.Count -ge 2) -or 
                      ($jobText.Contains("java") -or $jobText.Contains("spring") -or $jobText.Contains("software engineer") -or $jobText.Contains("developer")) -or
                      ($score -gt 0.7)
        
        if ($isGoodMatch) { $goodMatches++ }
        
        Write-Host "`n$($i + 1). $title at $company" -ForegroundColor White
        Write-Host "   Score: $($score.ToString('F3'))" -ForegroundColor Gray
        Write-Host "   Matches: $($keywordMatches -join ', ')" -ForegroundColor Gray
        Write-Host "   Snippet: $($snippet.Substring(0, [Math]::Min(100, $snippet.Length)))..." -ForegroundColor Gray
        Write-Host "   $(if ($isGoodMatch) {'✅ Good Match'} else {'⚠️  Poor Match'})" -ForegroundColor $(if ($isGoodMatch) {'Green'} else {'Yellow'})
    }
    
    $avgScore = ($response.recommendations | Measure-Object -Property score -Average).Average
    $matchQuality = $goodMatches / $response.recommendations.Count
    
    Write-Host "`n📈 SUMMARY:" -ForegroundColor Cyan
    Write-Host "   Average Score: $($avgScore.ToString('F3'))" -ForegroundColor White
    Write-Host "   Good Matches: $goodMatches/$($response.recommendations.Count) ($($matchQuality.ToString('P1')))" -ForegroundColor White
    
    # Quality assessment
    if ($matchQuality -ge 0.8 -and $avgScore -gt 0.6) {
        Write-Host "✅ EXCELLENT: High quality recommendations" -ForegroundColor Green
        $success = $true
    } elseif ($matchQuality -ge 0.6 -and $avgScore -gt 0.5) {
        Write-Host "✅ GOOD: Decent recommendations with room for improvement" -ForegroundColor Yellow
        $success = $true
    } else {
        Write-Host "❌ POOR: Recommendations need improvement" -ForegroundColor Red
        $success = $false
    }
    
    # Also test NLP service directly
    Write-Host "`n🔍 Testing NLP service directly..." -ForegroundColor Yellow
    $nlpBody = @{
        query = $sampleResume
        top_k = 5
    } | ConvertTo-Json
    
    try {
        $nlpResponse = Invoke-RestMethod -Uri "http://localhost:8001/search" -Method POST -Body $nlpBody -ContentType "application/json"
        
        if ($nlpResponse.status -eq "success") {
            Write-Host "✅ NLP service returned $($nlpResponse.results.Count) results" -ForegroundColor Green
            
            # Compare results
            Write-Host "`n🔍 Comparing API vs NLP results:" -ForegroundColor Cyan
            Write-Host "API returned: $($response.recommendations.Count) recommendations"
            Write-Host "NLP returned: $($nlpResponse.results.Count) results"
            
        } else {
            Write-Host "❌ NLP service error: $($nlpResponse.message)" -ForegroundColor Red
        }
    } catch {
        Write-Host "❌ Error testing NLP service: $($_.Exception.Message)" -ForegroundColor Red
    }
    
} catch {
    Write-Host "❌ Error testing recommendations: $($_.Exception.Message)" -ForegroundColor Red
    $success = $false
}

Write-Host "`n🎯 FINAL RESULT: $(if ($success) {'✅ TESTS PASSED'} else {'❌ TESTS FAILED'})" -ForegroundColor $(if ($success) {'Green'} else {'Red'})

