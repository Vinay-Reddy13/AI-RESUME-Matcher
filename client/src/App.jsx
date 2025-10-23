import React, { useState } from 'react'
import axios from 'axios'
import './index.css'
import * as pdfjsLib from 'pdfjs-dist'

// Configure PDF.js worker
pdfjsLib.GlobalWorkerOptions.workerSrc = `//cdnjs.cloudflare.com/ajax/libs/pdf.js/${pdfjsLib.version}/pdf.worker.min.js`

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

function App() {
  const [resumeText, setResumeText] = useState('')
  const [topK, setTopK] = useState(5)
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [uploadedFileName, setUploadedFileName] = useState('')
  const [expandedJobs, setExpandedJobs] = useState(new Set())

  const handleSubmit = async (e) => {
    e.preventDefault()
    
    if (!resumeText.trim()) {
      setError('Please enter your resume text')
      return
    }

    setLoading(true)
    setError('')
    setSuccess('')
    setResults([])

    try {
      const response = await axios.post(`${API_BASE_URL}/api/recommend`, {
        resumeText: resumeText.trim(),
        topK: parseInt(topK)
      })

      if (response.data && response.data.recommendations) {
        setResults(response.data.recommendations)
        setSuccess(`Found ${response.data.recommendations.length} matching jobs`)
      } else {
        setError('No results returned from the server')
      }
    } catch (err) {
      console.error('Error fetching recommendations:', err)
      if (err.response) {
        setError(`Server error: ${err.response.data?.message || err.response.statusText}`)
      } else if (err.request) {
        setError('Unable to connect to the server. Please check if the API is running.')
      } else {
        setError('An unexpected error occurred')
      }
    } finally {
      setLoading(false)
    }
  }

  const loadSampleResume = () => {
    const sampleText = `abc
Software Engineer
Email: abc@gmail.com | Phone: (123) 123-1234

PROFESSIONAL SUMMARY
Experienced software engineer with 4+ years of experience in full-stack development, specializing in Java, Spring Boot, and microservices architecture. Proven track record of building scalable web applications and leading development teams.

TECHNICAL SKILLS
• Programming Languages: Java, Python, JavaScript, TypeScript, SQL
• Frameworks: Spring Boot, Spring Security, React, Node.js, Express.js
• Databases: PostgreSQL, MySQL, MongoDB, Redis
• Cloud Platforms: AWS (EC2, S3, RDS, Lambda), Google Cloud Platform
• Tools: Docker, Kubernetes, Git, Jenkins, Maven, Gradle

PROFESSIONAL EXPERIENCE

Senior Software Engineer | TechCorp Inc. | 2022 - Present
• Led development of microservices architecture serving 1M+ daily active users
• Implemented RESTful APIs using Spring Boot and Spring Security
• Optimized database queries resulting in 40% performance improvement
• Mentored 3 junior developers and conducted code reviews

Software Engineer | StartupXYZ | 2020 - 2022
• Developed full-stack web applications using Java, Spring Boot, and React
• Built and maintained PostgreSQL databases with complex queries
• Implemented CI/CD pipelines using Jenkins and Docker

EDUCATION
Bachelor of Science in Computer Science
University of Technology | 2015 - 2019

CERTIFICATIONS
• AWS Certified Solutions Architect - Associate (2023)
• Oracle Certified Professional Java SE 11 Developer (2022)`

    setResumeText(sampleText)
    setUploadedFileName('')
  }

  const handleFileUpload = async (e) => {
    const file = e.target.files[0]
    if (!file) return

    setUploadedFileName(file.name)
    setError('')
    setLoading(true)

    try {
      // Check file type
      if (!file.type.includes('text') && !file.name.endsWith('.txt') && !file.name.endsWith('.pdf')) {
        setError('Please upload a .txt or .pdf file')
        return
      }

      let extractedText = ''

      if (file.type === 'application/pdf' || file.name.endsWith('.pdf')) {
        // Handle PDF files with proper text extraction
        try {
          const arrayBuffer = await file.arrayBuffer()
          const pdf = await pdfjsLib.getDocument(arrayBuffer).promise
          
          let fullText = ''
          
          // Extract text from all pages
          for (let i = 1; i <= pdf.numPages; i++) {
            const page = await pdf.getPage(i)
            const textContent = await page.getTextContent()
            const pageText = textContent.items.map(item => item.str).join(' ')
            fullText += pageText + '\n'
          }
          
          extractedText = fullText.trim()
          setSuccess(`PDF "${file.name}" processed successfully! Extracted text from ${pdf.numPages} page(s).`)
        } catch (pdfError) {
          console.error('PDF parsing error:', pdfError)
          setError('Failed to extract text from PDF. Please try copying and pasting the text manually.')
          return
        }
      } else {
        // Handle text files
        const reader = new FileReader()
        reader.onload = (event) => {
          extractedText = event.target.result
          setResumeText(extractedText)
          setSuccess(`File "${file.name}" loaded successfully!`)
          setLoading(false)
        }
        
        reader.onerror = () => {
          setError('Error reading text file. Please try again.')
          setLoading(false)
        }

        reader.readAsText(file)
        return
      }

      setResumeText(extractedText)
    } catch (err) {
      console.error('File processing error:', err)
      setError('Error processing file. Please try again or paste text manually.')
    } finally {
      setLoading(false)
    }
  }

  const toggleJobExpansion = (jobId) => {
    const newExpanded = new Set(expandedJobs)
    if (newExpanded.has(jobId)) {
      newExpanded.delete(jobId)
    } else {
      newExpanded.add(jobId)
    }
    setExpandedJobs(newExpanded)
  }

  return (
    <div className="App">
      <header className="header">
        <div className="container">
          <h1>AI Resume Matcher</h1>
          <p>Find your perfect job match using AI-powered semantic search</p>
        </div>
      </header>

      <main className="main">
        <div className="container">
          <div className="card">
            <h2>Upload or Paste Your Resume</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label htmlFor="fileUpload">
                  Upload Resume File
                </label>
                <input
                  type="file"
                  id="fileUpload"
                  accept=".txt,.pdf"
                  onChange={handleFileUpload}
                  disabled={loading}
                  style={{ marginBottom: '1rem', padding: '0.5rem', border: '1px solid #ddd', borderRadius: '4px', width: '100%', opacity: loading ? 0.6 : 1 }}
                />
                {loading && uploadedFileName && (
                  <p style={{ color: '#007bff', fontSize: '0.9rem', marginBottom: '1rem' }}>
                    ⏳ Processing {uploadedFileName}...
                  </p>
                )}
                {!loading && uploadedFileName && (
                  <p style={{ color: '#28a745', fontSize: '0.9rem', marginBottom: '1rem' }}>
                    📁 {uploadedFileName} processed successfully!
                  </p>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="resumeText">
                  Or Paste Resume Text Here
                  <button 
                    type="button" 
                    className="btn btn-secondary" 
                    onClick={loadSampleResume}
                    style={{ marginLeft: '1rem', padding: '0.5rem 1rem', fontSize: '0.875rem' }}
                  >
                    Load Sample
                  </button>
                </label>
                <textarea
                  id="resumeText"
                  value={resumeText}
                  onChange={(e) => {
                    setResumeText(e.target.value)
                    if (e.target.value) setUploadedFileName('')
                  }}
                  placeholder="Paste your resume text here... Include your skills, experience, education, and any other relevant information."
                />
              </div>

              <div className="form-group">
                <label htmlFor="topK">Number of Results</label>
                <input
                  id="topK"
                  type="number"
                  min="1"
                  max="20"
                  value={topK}
                  onChange={(e) => setTopK(e.target.value)}
                  placeholder="Number of job matches to return"
                />
              </div>

              <button 
                type="submit" 
                className="btn" 
                disabled={loading}
              >
                {loading ? (
                  <>
                    <div className="spinner"></div>
                    Finding Matches...
                  </>
                ) : (
                  'Find Matches'
                )}
              </button>
            </form>

            {error && (
              <div className="error">
                {error}
              </div>
            )}

            {success && (
              <div className="success">
                {success}
              </div>
            )}
          </div>

          {results.length > 0 && (
            <div className="card">
              <h3>Job Recommendations</h3>
              <div className="results">
                {results.map((job, index) => (
                  <div key={job.id || index} className="result-item">
                    <div className="result-header">
                      <div>
                        <div className="result-title">{job.title}</div>
                        <div className="result-company">{job.company}</div>
                        <div className="result-location">{job.location}</div>
                      </div>
                      <div className="result-score">
                        {Math.round(job.score * 100)}% Match
                      </div>
                    </div>
                    
                    <div className="result-snippet">
                      {expandedJobs.has(job.id) ? job.fullDescription || job.snippet : job.snippet}
                    </div>
                    
                    <div className="result-actions">
                      {job.fullDescription && job.fullDescription !== job.snippet && (
                        <button 
                          type="button"
                          className="btn btn-secondary"
                          onClick={() => toggleJobExpansion(job.id)}
                          style={{ marginRight: '1rem', padding: '0.5rem 1rem', fontSize: '0.875rem' }}
                        >
                          {expandedJobs.has(job.id) ? 'Show Less' : 'Show Full Description'}
                        </button>
                      )}
                      
                      <div className="apply-buttons" style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                        {job.applicationUrl && (
                          <a 
                            href={job.applicationUrl} 
                            target="_blank" 
                            rel="noopener noreferrer"
                            className="btn"
                            style={{ padding: '0.75rem 1.5rem', textDecoration: 'none' }}
                          >
                            Apply at {job.company}
                          </a>
                        )}
                        
                        {/* Always show LinkedIn and Indeed as alternatives */}
                        <a 
                          href={`https://www.linkedin.com/jobs/search/?keywords=${encodeURIComponent(job.title)} ${encodeURIComponent(job.company)}`}
                          target="_blank" 
                          rel="noopener noreferrer"
                          className="btn btn-secondary"
                          style={{ padding: '0.75rem 1.5rem', textDecoration: 'none' }}
                        >
                          LinkedIn
                        </a>
                        
                        <a 
                          href={`https://www.indeed.com/jobs?q=${encodeURIComponent(job.title)} ${encodeURIComponent(job.company)}`}
                          target="_blank" 
                          rel="noopener noreferrer"
                          className="btn btn-secondary"
                          style={{ padding: '0.75rem 1.5rem', textDecoration: 'none' }}
                        >
                          Indeed
                        </a>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {!loading && results.length === 0 && resumeText && (
            <div className="card">
              <div className="info">
                <strong>Ready to find matches!</strong> Click "Find Matches" to get AI-powered job recommendations based on your resume.
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  )
}

export default App

