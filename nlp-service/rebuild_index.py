#!/usr/bin/env python3
"""
Command-line script to rebuild FAISS index with model verification
Usage: python rebuild_index.py --model all-MiniLM-L6-v2 --csv data/jobs_enhanced.csv
"""

import argparse
import os
import sys
from vector_store import VectorStore

def main():
    parser = argparse.ArgumentParser(description='Rebuild FAISS index with model verification')
    parser.add_argument('--model', default='all-MiniLM-L6-v2', help='Model name to use')
    parser.add_argument('--csv', default='data/jobs_enhanced.csv', help='CSV file to use')
    parser.add_argument('--data-dir', default='/app/data', help='Data directory')
    
    args = parser.parse_args()
    
    print(f"Rebuilding FAISS index...")
    print(f"Model: {args.model}")
    print(f"CSV: {args.csv}")
    print(f"Data dir: {args.data_dir}")
    
    try:
        # Initialize vector store with specified model
        vector_store = VectorStore(model_name=args.model)
        vector_store.data_dir = args.data_dir
        
        # Build index
        vector_store.build_index()
        
        print(f" Index rebuilt successfully!")
        print(f" Jobs indexed: {vector_store.get_job_count()}")
        
    except Exception as e:
        print(f" Error rebuilding index: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
