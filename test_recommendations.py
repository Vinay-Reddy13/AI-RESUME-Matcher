#!/usr/bin/env python3
"""
Test script to verify job recommendation quality using the sample resume
"""

import requests
import json
import time
import sys
import os

# Configuration
API_BASE_URL = "http://localhost:8080"
NLP_BASE_URL = "http://localhost:8001"
SAMPLE_RESUME_PATH = "data/samples/sample_resume.txt"

def read_sample_resume():
    """Read the sample resume text"""
    try:
        with open(SAMPLE_RESUME_PATH, 'r', encoding='utf-8') as f:
            return f.read().strip()
    except FileNotFoundError:
        print(f"❌ Sample resume file not found at {SAMPLE_RESUME_PATH}")
        return None

def test_nlp_service_health():
    """Test if NLP service is running"""
    try:
        response = requests.get(f"{NLP_BASE_URL}/health", timeout=10)
        if response.status_code == 200:
            print("✅ NLP service is healthy")
            return True
        else:
            print(f"❌ NLP service health check failed: {response.status_code}")
            return False
    except requests.exceptions.RequestException as e:
        print(f"❌ Cannot connect to NLP service: {e}")
        return False

def test_api_service_health():
    """Test if API service is running"""
    try:
        response = requests.get(f"{API_BASE_URL}/actuator/health", timeout=10)
        if response.status_code == 200:
            print("✅ API service is healthy")
            return True
        else:
            print(f"❌ API service health check failed: {response.status_code}")
            return False
    except requests.exceptions.RequestException as e:
        print(f"❌ Cannot connect to API service: {e}")
        return False

def build_index():
    """Build the FAISS index"""
    try:
        print("🔄 Building FAISS index...")
        response = requests.post(f"{NLP_BASE_URL}/index/build", timeout=120)
        if response.status_code == 200:
            result = response.json()
            if result.get("status") == "success":
                print("✅ FAISS index built successfully")
                return True
            else:
                print(f"❌ Index build failed: {result.get('message')}")
                return False
        else:
            print(f"❌ Index build request failed: {response.status_code}")
            return False
    except requests.exceptions.RequestException as e:
        print(f"❌ Error building index: {e}")
        return False

def test_nlp_search(resume_text, top_k=5):
    """Test NLP service search directly"""
    try:
        payload = {
            "query": resume_text,
            "top_k": top_k
        }
        
        print(f"🔍 Testing NLP search for: {resume_text[:100]}...")
        response = requests.post(f"{NLP_BASE_URL}/search", json=payload, timeout=30)
        
        if response.status_code == 200:
            result = response.json()
            if result.get("status") == "success":
                results = result.get("results", [])
                print(f"✅ NLP search returned {len(results)} results")
                return results
            else:
                print(f"❌ NLP search failed: {result.get('message')}")
                return None
        else:
            print(f"❌ NLP search request failed: {response.status_code}")
            return None
    except requests.exceptions.RequestException as e:
        print(f"❌ Error during NLP search: {e}")
        return None

def test_api_recommendations(resume_text, top_k=5):
    """Test API recommendations endpoint"""
    try:
        payload = {
            "resumeText": resume_text,
            "topK": top_k
        }
        
        print(f"🔍 Testing API recommendations...")
        response = requests.post(f"{API_BASE_URL}/api/recommend", json=payload, timeout=30)
        
        if response.status_code == 200:
            result = response.json()
            recommendations = result.get("recommendations", [])
            print(f"✅ API returned {len(recommendations)} recommendations")
            return result
        else:
            print(f"❌ API recommendation request failed: {response.status_code}")
            print(f"Response: {response.text}")
            return None
    except requests.exceptions.RequestException as e:
        print(f"❌ Error during API recommendation: {e}")
        return None

