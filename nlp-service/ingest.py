"""
Data ingestion script for processing jobs.csv
Can be used for batch processing or data validation
"""

import pandas as pd
import os
import logging

logger = logging.getLogger(__name__)

def validate_jobs_data(csv_path: str) -> bool:
    """
    Validate the jobs CSV data format and content
    
    Args:
        csv_path: Path to the jobs CSV file
        
    Returns:
        bool: True if data is valid, False otherwise
    """
    try:
        if not os.path.exists(csv_path):
            logger.error(f"CSV file not found: {csv_path}")
            return False
        
        df = pd.read_csv(csv_path)
        
        # Check required columns
        required_columns = ['id', 'title', 'company', 'location', 'jd_text']
        missing_columns = [col for col in required_columns if col not in df.columns]
        
        if missing_columns:
            logger.error(f"Missing required columns: {missing_columns}")
            return False
        
        # Check for empty values in critical columns
        critical_columns = ['id', 'title', 'company', 'jd_text']
        for col in critical_columns:
            if df[col].isnull().any():
                logger.error(f"Found null values in column: {col}")
                return False
        
        # Check data types
        if not df['id'].dtype in ['int64', 'int32']:
            logger.error("ID column should be integer type")
            return False
        
        logger.info(f"Data validation passed. Found {len(df)} valid jobs")
        return True
        
    except Exception as e:
        logger.error(f"Error validating data: {str(e)}")
        return False

def get_job_statistics(csv_path: str) -> dict:
    """
    Get statistics about the jobs data
    
    Args:
        csv_path: Path to the jobs CSV file
        
    Returns:
        dict: Statistics about the jobs data
    """
    try:
        df = pd.read_csv(csv_path)
        
        stats = {
            'total_jobs': len(df),
            'unique_companies': df['company'].nunique(),
            'unique_locations': df['location'].nunique(),
            'avg_jd_length': df['jd_text'].str.len().mean(),
            'min_jd_length': df['jd_text'].str.len().min(),
            'max_jd_length': df['jd_text'].str.len().max()
        }
        
        return stats
        
    except Exception as e:
        logger.error(f"Error getting statistics: {str(e)}")
        return {}

if __name__ == '__main__':
    # Example usage
    data_dir = '/app/data'
    csv_path = os.path.join(data_dir, 'jobs.csv')
    
    if validate_jobs_data(csv_path):
        stats = get_job_statistics(csv_path)
        print("Job data statistics:")
        for key, value in stats.items():
            print(f"  {key}: {value}")
    else:
        print("Data validation failed")

