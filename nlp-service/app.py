"""
NLP Service for AI Resume Matcher
Provides semantic search capabilities using sentence transformers and FAISS
"""

from flask import Flask, request, jsonify
import os
import logging
from vector_store import VectorStore

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)

# Initialize vector store
vector_store = None

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({"status": "ok"})

@app.route('/index/build', methods=['POST'])
def build_index():
    """Build FAISS index from jobs.csv"""
    global vector_store
    
    try:
        logger.info("Building FAISS index from jobs.csv")
        vector_store = VectorStore()
        vector_store.build_index()
        
        job_count = vector_store.get_job_count()
        logger.info(f"Successfully indexed {job_count} jobs")
        
        return jsonify({
            "status": "success",
            "message": f"Index built successfully with {job_count} jobs",
            "job_count": job_count
        })
    
    except Exception as e:
        logger.error(f"Error building index: {str(e)}")
        return jsonify({
            "status": "error",
            "message": f"Failed to build index: {str(e)}"
        }), 500

@app.route('/search', methods=['POST'])
def search():
    """Search for matching jobs using semantic similarity with role-based filtering"""
    global vector_store
    
    try:
        data = request.get_json()
        if not data or 'query' not in data:
            return jsonify({
                "status": "error",
                "message": "Missing 'query' field in request body"
            }), 400
        
        query = data['query']
        top_k = data.get('top_k', 5)
        role = data.get('role', None)  # New role parameter
        
        if not vector_store:
            return jsonify({
                "status": "error",
                "message": "Index not built. Please call /index/build first"
            }), 400
        
        # Detect role if not provided
        detected_role = role if role else vector_store.detect_role(query)
        
        logger.info(f"Searching for top {top_k} matches for role '{detected_role}' with query: {query[:100]}...")
        results = vector_store.search(query, top_k, role)
        
        return jsonify({
            "status": "success",
            "role": detected_role,
            "count": len(results),
            "results": results
        })
    
    except Exception as e:
        logger.error(f"Error during search: {str(e)}")
        return jsonify({
            "status": "error",
            "message": f"Search failed: {str(e)}"
        }), 500

@app.errorhandler(404)
def not_found(error):
    return jsonify({"status": "error", "message": "Endpoint not found"}), 404

@app.errorhandler(500)
def internal_error(error):
    return jsonify({"status": "error", "message": "Internal server error"}), 500

if __name__ == '__main__':
    # Load existing index if available
    try:
        vector_store = VectorStore()
        if vector_store.load_index():
            logger.info("Loaded existing FAISS index")
        else:
            logger.info("No existing index found. Call /index/build to create one.")
    except Exception as e:
        logger.warning(f"Could not load existing index: {str(e)}")
        vector_store = None
    
    app.run(host='0.0.0.0', port=8001, debug=True)

