"""
Vector Store implementation using FAISS and sentence transformers
Handles embedding generation, indexing, and similarity search
"""

import os
import pandas as pd
import numpy as np
import faiss
import pickle
import json
import requests
import re
from sentence_transformers import SentenceTransformer
from typing import List, Dict, Any, Optional, Set
import logging

logger = logging.getLogger(__name__)

class VectorStore:
    def __init__(self, model_name='all-MiniLM-L6-v2'):
        """
        Initialize the vector store with sentence transformer model
        
        Args:
            model_name: Name of the sentence transformer model to use
        """
        self.model_name = model_name
        self.model = None
        self.index = None
        self.job_metadata = None
        self.data_dir = '/app/data'
        self.index_path = os.path.join(self.data_dir, 'jobs.faiss')
        self.metadata_path = os.path.join(self.data_dir, 'jobs_meta.parquet')
        self.model_meta_path = os.path.join(self.data_dir, 'model_meta.json')
        
        # Role detection keywords
        self.FULLSTACK = {"full stack", "full-stack", "react", "angular", "spring boot", "node", "express", "typescript", "java", "javascript"}
        self.DEVOPS = {"devops", "sre", "site reliability", "terraform", "ansible", "jenkins", "kubernetes", "eks", "helm", "argo", "ci/cd", "docker", "infrastructure"}
        
    def _load_model(self):
        """Load the sentence transformer model"""
        if self.model is None:
            logger.info(f"Loading sentence transformer model: {self.model_name}")
            self.model = SentenceTransformer(self.model_name)
            logger.info("Model loaded successfully")
    
    def _normalize_embeddings(self, embeddings: np.ndarray) -> np.ndarray:
        """Normalize embeddings to unit vectors for cosine similarity"""
        norms = np.linalg.norm(embeddings, axis=1, keepdims=True)
        return embeddings / norms
    
    def _save_model_fingerprint(self, source_csv: str):
        """Save model fingerprint to detect mismatches"""
        meta = {
            "model_name": self.model_name,
            "embedding_dim": int(self.index.d) if self.index is not None else None,
            "source_csv": os.path.basename(source_csv)
        }
        with open(self.model_meta_path, "w", encoding="utf-8") as f:
            json.dump(meta, f)
        logger.info(f"Saved model fingerprint: {meta}")
    
    def _load_model_fingerprint(self) -> Optional[Dict]:
        """Load model fingerprint to check for mismatches"""
        if not os.path.exists(self.model_meta_path):
            return None
        try:
            with open(self.model_meta_path, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            logger.warning(f"Failed to load model fingerprint: {e}")
            return None
    
    def detect_role(self, text: str) -> str:
        """Detect role from resume text with improved logic"""
        t = text.lower()
        
        # Enhanced role detection with more specific keywords
        fullstack_keywords = {
            "full stack", "full-stack", "react", "angular", "vue", "spring boot", 
            "node", "express", "typescript", "javascript", "java", "frontend", 
            "backend", "web developer", "software engineer", "developer"
        }
        devops_keywords = {
            "devops", "sre", "site reliability", "terraform", "ansible", "jenkins", 
            "kubernetes", "eks", "helm", "argo", "ci/cd", "docker", "infrastructure",
            "platform engineer", "cloud engineer", "aws", "azure", "gcp"
        }
        
        fs_hits = sum(1 for k in fullstack_keywords if k in t)
        dv_hits = sum(1 for k in devops_keywords if k in t)
        
        # More lenient role detection to get more results
        if fs_hits == 0 and dv_hits == 0:
            return "general"
        elif fs_hits > dv_hits * 2.0:  # Very strong fullstack preference only
            return "fullstack"
        elif dv_hits > fs_hits * 2.0:  # Very strong devops preference only
            return "devops"
        else:
            return "general"  # Default to general for more results
    
    def extract_skills(self, resume_text: str) -> Set[str]:
        """Extract normalized skills from resume text"""
        skills = set()
        text_lower = resume_text.lower()
        
        # Common tech skills
        tech_skills = [
            'java', 'python', 'javascript', 'typescript', 'react', 'angular', 'vue', 
            'spring boot', 'node.js', 'express', 'postgresql', 'mongodb', 'redis',
            'aws', 'azure', 'gcp', 'docker', 'kubernetes', 'jenkins', 'git',
            'hibernate', 'junit', 'jdbc', 'html', 'css', 'bootstrap'
        ]
        
        for skill in tech_skills:
            if skill in text_lower:
                skills.add(skill)
        
        return skills
    
    def is_live_url(self, url: str, timeout: int = 4) -> bool:
        """Check if URL is live and not expired"""
        try:
            r = requests.head(url, allow_redirects=True, timeout=timeout)
            if r.status_code != 200:
                return False
            
            # Check for expired/removed job indicators
            bad_words = ("expired", "not-found", "removed", "archived", "404", "page-not-found")
            if any(word in r.url.lower() for word in bad_words):
                return False
            
            # Check response content for error indicators
            if hasattr(r, 'text') and r.text:
                if any(word in r.text.lower() for word in bad_words):
                    return False
            
            return True
        except Exception as e:
            logger.debug(f"URL check failed for {url}: {e}")
            return False
    
    def build_index(self):
        """
        Build FAISS index from jobs_enhanced.csv file
        """
        try:
            # Load model
            self._load_model()
            
            # Try jobs_enhanced.csv first, fallback to jobs.csv
            csv_path = os.path.join(self.data_dir, 'jobs_enhanced.csv')
            if not os.path.exists(csv_path):
                csv_path = os.path.join(self.data_dir, 'jobs.csv')
                if not os.path.exists(csv_path):
                    raise FileNotFoundError(f"Jobs CSV file not found at {csv_path}")
            
            df = pd.read_csv(csv_path)
            logger.info(f"Loaded {len(df)} jobs from {os.path.basename(csv_path)}")
            
            # Prepare job metadata - include skills for better matching
            metadata_cols = ['id', 'title', 'company', 'location']
            if 'skills' in df.columns:
                metadata_cols.append('skills')
            else:
                df['skills'] = ''  # Add empty skills column if not present
            
            self.job_metadata = df[metadata_cols].copy()
            
            # Generate embeddings for job descriptions
            job_descriptions = df['jd_text'].tolist()
            logger.info("Generating embeddings for job descriptions...")
            
            embeddings = self.model.encode(job_descriptions, show_progress_bar=True)
            embeddings = self._normalize_embeddings(embeddings)
            
            # Create FAISS index
            dimension = embeddings.shape[1]
            self.index = faiss.IndexFlatIP(dimension)  # Inner product for cosine similarity
            self.index.add(embeddings.astype('float32'))
            
            # Save index, metadata, and model fingerprint
            self._save_index()
            self._save_metadata()
            self._save_model_fingerprint(csv_path)
            
            logger.info(f"Index built successfully with {len(df)} jobs")
            
        except Exception as e:
            logger.error(f"Error building index: {str(e)}")
            raise
    
    def _save_index(self):
        """Save FAISS index to disk"""
        try:
            os.makedirs(self.data_dir, exist_ok=True)
            faiss.write_index(self.index, self.index_path)
            logger.info(f"Index saved to {self.index_path}")
        except Exception as e:
            logger.error(f"Error saving index: {str(e)}")
            raise
    
    def _save_metadata(self):
        """Save job metadata to parquet file"""
        try:
            self.job_metadata.to_parquet(self.metadata_path, index=False)
            logger.info(f"Metadata saved to {self.metadata_path}")
        except Exception as e:
            logger.error(f"Error saving metadata: {str(e)}")
            raise
    
    def load_index(self) -> bool:
        """
        Load existing FAISS index and metadata with fingerprint validation
        
        Returns:
            bool: True if index loaded successfully, False otherwise
        """
        try:
            # Check if index file exists
            if not os.path.exists(self.index_path):
                logger.info(f"Index file not found at {self.index_path}")
                return False
            
            # Load model first to get dimension
            self._load_model()
            
            # Load index first
            self.index = faiss.read_index(self.index_path)
            logger.info(f"Loaded FAISS index with {self.index.ntotal} vectors")
            
            # Load metadata from CSV directly - skip parquet entirely to avoid dependency issues
            csv_path = os.path.join(self.data_dir, 'jobs_enhanced.csv')
            if not os.path.exists(csv_path):
                csv_path = os.path.join(self.data_dir, 'jobs.csv')
            
            if not os.path.exists(csv_path):
                logger.error(f"No CSV file found at {csv_path}")
                return False
                
            df = pd.read_csv(csv_path)
            # Ensure we have the required columns
            required_cols = ['id', 'title', 'company', 'location']
            if all(col in df.columns for col in required_cols):
                self.job_metadata = df[required_cols].copy()
                logger.info(f"Loaded metadata from CSV with {len(self.job_metadata)} jobs")
            else:
                logger.error(f"CSV missing required columns: {required_cols}")
                return False
            
            # Check model fingerprint (non-blocking)
            fingerprint = self._load_model_fingerprint()
            if fingerprint:
                if fingerprint.get("model_name") != self.model_name:
                    logger.warning(f"Model/index mismatch. Expected {self.model_name}, index was built with {fingerprint.get('model_name', 'UNKNOWN')}")
                if fingerprint.get("embedding_dim") != self.index.d:
                    logger.warning(f"Embedding dimension mismatch. Expected {self.index.d}, index was built with {fingerprint.get('embedding_dim', 'UNKNOWN')}")
            else:
                logger.info("No fingerprint found - assuming index is compatible")
            
            logger.info(f"Successfully loaded index with {len(self.job_metadata)} jobs")
            return True
            
        except Exception as e:
            logger.error(f"Error loading index: {str(e)}")
            return False
    
    def search(self, query: str, top_k: int = 5, role: Optional[str] = None) -> List[Dict[str, Any]]:
        """
        Search for similar jobs using semantic similarity with role-based filtering and improved scoring
        
        Args:
            query: Search query (resume text)
            top_k: Number of top results to return
            role: Role type ('fullstack', 'devops', 'general') - auto-detect if None
            
        Returns:
            List of dictionaries containing job information and similarity scores
        """
        if self.index is None or self.job_metadata is None:
            raise ValueError("Index not built. Call build_index() first.")
        
        try:
            # Auto-detect role if not provided
            if role is None:
                role = self.detect_role(query)
            
            # Generate embedding for query
            query_embedding = self.model.encode([query])
            query_embedding = self._normalize_embeddings(query_embedding)
            
            # Apply role-based filtering before search
            filtered_metadata = self.job_metadata.copy()
            if role == "fullstack":
                mask = filtered_metadata["title"].str.contains(
                    r"(full[- ]?stack|frontend|backend|software engineer|java developer|web developer|developer)", 
                    case=False, na=False
                )
                filtered_metadata = filtered_metadata[mask]
                logger.info(f"Fullstack filtering: {len(filtered_metadata)} jobs after filtering")
            elif role == "devops":
                mask = filtered_metadata["title"].str.contains(
                    r"(devops|sre|site reliability|platform|infra|cloud engineer)", 
                    case=False, na=False
                )
                filtered_metadata = filtered_metadata[mask]
                logger.info(f"DevOps filtering: {len(filtered_metadata)} jobs after filtering")
            
            # Search in much larger pool to account for filtering and thresholds
            search_k = min(top_k * 20, len(self.job_metadata))
            scores, indices = self.index.search(query_embedding.astype('float32'), search_k)
            
            # Get full job data for scoring
            csv_path = os.path.join(self.data_dir, 'jobs_enhanced.csv')
            if not os.path.exists(csv_path):
                csv_path = os.path.join(self.data_dir, 'jobs.csv')
            df = pd.read_csv(csv_path)
            
            # Extract resume skills for better matching
            resume_skills = self.extract_skills(query)
            query_lower = query.lower()
            
            results = []
            seen_jobs = set()
            
            for i, (similarity_score, idx) in enumerate(zip(scores[0], indices[0])):
                if idx >= len(self.job_metadata) or idx in seen_jobs:
                    continue
                    
                job_info = self.job_metadata.iloc[idx].to_dict()
                job_id = job_info.get('id')
                
                if job_id in seen_jobs:
                    continue
                seen_jobs.add(job_id)
                
                # Only include jobs that passed role filtering
                if len(filtered_metadata) > 0 and idx not in filtered_metadata.index and role != "general":
                    continue
                
                job_desc = df.iloc[idx]['jd_text'].lower()
                job_title = job_info['title'].lower()
                job_skills = set()
                
                # Extract job skills if available
                if 'skills' in job_info and job_info['skills']:
                    job_skills = set(job_info['skills'].lower().split(','))
                else:
                    # Fallback: extract from description
                    job_skills = self.extract_skills(df.iloc[idx]['jd_text'])
                
                # Calculate skill overlap (Jaccard similarity)
                skill_overlap = self._jaccard_similarity(resume_skills, job_skills)
                
                # Calculate composite score with better weighting
                semantic_score = float(similarity_score)
                skill_score = skill_overlap
                
                # Normalize and boost the final score for better user experience
                composite_score = (
                    0.70 * semantic_score +  # Higher weight on semantic similarity
                    0.30 * skill_score       # Skill overlap as secondary factor
                )
                
                # Boost the score slightly for better user experience (but keep it realistic)
                composite_score = min(composite_score * 1.1, 1.0)  # Max 100%
                
                # Apply minimum threshold with role-specific values (lowered for more results)
                if role == "fullstack":
                    min_threshold = 0.25  # Lowered threshold for fullstack
                elif role == "devops":
                    min_threshold = 0.22  # Lowered threshold for devops
                else:
                    min_threshold = 0.20  # Lowered general threshold
                
                # Only apply threshold if we have enough results OR if score is very low
                if composite_score < min_threshold and (len(results) >= top_k or composite_score < 0.15):
                    continue
                
                job_info.update({
                    'score': composite_score,
                    'similarity': float(similarity_score),
                    'skillOverlap': skill_overlap
                })
                
                results.append(job_info)
                
                if len(results) >= top_k:
                    break
            
            # Sort by composite score
            results.sort(key=lambda x: x['score'], reverse=True)
            
            # Validate URLs if we have enough results
            validated_results = []
            for job in results[:min(top_k * 2, len(results))]:  # Check more than needed
                # For now, skip URL validation to maintain performance
                # URL validation can be added here: if self.is_live_url(job.get('url', '')):
                validated_results.append(job)
                if len(validated_results) >= top_k:
                    break
            
            logger.info(f"Found {len(validated_results)} results for role '{role}' after filtering")
            
            # Return informative message if not enough good matches
            if len(validated_results) < top_k and validated_results:
                logger.warning(f"Only found {len(validated_results)} matches above threshold")
            
            return validated_results[:top_k]
            
        except Exception as e:
            logger.error(f"Error during search: {str(e)}")
            raise
    
    def _jaccard_similarity(self, set1: Set[str], set2: Set[str]) -> float:
        """Calculate Jaccard similarity between two sets of skills"""
        if not set1 and not set2:
            return 0.0
        intersection = len(set1 & set2)
        union = len(set1 | set2)
        return intersection / union if union > 0 else 0.0
    
    def get_job_count(self) -> int:
        """Get the number of jobs in the index"""
        if self.job_metadata is not None:
            return len(self.job_metadata)
        return 0

