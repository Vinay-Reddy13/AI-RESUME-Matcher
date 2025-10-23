"""
Basic tests for the embedding functionality
Tests model loading, embedding generation, and basic operations
"""

import pytest
import numpy as np
from sentence_transformers import SentenceTransformer
from vector_store import VectorStore

def test_model_loading():
    """Test that the sentence transformer model loads correctly"""
    model = SentenceTransformer('all-MiniLM-L6-v2')
    assert model is not None
    print("✓ Model loaded successfully")

def test_embedding_generation():
    """Test that embeddings are generated with correct shape"""
    model = SentenceTransformer('all-MiniLM-L6-v2')
    
    # Test with a small list of texts
    texts = [
        "Software engineer with Java experience",
        "Data scientist working with machine learning",
        "Frontend developer using React and TypeScript"
    ]
    
    embeddings = model.encode(texts)
    
    # Check shape: should be (3, 384) for all-MiniLM-L6-v2
    assert embeddings.shape[0] == 3  # 3 texts
    assert embeddings.shape[1] == 384  # 384-dimensional embeddings
    print(f"✓ Embeddings generated with shape: {embeddings.shape}")

def test_embedding_normalization():
    """Test that embeddings are properly normalized"""
    model = SentenceTransformer('all-MiniLM-L6-v2')
    vector_store = VectorStore()
    
    texts = ["Test text for normalization"]
    embeddings = model.encode(texts)
    normalized = vector_store._normalize_embeddings(embeddings)
    
    # Check that normalized embeddings have unit norm
    norms = np.linalg.norm(normalized, axis=1)
    assert np.allclose(norms, 1.0, atol=1e-6)
    print("✓ Embeddings normalized correctly")

def test_similarity_calculation():
    """Test that similarity between similar texts is higher than dissimilar texts"""
    model = SentenceTransformer('all-MiniLM-L6-v2')
    vector_store = VectorStore()
    
    # Similar texts
    similar_texts = [
        "Java developer with Spring Boot experience",
        "Software engineer working with Java and Spring"
    ]
    
    # Dissimilar text
    dissimilar_text = "Marketing manager with social media experience"
    
    # Generate embeddings
    similar_embeddings = model.encode(similar_texts)
    dissimilar_embedding = model.encode([dissimilar_text])
    
    # Normalize embeddings
    similar_embeddings = vector_store._normalize_embeddings(similar_embeddings)
    dissimilar_embedding = vector_store._normalize_embeddings(dissimilar_embedding)
    
    # Calculate similarities
    similar_similarity = np.dot(similar_embeddings[0], similar_embeddings[1])
    dissimilar_similarity = np.dot(similar_embeddings[0], dissimilar_embedding[0])
    
    # Similar texts should have higher similarity
    assert similar_similarity > dissimilar_similarity
    print(f"✓ Similarity test passed: {similar_similarity:.3f} > {dissimilar_similarity:.3f}")

def test_vector_store_initialization():
    """Test that VectorStore initializes correctly"""
    vector_store = VectorStore()
    assert vector_store.model_name == 'all-MiniLM-L6-v2'
    assert vector_store.index is None
    assert vector_store.job_metadata is None
    print("✓ VectorStore initialized correctly")

if __name__ == '__main__':
    # Run tests manually
    print("Running basic embedding tests...")
    
    try:
        test_model_loading()
        test_embedding_generation()
        test_embedding_normalization()
        test_similarity_calculation()
        test_vector_store_initialization()
        print("\n All tests passed!")
    except Exception as e:
        print(f"\n Test failed: {str(e)}")
        raise

