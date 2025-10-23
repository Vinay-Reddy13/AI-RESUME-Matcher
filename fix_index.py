#!/usr/bin/env python3

# Simple script to fix the load_index method in the container
import os

def fix_vector_store():
    content = '''import os
import json
import logging
import numpy as np
import pandas as pd
import faiss
from sentence_transformers import SentenceTransformer
from typing import List, Dict, Any, Optional, Set

logger = logging.getLogger(__name__)

class VectorStore:
    def __init__(self, model_name='all-MiniLM-L6-v2'):
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
        if self.model is None:
            logger.info(f"Loading sentence transformer model: {self.model_name}")
            self.model = SentenceTransformer(self.model_name)
            logger.info("Model loaded successfully")
    
    def load_index(self) -> bool:
        """Load existing FAISS index and metadata - CSV only version"""
        try:
            if not os.path.exists(self.index_path):
                logger.info(f"Index file not found at {self.index_path}")
                return False
            
            self._load_model()
            self.index = faiss.read_index(self.index_path)
            logger.info(f"Loaded FAISS index with {self.index.ntotal} vectors")
            
            # Load from CSV directly - skip parquet entirely
            csv_path = os.path.join(self.data_dir, 'jobs_enhanced.csv')
            if not os.path.exists(csv_path):
                csv_path = os.path.join(self.data_dir, 'jobs.csv')
            
            if not os.path.exists(csv_path):
                logger.error(f"No CSV file found at {csv_path}")
                return False
                
            df = pd.read_csv(csv_path)
            required_cols = ['id', 'title', 'company', 'location']
            if all(col in df.columns for col in required_cols):
                self.job_metadata = df[required_cols].copy()
                logger.info(f"Loaded metadata from CSV with {len(self.job_metadata)} jobs")
            else:
                logger.error(f"CSV missing required columns: {required_cols}")
                return False
            
            logger.info(f"Successfully loaded index with {len(self.job_metadata)} jobs")
            return True
            
        except Exception as e:
            logger.error(f"Error loading index: {str(e)}")
            return False
    
    def detect_role(self, text: str) -> str:
        t = text.lower()
        fs_hits = sum(1 for k in self.FULLSTACK if k in t)
        dv_hits = sum(1 for k in self.DEVOPS if k in t)
        
        if fs_hits == 0 and dv_hits == 0:
            return "general"
        return "fullstack" if fs_hits >= dv_hits else "devops"
    
    def search(self, query: str, top_k: int = 5, role: Optional[str] = None) -> List[Dict[str, Any]]:
        if self.index is None or self.job_metadata is None:
            raise ValueError("Index not built. Call build_index() first.")
        
        if role is None:
            role = self.detect_role(query)
        
        query_embedding = self.model.encode([query])
        query_embedding = query_embedding / np.linalg.norm(query_embedding, axis=1, keepdims=True)
        
        # Search in the index
        search_k = min(top_k * 5, len(self.job_metadata))
        scores, indices = self.index.search(query_embedding.astype('float32'), search_k)
        
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
            
            # Apply role filtering
            if role == "fullstack":
                title = job_info.get('title', '').lower()
                if not any(keyword in title for keyword in ['java', 'full stack', 'full-stack', 'frontend', 'backend', 'software engineer', 'web developer']):
                    continue
            
            job_info.update({
                'score': float(similarity_score),
                'similarity': float(similarity_score)
            })
            
            results.append(job_info)
            
            if len(results) >= top_k:
                break
        
        results.sort(key=lambda x: x['score'], reverse=True)
        return results[:top_k]
'''

    # Write the simplified version
    with open('/app/vector_store_simple.py', 'w') as f:
        f.write(content)
    
    print(" Created simplified vector_store_simple.py")

if __name__ == "__main__":
    fix_vector_store()