def analyze_recommendations(recommendations, resume_text):
    """Analyze the quality of recommendations"""
    if not recommendations:
        print("❌ No recommendations to analyze")
        return False
    
    resume_lower = resume_text.lower()
    
    print("\n📊 RECOMMENDATION ANALYSIS:")
    print("=" * 50)
    
    # Expected keywords from the resume
    expected_keywords = [
        "java", "spring", "spring boot", "microservices", "react", "postgresql", 
        "docker", "kubernetes", "aws", "gcp", "software engineer", "developer",
        "backend", "full stack", "api", "rest", "database"
    ]
    
    good_matches = 0
    total_score = 0
    
    for i, rec in enumerate(recommendations, 1):
        title = rec.get("title", "")
        company = rec.get("company", "")
        score = rec.get("score", 0)
        snippet = rec.get("snippet", "")
        
        total_score += score
        
        # Check for keyword matches
        job_text = f"{title} {snippet}".lower()
        keyword_matches = [kw for kw in expected_keywords if kw in job_text]
        
        # Determine if this is a good match
        is_good_match = (
            len(keyword_matches) >= 2 or  # At least 2 keyword matches
            any(tech in job_text for tech in ["java", "spring", "software engineer", "developer"]) or
            score > 0.7  # High semantic similarity
        )
        
        if is_good_match:
            good_matches += 1
        
        print(f"\n{i}. {title} at {company}")
        print(f"   Score: {score:.3f}")
        print(f"   Matches: {', '.join(keyword_matches[:5])}")
        print(f"   Snippet: {snippet[:100]}...")
        print(f"   {'✅ Good Match' if is_good_match else '⚠️  Poor Match'}")
    
    avg_score = total_score / len(recommendations) if recommendations else 0
    match_quality = good_matches / len(recommendations) if recommendations else 0
    
    print(f"\n📈 SUMMARY:")
    print(f"   Average Score: {avg_score:.3f}")
    print(f"   Good Matches: {good_matches}/{len(recommendations)} ({match_quality:.1%})")
    
    # Quality assessment
    if match_quality >= 0.8 and avg_score > 0.6:
        print("✅ EXCELLENT: High quality recommendations")
        return True
    elif match_quality >= 0.6 and avg_score > 0.5:
        print("✅ GOOD: Decent recommendations with room for improvement")
        return True
    else:
        print("❌ POOR: Recommendations need improvement")
        return False

def main():
    """Main test function"""
    print("🚀 Starting Job Recommendation Quality Test")
    print("=" * 50)
    
    # Read sample resume
    resume_text = read_sample_resume()
    if not resume_text:
        return False
    
    print(f"📄 Loaded resume with {len(resume_text)} characters")
    
    # Test service health
    if not test_nlp_service_health():
        print("❌ NLP service not available. Please start the services first.")
        return False
    
    if not test_api_service_health():
        print("❌ API service not available. Please start the services first.")
        return False
    
    # Build index
    if not build_index():
        print("❌ Failed to build index. Cannot proceed with tests.")
        return False
    
    # Wait a bit for index to be ready
    time.sleep(2)
    
    # Test NLP service directly
    print("\n" + "="*50)
    print("🔍 TESTING NLP SERVICE DIRECTLY")
    print("="*50)
    
    nlp_results = test_nlp_search(resume_text, top_k=5)
    if nlp_results:
        nlp_quality = analyze_recommendations(nlp_results, resume_text)
    else:
        nlp_quality = False
    
    # Test API service
    print("\n" + "="*50)
    print("🔍 TESTING API SERVICE")
    print("="*50)
    
    api_result = test_api_recommendations(resume_text, top_k=5)
    if api_result:
        api_recommendations = api_result.get("recommendations", [])
        api_quality = analyze_recommendations(api_recommendations, resume_text)
    else:
        api_quality = False
    
    # Final assessment
    print("\n" + "="*50)
    print("🎯 FINAL ASSESSMENT")
    print("="*50)
    
    if nlp_quality and api_quality:
        print("✅ ALL TESTS PASSED: Job recommendations are working well!")
        return True
    elif nlp_quality or api_quality:
        print("⚠️  PARTIAL SUCCESS: One service is working, but there are issues")
        return False
    else:
        print("❌ ALL TESTS FAILED: Job recommendations need improvement")
        return False

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)

